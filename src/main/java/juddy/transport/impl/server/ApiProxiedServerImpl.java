package juddy.transport.impl.server;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.common.ApiBean;
import juddy.transport.api.common.ProxiedStage;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.common.ApiCallProcessor;
import juddy.transport.impl.error.ApiCallException;
import juddy.transport.impl.error.CallPointNotFoundException;
import juddy.transport.impl.error.IllegalCallPointException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ApiProxiedServerImpl extends ApiServerBase implements ProxiedStage {

    private final ApiCallProcessor apiCallProcessor;

    protected ApiProxiedServerImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, CallPoint<?>> points) {
        super(points);
        this.apiCallProcessor = apiCallProcessor;
    }

    protected ApiProxiedServerImpl(Map<Class<?>, CallPoint<?>> points) {
        this(new ApiCallProcessor(), points);
    }

    public static ApiProxiedServerImpl of(List<Class<?>> apiInterfaces) {
        return new ApiProxiedServerImpl(apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz).build())));
    }

    public static ApiProxiedServerImpl of(Map<Class<?>, Object> api) {
        return new ApiProxiedServerImpl(new ArrayList<>(api.keySet())
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz)
                                .apiServerImpl(api.get(clazz)).build())));
    }

    @Override
    public <T> T getProxy(Class<T> clazz) {
        return (T) getPoints().get(clazz).getApiImpl();
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class)
                .merge(apiCallProcessor.clientApiSource())
                .log(logTitle("api call arguments"))
                .map(this::next)
                .map(this::call)
                .log(logTitle("api call result"))
                .map(this::response)
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    @Override
    protected <T> Method findCallingMethod(ArgsWrapper argsWrapper, CallPoint<T> callPoint) {
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
        Arrays.stream(point.getApi().getMethods())
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
        List<?> beanList = candidates.values().stream()
                .filter(bean -> isApiBeanOf(bean, clazz))
                .collect(Collectors.toList());
        if (beanList.isEmpty()) {
            throw new CallPointNotFoundException(String.format("Beans of type %s annotated by %s not found",
                    clazz.getName(), ApiBean.class.getName()));
        } else if (beanList.size() > 1) {
            throw new CallPointNotFoundException(String.format("Too many beans of type %s annotated by %s found",
                    clazz.getName(), ApiBean.class.getName()));
        }
        return beanList.get(0);
    }

    private <T> boolean isApiBeanOf(T bean, Class<?> clazz) {
        Class<?> beanClass = bean.getClass();
        ApiBean apiBean = beanClass.getAnnotation(ApiBean.class);
        return clazz.equals(apiBean.value());
    }

    protected class DefaultApiProxy implements InvocationHandler {

        private final CallPoint<?> point;

        public DefaultApiProxy(CallPoint<?> point) {
            this.point = point;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            ArgsWrapper argsWrapper = ArgsWrapper.of(args);
            argsWrapper.setCallInfo(CallInfo.builder()
                    .apiMethod(findRealMethod(method))
                    .apiClass((Class<Object>) method.getDeclaringClass()).build());
            return apiCallProcessor.request(argsWrapper)
                    .thenApply(this::fromArgsWrapper);
        }

        private Object fromArgsWrapper(ArgsWrapper answer) {
            ObjectApiCallArguments<?> result = (ObjectApiCallArguments<?>) answer.getApiCallArguments();
            return result.getValue();
        }

        private Method findRealMethod(Method method) {
            return point.getMethods()
                    .stream()
                    .filter(currMethod -> currMethod.getName().equals(method.getName())
                            && Arrays.equals(currMethod.getParameterTypes(), method.getParameterTypes()))
                    .findFirst()
                    .orElseThrow(() -> new ApiCallException(String.format("Method %s not found with parameters %s",
                            method.getName(),
                            Arrays.toString(method.getParameterTypes()))));
        }
    }
}
