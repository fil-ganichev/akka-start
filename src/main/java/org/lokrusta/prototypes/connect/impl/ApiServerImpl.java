package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import lombok.Builder;
import lombok.Data;
import org.lokrusta.prototypes.connect.api.*;
import org.lokrusta.prototypes.connect.api.dto.ArrayApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.impl.common.ApiCallException;
import org.lokrusta.prototypes.connect.impl.common.CallPointNotFoundException;
import org.lokrusta.prototypes.connect.impl.common.IllegalCallPointException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ApiServerImpl extends StageBase implements ApiServer, ProxiedStage, ApplicationContextAware, InitializingBean {

    private final Map<Class<?>, CallPoint> points;
    private final ApiCallProcessor apiCallProcessor;
    private final boolean isProxyCall;

    private ApplicationContext applicationContext;

    private Consumer<Exception> errorListener;

    private ApiServerImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, CallPoint> points, boolean isProxyCall) {
        this.apiCallProcessor = apiCallProcessor;
        this.points = points;
        this.isProxyCall = isProxyCall;
        this.argsConverter = new DefaultApiServerArgsConverter();
    }

    private ApiServerImpl(Map<Class<?>, CallPoint> points, boolean isProxyCall) {
        this(new ApiCallProcessor(), points, isProxyCall);
    }

    public static ApiServerImpl of(List<Class<?>> apiInterfaces, boolean isProxyCall) {
        return new ApiServerImpl(apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz).build())), isProxyCall);
    }

    public static ApiServerImpl of(Map<Class<?>, Object> api, boolean isProxyCall) {
        return new ApiServerImpl(api.keySet().stream().collect(Collectors.toList())
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz)
                                .apiServerImpl(api.get(clazz)).build())), isProxyCall);
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        //Поддерживаются оба варианта вызова за счет merge (вызов неявный через данные из источника и явный программно)
        if (isProxyCall) {
            return Flow.of(ArgsWrapper.class)
                    .merge(apiCallProcessor.clientApiSource())
                    .map(this::next)
                    .map(this::call)
                    .map(this::response)
                    .map(this::checkError)
                    .mapError(new PFBuilder<Throwable, Throwable>()
                            .match(Exception.class, this::onError)
                            .build());
        } else {
            return Flow.of(ArgsWrapper.class)
                    .map(this::next)
                    .map(this::call)
                    .map(this::response)
                    .map(this::checkError)
                    .mapError(new PFBuilder<Throwable, Throwable>()
                            .match(Exception.class, this::onError)
                            .build());
        }
    }

    public <T> ArgsWrapper call(ArgsWrapper argsWrapper) {
        try {
            Class<T> apiClass = argsWrapper.getCallInfo().getApiClass();
            CallPoint<T> callPoint = points.get(apiClass);
            Method method = argsWrapper.getCallInfo().getApiMethod();
            if (!isProxyCall && Modifier.isAbstract(method.getModifiers())) {
                method = callPoint.getApiServerImpl().getClass().getMethod(method.getName(), method.getParameterTypes());
            }
            Object bean = callPoint.getApiServerImpl();
            Class<?> parameterTypes[] = method.getParameterTypes();
            ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
            if (parameterTypes.length == 0) {
                throw new IllegalCallPointException();
            } else if (parameterTypes.length == 1) {
                if (apiCallArguments instanceof ObjectApiCallArguments) {
                    Object result = method.invoke(bean, ((ObjectApiCallArguments) apiCallArguments).getValue());
                    return ArgsWrapperImpl.of(result).withCorrelationId(argsWrapper.getCorrelationId());
                }
            }
            Object result;
            if (apiCallArguments instanceof ArrayApiCallArguments) {
                result = method.invoke(bean, ((ArrayApiCallArguments) apiCallArguments).getValues());
            } else {
                Object parameters = apiCallArguments instanceof ObjectApiCallArguments
                        ? ((ObjectApiCallArguments) apiCallArguments).getValue()
                        : apiCallArguments;
                Method apiMethod = callPoint.getApi().getMethod(method.getName(), method.getParameterTypes());
                List args = prepareArgValues(apiMethod, parameters);
                result = method.invoke(bean, args.toArray(new Object[args.size()]));
            }
            return ArgsWrapperImpl.of(result).withCorrelationId(argsWrapper.getCorrelationId());
        } catch (Exception e) {
            Exception cause = e.getCause() != null && e.getCause() instanceof Exception && e instanceof InvocationTargetException
                    ? (Exception) e.getCause()
                    : e;
            return ArgsWrapperImpl.of(cause).withCorrelationId(argsWrapper.getCorrelationId());
        }
    }

    private Object findValue(String parameterName, Object parameters) {
        try {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(parameterName, parameters.getClass());
            return propertyDescriptor.getReadMethod().invoke(parameters);
        } catch (Exception e) {
            throw new ApiCallException(e);
        }
    }

    private List prepareArgValues(Method method, Object parameters) {
        return Arrays.stream(method.getParameterAnnotations())
                .map(annotattions -> Arrays.stream(annotattions)
                        .filter(annotattion -> annotattion.annotationType().equals(ApiArg.class))
                        .findFirst()
                        .orElseThrow())
                .map(annotation -> findValue(((ApiArg) annotation).value(), parameters))
                .collect(Collectors.toList());
    }

    protected Exception onError(Exception e) throws Exception {
        if (errorListener != null) {
            errorListener.accept(e);
        }
        return e;
    }

    public ApiServerImpl withErrorListener(Consumer<Exception> errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    protected ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        if (argsWrapper.getException() != null) {
            throw argsWrapper.getException();
        }
        return argsWrapper;
    }

    @Override
    public void init() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getProxy(Class<T> clazz) {
        return (T) points.get(clazz).getApiImpl();
    }

    public Object getBean(Class<?> clazz) {
        CallPoint callPoint = points.get(clazz);
        return callPoint == null
                ? null
                : callPoint.getApiServerImpl();
    }

    private <T> Object findBean(Class<T> clazz, Map<String, Object> candidates) {
        //При proxyCall, серверный бин не implements интерфейс point.getApi(), интерфейс в ApiBean указан
        List beanList = candidates.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(bean -> isApiBeanOf(bean, clazz))
                .collect(Collectors.toList());
        if (beanList.isEmpty()) {
            throw new CallPointNotFoundException(String.format("Beans of type %s anotated by %s not found", clazz.getName(), ApiBean.class.getName()));
        } else if (beanList.size() > 1) {
            throw new CallPointNotFoundException(String.format("Too many beans of type %s anotated by %s found", clazz.getName(), ApiBean.class.getName()));
        }
        return beanList.get(0);

    }

    private <T> T findBean(Class<T> clazz, Set<String> candidates) {
        Map<String, T> beans = applicationContext.getBeansOfType(clazz);
        Set<String> beanNames = beans.keySet();
        beanNames.retainAll(candidates);
        if (beanNames.isEmpty()) {
            throw new CallPointNotFoundException(String.format("Too many beans of type %s anotated by %s found", clazz.getName(), ApiBean.class.getName()));
        } else if (beanNames.size() > 1) {
            throw new CallPointNotFoundException(String.format("Too many beans of type %s anotated by %s found", clazz.getName(), ApiBean.class.getName()));
        }
        return (T) applicationContext.getBean(String.valueOf(beanNames.toArray()[0]));
    }

    private <T> boolean isApiBeanOf(T bean, Class<?> clazz) {
        Class beanClass = bean.getClass();
        ApiBean apiBean = (ApiBean) beanClass.getAnnotation(ApiBean.class);
        return clazz.equals(apiBean.value());
    }

    private <T> void initCallPoint(CallPoint<T> point) {
        if (isProxyCall) {
            if (point.getApiServerImpl() == null) {
                Map<String, Object> beanCandidates = applicationContext.getBeansWithAnnotation(ApiBean.class);
                point.setApiServerImpl(findBean(point.getApi(), beanCandidates));
            }
            final Set<Method> methods = new HashSet<>();
            final Class<?> serverClass = point.getApiServerImpl().getClass();
            Arrays.asList(point.getApi().getMethods())
                    .stream()
                    .filter(method -> method.getReturnType().isAssignableFrom(CompletableFuture.class))
                    .forEach(method -> {
                        try {
                            Method serverMethod = serverClass.getMethod(method.getName(), method.getParameterTypes());
                            methods.add(serverMethod);
                        } catch (NoSuchMethodException e) {
                            throw new IllegalCallPointException(e);
                        }
                    });
            point.setMethods(methods);
            T apiProxy = (T) Proxy.newProxyInstance(
                    this.getClass().getClassLoader(),
                    new Class[]{point.getApi()},
                    new DefaultApiProxy(point));
            point.setApiImpl(apiProxy);
        } else {
            if (point.getApiServerImpl() == null) {
                Set<String> beanNameCandidates = new HashSet<>(Arrays.asList(applicationContext.getBeanNamesForAnnotation(ApiBean.class)));
                point.setApiServerImpl(findBean(point.getApi(), beanNameCandidates));
            }
            Set<Method> methods = Arrays.asList(point.getApi().getMethods())
                    .stream()
                    .collect(Collectors.toSet());
            point.setMethods(methods);
        }
    }

    protected ArgsWrapper response(ArgsWrapper argsWrapper) {
        if (isProxyCall) {
            apiCallProcessor.response(argsWrapper);
        }
        return argsWrapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector();
        points.forEach((key, point) -> initCallPoint(point));
    }

    protected void initServerBean(Class<?> clazz, Object beanInstance) {
        points.get(clazz).setApiServerImpl(beanInstance);
    }

    @Data
    @Builder
    protected static class CallPoint<T> {
        //Интерфейс
        private Class<T> api;
        //Генерируемая имплементация интерфейса
        private T apiImpl;
        //Экземпляр класса бина-обработчика
        private Object apiServerImpl;
        private Set<Method> methods;
    }

    protected class DefaultApiServerArgsConverter implements Function<ArgsWrapper, ArgsWrapper> {

        @Override
        public ArgsWrapper apply(ArgsWrapper argsWrapper) {
            if (argsWrapper.getCallInfo() == null) {
                if (points.size() == 1) {
                    CallPoint callPoint = points.values().stream().findFirst().orElseThrow();
                    if (callPoint.getMethods().size() == 1) {
                        ((ArgsWrapperImpl) argsWrapper).setCallInfo(CallInfo.builder()
                                .apiClass(callPoint.getApi())
                                .apiMethod((Method) callPoint.getMethods().stream().findFirst().orElseThrow())
                                .build());
                    }
                }
            }
            if (argsWrapper.getCallInfo() == null) {
                throw new CallPointNotFoundException();
            }
            return argsWrapper;
        }
    }

    protected class DefaultApiProxy implements InvocationHandler {

        private final CallPoint<?> point;

        public DefaultApiProxy(CallPoint<?> point) {
            this.point = point;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ArgsWrapperImpl argsWrapper = ArgsWrapperImpl.of(args);
            argsWrapper.setCallInfo(CallInfo.builder()
                    .apiMethod(findRealMethod(method))
                    .apiClass((Class<Object>) method.getDeclaringClass()).build());
            return apiCallProcessor.request(argsWrapper)
                    .thenApply(this::fromArgsWrapper);
        }

        private Object fromArgsWrapper(ArgsWrapper answer) {
            ObjectApiCallArguments result = (ObjectApiCallArguments) answer.getApiCallArguments();
            return result.getValue();
        }

        private Method findRealMethod(Method method) {
            //todo Оптимальнее поиск - использование intern(), кэширование? Или DefaultApiProxy для каждого метода создаем, чтоб вообще не искать
            Method realMethod = point
                    .getMethods()
                    .stream()
                    .filter(currMethod -> currMethod.getName().equals(method.getName())
                            && Arrays.equals(currMethod.getParameterTypes(), method.getParameterTypes()))
                    .findFirst()
                    .orElseThrow();
            return realMethod;
        }
    }
}
