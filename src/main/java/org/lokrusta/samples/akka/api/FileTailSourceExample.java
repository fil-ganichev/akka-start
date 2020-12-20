package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class FileTailSourceExample {

    private ActorSystem system = ActorSystem.create("QuickStart");

    private final FileSystem fs = FileSystems.getDefault();
    private final Duration pollingInterval = Duration.ofMillis(250);
    private final int maxLineSize = 8192;

    private static final String FILE_NAME = "C:/tmp/data.txt";
    private static final long WRITE_PER_SECOND = 70000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Source<String, NotUsed> lines =
            akka.stream.alpakka.file.javadsl.FileTailSource
                    .createLines(fs.getPath(FILE_NAME), maxLineSize, pollingInterval, "\n", Charset.forName("utf8"))
                    .log("error");

    private final Flow<String, ByteString, NotUsed> linesSource = Flow.<String>create()
            .map(nextString -> fromString(nextString, CustomMethodParameter.class))
            .log("error")
            .map(parameter -> {
                System.out.println(this.toString(parameter));
                return ByteString.emptyByteString();
            })
            .log("error");


    public void runExample() {
        writeFile();
        lines.via(linesSource).run(ActorMaterializer.create(system));
        System.out.println("FileTailSourceExample shutdown");
    }

    private void writeFile() {
        final RateLimiter rateLimiter = RateLimiter.create(WRITE_PER_SECOND);
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
                    bwr.write(toString(parameter));
                    bwr.write("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    protected <T> T fromString(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    protected String toString(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static void main(String[] args) {
        final FileTailSourceExample fileTailSourceExample = new FileTailSourceExample();
        fileTailSourceExample.runExample();
    }
}
