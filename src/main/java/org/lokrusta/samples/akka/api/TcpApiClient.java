package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class TcpApiClient extends TcpPointBase {

    private final ApiProcessor apiProcessor;

    public TcpApiClient(ObjectMapper objectMapper,
                        ApiProcessor apiProcessor) {
        super(objectMapper);
        this.apiProcessor = apiProcessor;
    }

    void runApiClient() {
        ActorSystem system = ActorSystem.create("QuickStart");

        final Flow<Message, ByteString, NotUsed> apiCall =
                Flow.<Message>create()
                        .map(message -> ByteString.fromString(this.toString(apiProcessor.next())));


        final Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(system).outgoingConnection("127.0.0.1", 8888);

        final Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class)
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                        .map(ByteString::utf8String)
                        .map(this::messageFromString)
                        .map(message -> {
                            if (message.getMessageType() == Message.MessageType.REQUEST) {
                                apiProcessor.response(message);
                            }
                            return message;
                        })
                        .via(apiCall);

        CompletionStage<Tcp.OutgoingConnection> connectionCS = connection.join(repl).run(system);
    }

    public static void main(String args[]) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ApiProcessor apiProcessor = new ApiProcessor(10, objectMapper);
        final TcpApiClient tcpApiClient = new TcpApiClient(objectMapper, apiProcessor);
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            CompletionStage<Message> response = apiProcessor.request(CustomMethodParameter.builder()
                    .age(28)
                    .name("Filipp")
                    .build());
            response.thenApply(message -> {
                System.out.println("Api succsessfully called: "
                        .concat(tcpApiClient.messageToString(message)));
                return message;
            });
        }, 1, 1, TimeUnit.SECONDS);
        tcpApiClient.runApiClient();
    }
}
