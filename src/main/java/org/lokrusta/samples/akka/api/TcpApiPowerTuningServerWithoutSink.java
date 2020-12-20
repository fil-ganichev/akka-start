package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TcpApiPowerTuningServerWithoutSink extends TcpPointBase {

    private static final int PARALLELISM = 8;

    public TcpApiPowerTuningServerWithoutSink(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    void runApiServer() {
        ActorSystem system = ActorSystem.create("QuickStart");
        final Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(system).bind("127.0.0.1", 8888);

        connections.via(Flow.of(Tcp.IncomingConnection.class).map(
                (Tcp.IncomingConnection connection) -> {
                    final Message welcomeMsg = Message.builder().messageType(Message.MessageType.WELCOME).build();
                    final Source<Message, NotUsed> welcome = Source.single(welcomeMsg);
                    final Flow<ByteString, ByteString, NotUsed> serverLogic =
                            Flow.of(ByteString.class)
                                    .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                                    .map(ByteString::utf8String)
                                    .map(this::messageFromString)
                                    .merge(welcome)
                                    .async()
                                    .mapAsyncUnordered(PARALLELISM, message -> CompletableFuture.supplyAsync(() -> {
                                        //Обработка вызова API
                                        if (message.getMessageType() == Message.MessageType.REQUEST) {
                                            //System.out.println(String.format("Api call with correlationId: %d. Message: %s", message.getCorrelationId(), messageToString(message)));
                                            CustomMethodParameter customMethodParameter = parameterFromBase64String(message.getBase64Json());
                                            customMethodParameter.setName(customMethodParameter.getName().concat(" ").concat("Ganichev"));
                                            message.setBase64Json(toBase64String(customMethodParameter));
                                            work(POWER_BASE);
                                        }
                                        return ByteString.fromString(this.toString(message));
                                    }));
                    connection.handleWith(serverLogic, system);
                    return connection;
                }))
                .run(system);
    }

    public static void main(String args[]) {
        ObjectMapper objectMapper = new ObjectMapper();
        TcpApiPowerTuningServerWithoutSink tcpApiServer = new TcpApiPowerTuningServerWithoutSink(objectMapper);
        tcpApiServer.runApiServer();
    }
}
