package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.lokrusta.prototypes.connect.impl.common.ApiEngineException;
import org.lokrusta.prototypes.connect.api.ApiEngine;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.impl.common.IllegalCallPointException;

import java.util.ArrayList;
import java.util.Optional;

public class ApiEngineImpl implements ApiEngine {

    private final ArrayList<StageBase> stages = new ArrayList<>();
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> lastFlow;
    protected ActorSystem actorSystem = ActorSystem.create("QuickStart");

    @Override
    public ApiEngine run() {
        stages.forEach(StageBase::init);
        if (isFromServer()) {
            ((TcpServerTransportImpl) stages.get(0)).run(lastFlow);
        } else {
            Source.empty(ArgsWrapper.class).via(lastFlow).run(actorSystem);
        }
        return this;
    }

    public static ApiEngineImpl of(ApiSourceImpl source) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(source);
        return apiEngine;
    }

    public static ApiEngineImpl of(ApiClientImpl client) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(client);
        return apiEngine;
    }

    public static ApiEngineImpl of(ApiServerImpl server) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(server);
        return apiEngine;

    }

    public static ApiEngineImpl of(TcpServerTransportImpl tcpServer) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(tcpServer);
        return apiEngine;
    }

    public <T> T findProxy(Class<T> clazz) {
        if (stages.get(0) instanceof ApiServerImpl) {
            ApiServerImpl apiServer = (ApiServerImpl) stages.get(0);
            return apiServer.getProxy(clazz);
        }
        throw new ApiEngineException("ApiEngine start point is not ApiServer");
    }

    public Object getBean(Class<?> clazz) {
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i) instanceof ApiServerImpl) {
                Object bean = ((ApiServerImpl) stages.get(i)).getBean(clazz);
                if (bean != null) {
                    return bean;
                }
            }
        }
        return null;
    }

    private ApiEngineImpl(ApiSourceImpl source) {
        stages.add(source);
        lastFlow = source.getStageConnector();
    }

    private ApiEngineImpl(ApiServerImpl server) {
        stages.add(server);
        lastFlow = server.getStageConnector();
    }

    private ApiEngineImpl(ApiClientImpl client) {
        stages.add(client);
        lastFlow = client.getStageConnector();
    }

    private ApiEngineImpl(TcpServerTransportImpl tcpServer) {
        stages.add(tcpServer);
        lastFlow = tcpServer.getStageConnector();
    }

    public ApiEngineImpl connect(StageBase stage) {
        //todo Проверки на некорректные соединения. Например, client->client, tcpServer не первый stage и проч..
        if (stage instanceof TcpServerTransportImpl) {
            throw new ApiEngineException();
        }
        lastFlow = lastFlow.via(stage.getStageConnector());
        stages.add(stage);
        return this;
    }

    private boolean isFromServer() {
        return stages.get(0) instanceof TcpServerTransportImpl;
    }
}
