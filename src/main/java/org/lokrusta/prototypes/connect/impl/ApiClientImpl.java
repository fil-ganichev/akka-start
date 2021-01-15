package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import lombok.Builder;
import lombok.Data;
import org.lokrusta.prototypes.connect.api.*;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ApiClientImpl extends StageBase implements ApiClient, ProxiedStage, InitializingBean {

    private final Map<Class<?>, ApiClientImpl.CallPoint> points;
    private final ApiCallProcessor apiCallProcessor;

    private Consumer<Exception> errorListener;


    protected ApiClientImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, ApiClientImpl.CallPoint> points) {
        this.apiCallProcessor = apiCallProcessor;
        this.points = points;
    }

    protected ApiClientImpl(Map<Class<?>, ApiClientImpl.CallPoint> points) {
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

    // Клиент соединяется c только ApiTransport и ApiTransport вызывает ApiCallProcessor.response()
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class).log("").merge(apiCallProcessor.clientApiSource()).log("")
                .map(this::next).log("")
                .map(this::checkError).log("")
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build()).log("");
    }

    protected Exception onError(Exception e) throws Exception {
        if (errorListener != null) {
            errorListener.accept(e);
        }
        return e;
    }

    public ApiClientImpl withErrorListener(Consumer<Exception> errorListener) {
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
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector();
        points.forEach((key, point) -> initCallPoint(point));
    }

    @Override
    public <T> T getProxy(Class<T> clazz) {
        return (T) points.get(clazz).getApiImpl();
    }

    public ApiCallProcessor getApiCallProcessor() {
        return apiCallProcessor;
    }

    private <T> void initCallPoint(ApiClientImpl.CallPoint<T> point) {
        T apiProxy = (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{point.getApi()},
                new ApiClientImpl.DefaultApiProxy());
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
            ArgsWrapperImpl argsWrapper = ArgsWrapperImpl.of(args);
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
