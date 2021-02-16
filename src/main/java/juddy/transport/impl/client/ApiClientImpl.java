package juddy.transport.impl.client;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ApiCallArguments;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.client.ApiClient;
import juddy.transport.api.common.ProxiedStage;
import juddy.transport.impl.common.ApiCallProcessor;
import juddy.transport.impl.common.StageBase;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiClientImpl extends StageBase implements ApiClient, ProxiedStage {

    private final Map<Class<?>, CallPoint<?>> points;
    private final ApiCallProcessor apiCallProcessor;

    protected ApiClientImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, CallPoint<?>> points) {
        this.apiCallProcessor = apiCallProcessor;
        this.points = points;
    }

    protected ApiClientImpl(Map<Class<?>, CallPoint<?>> points) {
        this(new ApiCallProcessor(), points);
    }

    public static ApiClientImpl of(List<Class<?>> apiInterfaces) {
        return new ApiClientImpl(apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> ApiClientImpl.CallPoint.builder()
                                .api((Class<Object>) clazz)
                                .methods(Arrays.asList(clazz.getMethods()))
                                .build())));
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class).merge(apiCallProcessor.clientApiSource())
                .log(logTitle("api call arguments"))
                .map(this::next)
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        points.forEach((key, point) -> initCallPoint(point));
    }

    @Override
    public <T> T getProxy(Class<T> clazz) {
        return (T) points.get(clazz).getApiImpl();
    }

    @Override
    public ApiCallProcessor getApiCallProcessor() {
        return apiCallProcessor;
    }

    private <T> void initCallPoint(CallPoint<T> point) {
        T apiProxy = (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{point.getApi()},
                new DefaultApiProxy());
        point.setApiImpl(apiProxy);
    }

    @Data
    @Builder
    protected static class CallPoint<T> {
        //Интерфейс
        private Class<T> api;
        //Генерируемая имплементация интерфейса
        private T apiImpl;
        private List<Method> methods;
    }

    protected class DefaultApiProxy implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ArgsWrapper argsWrapper = ArgsWrapper.of(args);
            argsWrapper.setCallInfo(CallInfo.builder()
                    .apiMethod(method)
                    .apiClass((Class<Object>) method.getDeclaringClass()).build());
            return apiCallProcessor.request(argsWrapper)
                    .thenApply(this::fromArgsWrapper);
        }

        private Object fromArgsWrapper(ArgsWrapper answer) {
            ApiCallArguments result = answer.getApiCallArguments();
            return result.getResult();
        }
    }
}
