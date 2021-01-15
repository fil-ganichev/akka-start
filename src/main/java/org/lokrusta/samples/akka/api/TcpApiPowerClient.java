package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TcpApiPowerClient extends TcpPointBase {

    private final ApiProcessor apiProcessor;
    private static final long CALLS_PER_SECOND = 70000;
    private static final int PARALLELISM = 20;
    private static final int CALL_QUEUE_SIZE = 50000;

    public TcpApiPowerClient(ObjectMapper objectMapper,
                             ApiProcessor apiProcessor) {
        super(objectMapper);
        this.apiProcessor = apiProcessor;
    }

    void runApiClient() {
        ActorSystem system = ActorSystem.create("QuickStart");

        final Flow<Message, ByteString, NotUsed> apiCall =
                Flow.<Message>create()
                        .mapAsync(PARALLELISM, message -> CompletableFuture.supplyAsync(() -> ByteString.fromString(this.toString(apiProcessor.next()))));


        final Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(system).outgoingConnection("127.0.0.1", 8889);

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
                        .async()
                        .via(apiCall)
                        .async();

        CompletionStage<Tcp.OutgoingConnection> connectionCS = connection.join(repl).run(system);
        System.out.println("TcpApiPowerClient shutdown..");
    }

    public static void main(String args[]) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ApiProcessor apiProcessor = new ApiProcessor(CALL_QUEUE_SIZE, objectMapper);
        final TcpApiClient tcpApiClient = new TcpApiClient(objectMapper, apiProcessor);
        final RateLimiter rateLimiter = RateLimiter.create(CALLS_PER_SECOND);
        CompletableFuture.runAsync(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("Api-generator-thread");
            while (!thread.isInterrupted()) {
                rateLimiter.acquire();
                CompletionStage<Message> response = apiProcessor.request(CustomMethodParameter.builder()
                        .age(28)
                        .name("Filipp")
                        .build());
                response.thenApply(message -> {
                    System.out.println("Api succsessfully called: "
                            .concat(tcpApiClient.messageToString(message)));
                    return message;
                });
            }
        });
        tcpApiClient.runApiClient();
    }
}
