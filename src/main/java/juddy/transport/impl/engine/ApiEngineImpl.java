package juddy.transport.impl.engine;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.common.ProxiedStage;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.client.ApiClientImpl;
import juddy.transport.impl.common.ApiCallProcessor;
import juddy.transport.impl.common.StageBase;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.error.ApiEngineException;
import juddy.transport.impl.error.ErrorProcessor;
import juddy.transport.impl.net.TcpClientTransportImpl;
import juddy.transport.impl.net.TcpServerTransportImpl;
import juddy.transport.impl.server.ApiServerBase;
import juddy.transport.impl.server.ApiServerImpl;
import juddy.transport.impl.source.ApiSourceImpl;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ApiEngineImpl implements ApiEngine {

    private final ArrayList<StageBase> stages = new ArrayList<>();
    private final ErrorProcessor errorProcessor = new ErrorProcessor();
    private Flow<ArgsWrapper, ArgsWrapper, NotUsed> lastFlow;

    @Override
    public ApiEngine run() {
        stages.forEach(StageBase::init);
        if (isFromServer()) {
            ((TcpServerTransportImpl) stages.get(0)).run(lastFlow);
        } else {
            Source.empty(ArgsWrapper.class).via(lastFlow)
                    .map(this::checkError)
                    .mapError(new PFBuilder<Throwable, Throwable>()
                            .match(Exception.class, this::onError)
                            .build())
                    .run(ApiEngineContextProvider.getApiEngineContext().getActorSystem());
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

    public static ApiEngineImpl of(ApiServerBase server) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(server);
        return apiEngine;
    }

    public static ApiEngineImpl of(TcpServerTransportImpl tcpServer) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(tcpServer);
        return apiEngine;
    }

    public <T> T findProxy(Class<T> clazz) {
        if (stages.get(0) instanceof ProxiedStage) {
            ProxiedStage proxiedStage = (ProxiedStage) stages.get(0);
            return proxiedStage.getProxy(clazz);
        }
        throw new ApiEngineException("ApiEngine start point is not ProxiedStage");
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

    private ApiEngineImpl(ApiServerBase server) {
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
        if (stage instanceof TcpServerTransportImpl) {
            throw new ApiEngineException();
        }
        if (stage instanceof TcpClientTransportImpl) {
            ApiCallProcessor apiCallProcessor = findClientApiCallProcessor();
            if (apiCallProcessor == null) {
                throw new ApiEngineException();
            }
            ((TcpClientTransportImpl) stage).withApiCallProcessor(apiCallProcessor);
        }
        lastFlow = lastFlow.via(stage.getStageConnector());
        stages.add(stage);
        return this;
    }

    private ApiCallProcessor findClientApiCallProcessor() {
        for (int i = stages.size() - 1; i >= 0; i++) {
            if (stages.get(i) instanceof ApiClientImpl) {
                return ((ApiClientImpl) stages.get(i)).getApiCallProcessor();
            }
        }
        return null;
    }

    private boolean isFromServer() {
        return stages.get(0) instanceof TcpServerTransportImpl;
    }

    protected Exception onError(Exception e) throws Exception {
        return errorProcessor.onError(e);
    }

    public ApiEngineImpl withErrorListener(Consumer<Exception> errorListener) {
        errorProcessor.setErrorListener(errorListener);
        return this;
    }

    protected ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        return errorProcessor.checkError(argsWrapper);
    }
}
