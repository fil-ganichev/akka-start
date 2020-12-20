package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import org.lokrusta.prototypes.connect.api.ApiTransport;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;

import java.util.concurrent.CompletionStage;

public class TcpClientTransportImpl extends StageBase implements ApiTransport {

    private final ActorSystem actorSystem;
    private final ApiCallProcessor apiCallProcessor;
    private final String host;
    private final int port;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> stageConnector;

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> getStageConnector() {
        return stageConnector;
    }

    public TcpClientTransportImpl(ActorSystem actorSystem, ApiCallProcessor apiCallProcessor, String host, int port) {
        this.actorSystem = actorSystem;
        this.apiCallProcessor = apiCallProcessor;
        this.host = host;
        this.port = port;
        this.stageConnector = createConnector();
    }

    @Override
    public void init() {
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(actorSystem).outgoingConnection(host, port);

        Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class)
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                        .map(ByteString::utf8String)
                        .map(ApiHelper::messageFromString)
                        .map(message -> {
                            if (message.getMessageType() == Message.MessageType.REQUEST) {
                                ArgsWrapper argsWrapper = ApiHelper.parameterFromBase64String(message.getBase64Json());
                                apiCallProcessor.response(argsWrapper);
                            }
                            return ByteString.emptyByteString();
                        });

        return Flow.of(ArgsWrapper.class)
                .map(this::next)
                .map(ApiHelper::messageFromArgs)
                .map(ApiHelper::messageToString)
                .map(ByteString::fromString)
                .via(connection)
                .via(repl)
                .map(s -> ArgsWrapperImpl.of((String) null));
    }
}
