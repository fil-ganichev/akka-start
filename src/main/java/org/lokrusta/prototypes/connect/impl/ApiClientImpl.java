package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import lombok.Builder;
import lombok.Data;
import org.lokrusta.prototypes.connect.api.ApiClient;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class ApiClientImpl extends StageBase implements ApiClient {

    private final Map<Class<?>, ApiClientImpl.CallPoint> points;
    private final ApiCallProcessor apiCallProcessor;


    private ApiClientImpl(ApiCallProcessor apiCallProcessor, Map<Class<?>, ApiClientImpl.CallPoint> points) {
        this.apiCallProcessor = apiCallProcessor;
        this.points = points;
        this.stageConnector = createConnector();
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class).merge(apiCallProcessor.clientApiSource()).map(this::next);
    }

    @Override
    public void init() {
    }

    @Data
    @Builder
    protected static class CallPoint {
        //Интерфейс
        private Class<?> api;
        //Генерируемая имплементация интерфейса
        private Class<?> apiImpl;
        private List<Method> methods;
    }
}
