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
import juddy.transport.impl.server.ApiServerImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ApiEngineImpl implements ApiEngine {

    private final List<StageBase> stages = new ArrayList<>();
    private final ErrorProcessor errorProcessor = new ErrorProcessor();
    private Flow<ArgsWrapper, ArgsWrapper, NotUsed> lastFlow;

    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;

    private ApiEngineImpl(StageBase stageBase) {
        stages.add(stageBase);
        lastFlow = stageBase.getStageConnector();
    }

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
                    .run(apiEngineContextProvider.getApiEngineContext().getActorSystem());
        }
        return this;
    }

    public static ApiEngineImpl of(StageBase stageBase) {
        ApiEngineImpl apiEngine = new ApiEngineImpl(stageBase);
        return apiEngine;
    }

    public <T> T findProxy(Class<T> clazz) {
        if (stages.get(0) instanceof ProxiedStage) {
            ProxiedStage proxiedStage = (ProxiedStage) stages.get(0);
            return proxiedStage.getProxy(clazz);
        }
        throw new ApiEngineException("ApiEngine start point is not ProxiedStage");
    }

    public Object findServerBean(Class<?> clazz) {
        for (StageBase stage : stages) {
            if (stage instanceof ApiServerImpl) {
                Object bean = ((ApiServerImpl) stage).getBean(clazz);
                if (bean != null) {
                    return bean;
                }
            }
        }
        return null;
    }

    public ApiEngineImpl connect(StageBase stageBase) {
        return connectStage(stageBase);
    }

    public ApiEngineImpl connect(TcpServerTransportImpl tcpServerTransport) {
        throw new ApiEngineException();
    }

    public ApiEngineImpl connect(TcpClientTransportImpl tcpClientTransport) {
        ApiCallProcessor apiCallProcessor = findClientApiCallProcessor();
        if (apiCallProcessor == null) {
            throw new ApiEngineException();
        }
        tcpClientTransport.withApiCallProcessor(apiCallProcessor);
        return connectStage(tcpClientTransport);
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

    private ApiCallProcessor findClientApiCallProcessor() {
        for (int i = stages.size() - 1; i >= 0; i--) {
            if (stages.get(i) instanceof ApiClientImpl) {
                return ((ApiClientImpl) stages.get(i)).getApiCallProcessor();
            }
        }
        return null;
    }

    private boolean isFromServer() {
        return stages.get(0) instanceof TcpServerTransportImpl;
    }

    private ApiEngineImpl connectStage(StageBase stageBase) {
        lastFlow = lastFlow.via(stageBase.getStageConnector());
        stages.add(stageBase);
        return this;
    }
}
