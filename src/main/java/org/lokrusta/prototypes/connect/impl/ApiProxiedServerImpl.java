package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import org.lokrusta.prototypes.connect.api.ApiBean;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.CallInfo;
import org.lokrusta.prototypes.connect.api.ProxiedStage;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.impl.error.CallPointNotFoundException;
import org.lokrusta.prototypes.connect.impl.error.IllegalCallPointException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ApiProxiedServerImpl extends ApiServerBase implements ProxiedStage {

    private final ApiCallProcessor apiCallProcessor;

    protected ApiProxiedServerImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, CallPoint> points) {
        super(points);
        this.apiCallProcessor = apiCallProcessor;
    }

    protected ApiProxiedServerImpl(Map<Class<?>, CallPoint> points) {
        this(new ApiCallProcessor(), points);
    }

    public static ApiProxiedServerImpl of(List<Class<?>> apiInterfaces) {
        return new ApiProxiedServerImpl(apiToCallPoints(apiInterfaces));
    }

    public static ApiProxiedServerImpl of(Map<Class<?>, Object> api) {
        return new ApiProxiedServerImpl(apiToCallPoints(api));
    }

    @Override
    public <T> T getProxy(Class<T> clazz) {
        return (T) getPoints().get(clazz).getApiImpl();
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class)
                .merge(apiCallProcessor.clientApiSource())
                .map(this::next)
                .map(this::call)
                .map(this::response)
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    @Override
    protected <T> Method findCallingMethod(ArgsWrapper argsWrapper, CallPoint<T> callPoint) throws NoSuchMethodException {
        return argsWrapper.getCallInfo().getApiMethod();
    }

    @Override
    protected <T> void initCallPoint(CallPoint<T> point) {
        if (point.getApiServerImpl() == null) {
            Map<String, Object> beanCandidates = getApplicationContext().getBeansWithAnnotation(ApiBean.class);
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
                new ApiProxiedServerImpl.DefaultApiProxy(point));
        point.setApiImpl(apiProxy);
    }

    private ArgsWrapper response(ArgsWrapper argsWrapper) {
        apiCallProcessor.response(argsWrapper);
        return argsWrapper;
    }

    private <T> Object findBean(Class<T> clazz, Map<String, Object> candidates) {
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

    private <T> boolean isApiBeanOf(T bean, Class<?> clazz) {
        Class beanClass = bean.getClass();
        ApiBean apiBean = (ApiBean) beanClass.getAnnotation(ApiBean.class);
        return clazz.equals(apiBean.value());
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