package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TcpApiFileClient extends TcpPointBase {
    private final ApiProcessor apiProcessor;
    private static final long CALLS_PER_SECOND = 70000;
    private static final int CALL_QUEUE_SIZE = 50000;
    private static final String FILE_NAME = "C:/tmp/data.txt";

    public TcpApiFileClient(ObjectMapper objectMapper,
                            ApiProcessor apiProcessor) {
        super(objectMapper);
        this.apiProcessor = apiProcessor;
    }

    void runApiClient() {
        ActorSystem system = ActorSystem.create("QuickStart");

        final FileSystem fs = FileSystems.getDefault();
        final Duration pollingInterval = Duration.ofMillis(250);
        final int maxLineSize = 8192;

        final Source<String, NotUsed> lines =
                akka.stream.alpakka.file.javadsl.FileTailSource
                        .createLines(fs.getPath(FILE_NAME), maxLineSize, pollingInterval, "\n", Charset.forName("utf8"))
                        .log("error");

        final Flow<String, ByteString, NotUsed> apiSource = Flow.<String>create()
                .map(nextString -> fromString(nextString, CustomMethodParameter.class))
                .log("error")
                .map(parameter -> {
                    Pair<Message, CompletableFuture<Message>> response = apiProcessor.req(parameter);
                    response.getRight().thenApply(message -> {
                        System.out.println("Api succsessfully called: "
                                .concat(messageToString(message)));
                        return message;
                    });
                    return ByteString.fromString(this.toString(response.getLeft()));
                })
                .log("error");

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
                            return ByteString.emptyByteString();
                        });
        lines.via(apiSource).via(connection).via(repl).run(system);
        System.out.println("TcpApiFileClient shutdown..");
    }

    public static void main(String args[]) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ApiProcessor apiProcessor = new ApiProcessor(CALL_QUEUE_SIZE, objectMapper);
        final TcpApiFileClient tcpApiClient = new TcpApiFileClient(objectMapper, apiProcessor);
        writeFile(tcpApiClient);
        tcpApiClient.runApiClient();
    }

    private static void writeFile(TcpApiFileClient tcpApiClient) {
        final RateLimiter rateLimiter = RateLimiter.create(CALLS_PER_SECOND);
        CompletableFuture.runAsync(() -> {
            Thread thread = Thread.currentThread();
            thread.setName("File-thread");
            FileWriter writer = null;
            try {
                writer = new FileWriter(FILE_NAME);
                BufferedWriter bwr = new BufferedWriter(writer);
                while (!thread.isInterrupted()) {
                    rateLimiter.acquire();
                    CustomMethodParameter parameter = CustomMethodParameter.builder()
                            .age(28)
                            .name("Filipp")
                            .build();
                    bwr.write(tcpApiClient.toString(parameter));
                    bwr.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
