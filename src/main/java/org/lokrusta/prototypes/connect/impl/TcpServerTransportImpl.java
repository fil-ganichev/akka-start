package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import org.lokrusta.prototypes.connect.api.ApiTransport;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;

import java.util.concurrent.CompletionStage;

public class TcpServerTransportImpl extends StageBase implements ApiTransport {

    private final ActorSystem actorSystem;
    private final String host;
    private final int port;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> getStageConnector() {
        return stageConnector;
    }

    public TcpServerTransportImpl(ActorSystem actorSystem, ApiCallProcessor apiCallProcessor, String host, int port) {
        this.actorSystem = actorSystem;
        this.host = host;
        this.port = port;
        this.stageConnector = createConnector();
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        stageConnector = Flow.of(ArgsWrapper.class);
        return stageConnector;
    }

    protected void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {
        //todo Вызывается из ApiEngine. Входной параметр = stageConnector.via().via()...
        Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(actorSystem).bind(host, port);
        Message welcomeMsg = Message.builder().messageType(Message.MessageType.WELCOME).build();

        Source<Message, NotUsed> welcome = Source.single(welcomeMsg);

        final Flow<ByteString, ByteString, NotUsed> serverLogic = Flow.of(ByteString.class)
                .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                .map(ByteString::utf8String)
                .map(ApiHelper::messageFromString)
                .merge(welcome)
                .map(Message::getBase64Json)
                .map(ApiHelper::parameterFromBase64String)
                .via(graphProcessor)
                .map(ApiHelper::messageFromArgs)
                .map(ApiHelper::messageToString)
                .map(ByteString::fromString);

        connections.via(Flow.of(Tcp.IncomingConnection.class).map(
                (Tcp.IncomingConnection connection) -> {
                    connection.handleWith(serverLogic, actorSystem);
                    return connection;
                })).run(actorSystem);
    }

    @Override
    public void init() {
    }
}
