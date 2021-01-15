package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletionStage;

public class TcpApiServer extends TcpPointBase {


    public TcpApiServer(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    void runApiServer() {
        ActorSystem system = ActorSystem.create("QuickStart");
        final Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(system).bind("127.0.0.1", 8889);
        connections
                .to(
                        Sink.foreach(
                                (Tcp.IncomingConnection connection) -> {
                                    final Message welcomeMsg = Message.builder().messageType(Message.MessageType.WELCOME).build();
                                    final Source<Message, NotUsed> welcome = Source.single(welcomeMsg);
                                    final Flow<ByteString, ByteString, NotUsed> serverLogic =
                                            Flow.of(ByteString.class)
                                                    .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                                                    .map(ByteString::utf8String)
                                                    .map(this::messageFromString)
                                                    .merge(welcome)
                                                    .map(message -> {
                                                        //Обработка вызова API
                                                        if (message.getMessageType() == Message.MessageType.REQUEST) {
                                                            System.out.println(String.format("Api call with correlationId: %d. Message: %s", message.getCorrelationId(), messageToString(message)));
                                                            CustomMethodParameter customMethodParameter = parameterFromBase64String(message.getBase64Json());
                                                            customMethodParameter.setName(customMethodParameter.getName().concat(" ").concat("Ganichev"));
                                                            message.setBase64Json(toBase64String(customMethodParameter));
                                                        }
                                                        return message;
                                                    })
                                                    .map(message -> ByteString.fromString(this.toString(message)));

                                    connection.handleWith(serverLogic, system);
                                }))
                .run(system);
    }

    public static void main(String args[]) {
        ObjectMapper objectMapper = new ObjectMapper();
        TcpApiServer tcpApiServer = new TcpApiServer(objectMapper);
        tcpApiServer.runApiServer();
    }
}
