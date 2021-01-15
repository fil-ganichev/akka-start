package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import org.lokrusta.prototypes.connect.api.ApiTransport;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.StringApiCallArguments;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContext;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class TcpServerTransportImpl extends StageBase implements ApiTransport, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String host;
    private final int port;
    private final ApiEngineContext apiEngineContext = ApiEngineContextProvider.getApiEngineContext();
    private Consumer<Exception> errorListener;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> getStageConnector() {
        return stageConnector;
    }

    private TcpServerTransportImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TcpServerTransportImpl of(String host, int port) {
        return new TcpServerTransportImpl(host, port);
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        stageConnector = Flow.of(ArgsWrapper.class);
        return stageConnector;
    }

    protected void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {
        ActorSystem actorSystem = apiEngineContext.getActorSystem();
        Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(actorSystem).bind(host, port);
        Message welcomeMsg = ApiHelper.welcome();
        Source<Message, NotUsed> welcome = Source.single(welcomeMsg);

        final Flow<ByteString, ByteString, NotUsed> serverLogic = Flow.of(ByteString.class).log("")
                .via(JsonFraming.objectScanner(Integer.MAX_VALUE)).log("")
                .map(ByteString::utf8String).log("")
                .map(ApiHelper::messageFromString).log("")
                .map(Message::getBase64Json).log("")
                .map(ApiHelper::parameterFromBase64String).log("")
                .via(graphProcessor).log("")
                .map(this::checkError).log("")
                .map(ApiHelper::messageFromArgs).log("")
                .merge(welcome).log("")
                .map(ApiHelper::messageToString).log("")
                .map(ByteString::fromString).log("")
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build())
                .log("");
        connections.log("").via(Flow.of(Tcp.IncomingConnection.class).log("").map(
                (Tcp.IncomingConnection connection) -> {
                    connection.handleWith(serverLogic, actorSystem);
                    return connection;
                })).log("").run(actorSystem);
    }

    @Override
    public void init() {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector();
    }

    protected Exception onError(Exception e) throws Exception {
        if (errorListener != null) {
            errorListener.accept(e);
        }
        return e;
    }

    public TcpServerTransportImpl withErrorListener(Consumer<Exception> errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    protected ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        if (argsWrapper.getException() != null) {
            throw argsWrapper.getException();
        }
        return argsWrapper;
    }
}
