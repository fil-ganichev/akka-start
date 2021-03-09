package juddy.transport.impl.engine;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.common.ProxiedStage;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.client.ApiClientImpl;
import juddy.transport.impl.common.*;
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
    private List<Flow<ArgsWrapper, ArgsWrapper, NotUsed>> runnableFlows = new ArrayList<>();

    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;

    private ApiEngineImpl(StageBase stageBase) {
        stages.add(stageBase);
        runnableFlows.add(stageBase.getStageConnector());
    }

    @Override
    public ApiEngine run() {
        stages.forEach(StageBase::init);
        for (int i = runnableFlows.size() - 1; i >= 0; i--) {
            Flow<ArgsWrapper, ArgsWrapper, NotUsed> runnableFlow = runnableFlows.get(i);
            if (i == 0 && isRunnableStage()) {
                ((RunnableStage) stages.get(0)).run(runnableFlow);
            } else {
                Source.empty(ArgsWrapper.class).via(runnableFlow)
                        .map(this::checkError)
                        .mapError(new PFBuilder<Throwable, Throwable>()
                                .match(Exception.class, this::onError)
                                .build())
                        .run(apiEngineContextProvider.getApiEngineContext().getActorSystem());
            }
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
        if (apiCallProcessor == null && tcpClientTransport.getTransportMode() == TransportMode.API_CALL) {
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

    private boolean isRunnableStage() {
        return firstStage() instanceof RunnableStage;
    }

    private ApiEngineImpl connectStage(StageBase stageBase) {
        connectFlow(stageBase.getStageConnector());
        if (isNewSource(stageBase)) {
            newRunnableFlow(((NewSource) stageBase).getNewSource());
        }
        stages.add(stageBase);
        return this;
    }

    private StageBase firstStage() {
        return stages.get(0);
    }

    private boolean isNewSource(StageBase stageBase) {
        return stageBase instanceof NewSource && ((NewSource) stageBase).enabled();
    }

    private void newRunnableFlow(Flow<ArgsWrapper, ArgsWrapper, NotUsed> connector) {
        runnableFlows.add(connector);
    }

    private void connectFlow(Flow<ArgsWrapper, ArgsWrapper, NotUsed> connector) {
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> currentFlow = runnableFlows.get(runnableFlows.size() - 1);
        currentFlow = currentFlow.via(connector);
        runnableFlows.set(runnableFlows.size() - 1, currentFlow);
    }
}
