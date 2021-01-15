package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import org.lokrusta.prototypes.connect.api.ApiTransport;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContext;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.CompletionStage;

public class TcpClientTransportImpl extends StageBase implements ApiTransport, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String host;
    private final int port;
    private final ApiEngineContext apiEngineContext = ApiEngineContextProvider.getApiEngineContext();
    private ApiCallProcessor apiCallProcessor;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> getStageConnector() {
        return stageConnector;
    }

    protected TcpClientTransportImpl(ApiCallProcessor apiCallProcessor, String host, int port) {
        this.apiCallProcessor = apiCallProcessor;
        this.host = host;
        this.port = port;
    }

    protected TcpClientTransportImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TcpClientTransportImpl of(String host, int port) {
        return new TcpClientTransportImpl(host, port);
    }

    @Override
    public void init() {
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(apiEngineContext.getActorSystem()).outgoingConnection(host, port);

        Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class).log("")
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE)).log("")
                        .map(ByteString::utf8String).log("")
                        .map(ApiHelper::messageFromString).log("")
                        .map(message -> {
                            if (message.getMessageType() == Message.MessageType.REQUEST) {
                                ArgsWrapper argsWrapper = ApiHelper.parameterFromBase64String(message.getBase64Json());
                                apiCallProcessor.response(argsWrapper);
                            }
                            return ByteString.emptyByteString();
                        }).log("");

        return Flow.of(ArgsWrapper.class).log("")
                .map(this::next).log("")
                .map(ApiHelper::messageFromArgs).log("")
                .map(ApiHelper::messageToString).log("")
                .map(ByteString::fromString).log("")
                .via(connection).log("")
                .via(repl).log("")
                .map(s -> (ArgsWrapper) (ArgsWrapperImpl.of((String) null))).log("");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector();
    }

    protected TcpClientTransportImpl withApiCallProcessor(ApiCallProcessor apiCallProcessor) {
        this.apiCallProcessor = apiCallProcessor;
        return this;
    }
}
