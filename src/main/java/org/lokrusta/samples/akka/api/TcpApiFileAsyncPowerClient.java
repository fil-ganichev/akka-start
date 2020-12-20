package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TcpApiFileAsyncPowerClient extends TcpPointBase {

    //private static final int LOG_STEP = 100000;
    private static final int LOG_STEP = 1000;
    private static final String FILE_NAME = "C:/tmp/data.txt";
    private static final int API_SIZE = 1024;
    private static final int THREADS_AMOUNT = 4;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ApiProcessor apiProcessor = new ApiProcessor(1, objectMapper);
    private static final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
    private static final Counter callsCounter = simpleMeterRegistry.counter("callsCounter");
    private static final Counter totalTimeCounter = simpleMeterRegistry.counter("totalTimeCounter");

    private long startTimeMs;
    private ByteBuffer readBuffer;
    private ByteBuffer writeBuffer;
    private AsynchronousSocketChannel client;
    private BufferedReader reader;
    private final WriteCompletionHandler writeCompletionHandler = new WriteCompletionHandler();
    private final ReadCompletionHandler readCompletionHandler = new ReadCompletionHandler();

    public TcpApiFileAsyncPowerClient() {
        super(objectMapper);
    }

    public void runApiClient() throws Exception {
        Path path = Paths.get(FILE_NAME);
        startTimeMs = System.currentTimeMillis();
        client = AsynchronousSocketChannel.open();
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8888);
        Future<Void> future = client.connect(hostAddress);
        future.get();
        reader = Files.newBufferedReader(path, Charset.forName("UTF-8"));
        nextCall();
    }

    void nextCall() throws Exception {
        writeBuffer = ByteBuffer.allocate(API_SIZE);
        String currentLine = reader.readLine();
        Message message = callApi(currentLine);
        byte[] byteMsg = toString(message).getBytes("utf-8");
        writeBuffer.put(byteMsg);
        writeBuffer.flip();
        client.write(writeBuffer, null, writeCompletionHandler);
    }

    Message callApi(String nextString) {
        CustomMethodParameter parameter = fromString(nextString, CustomMethodParameter.class);
        Pair<Message, CompletableFuture<Message>> response = apiProcessor.req(parameter);
        response.getRight().thenApply(message -> {
            if (callsCounter.count() % LOG_STEP == 0) {
                System.out.println("Api succsessfully called: "
                        .concat(toString(message)));
                long currTimeMs = System.currentTimeMillis();
                totalTimeCounter.increment(currTimeMs - startTimeMs);
                startTimeMs = currTimeMs;
                System.out.printf("API calls: %.0f, Total time, sec: %.0f%n", callsCounter.count(), totalTimeCounter.count() / 1000);
            }
            callsCounter.increment();
            return message;
        });
        return response.getLeft();
    }

    private List<Message> messagesFromString(String data) {
        String[] splitted = data.split("\\n");
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < splitted.length; i++) {
                Message message = messageFromString(splitted[i]);
                messages.add(message);

        }
        return messages;
    }

    private void nextResponse() {
        readBuffer = ByteBuffer.allocate(API_SIZE);
        client.read(readBuffer, null, readCompletionHandler);
    }

    private void processResponse() {
        readBuffer.flip();
        byte[] content = new byte[readBuffer.remaining()];
        readBuffer.get(content);
        String data = new String(content).trim();
        List<Message> messages = messagesFromString(data);
        for (Message message : messages) {
            apiProcessor.response(message);
        }
    }

    private class ReadCompletionHandler implements CompletionHandler<Integer, Void> {

        private ReadCompletionHandler() {
        }

        @SneakyThrows
        @Override
        public void completed(Integer bytesRead, Void attachment) {
            //System.out.printf("Client read completed: %d bytes read%n", bytesRead);
            processResponse();
            nextCall();
        }

        @Override
        public void failed(Throwable t, Void attachment) {
            t.printStackTrace();
        }
    }

    private class WriteCompletionHandler implements CompletionHandler<Integer, Void> {
        private WriteCompletionHandler() {
        }

        @SneakyThrows
        @Override
        public void completed(Integer bytesWritten, Void attachment) {
            //System.out.printf("Client write completed: %d bytes wrote%n", bytesWritten);
            nextResponse();
        }

        @Override
        public void failed(Throwable t, Void attachment) {
            t.printStackTrace();
        }
    }

    public static void main(String args[]) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(TcpApiFileAsyncPowerClient.THREADS_AMOUNT);
        for (int i = 0; i < TcpApiFileAsyncPowerClient.THREADS_AMOUNT; i++) {
            threadPool.execute(() -> {
                try {
                    TcpApiFileAsyncPowerClient tcpApiFileAsyncClient = new TcpApiFileAsyncPowerClient();
                    tcpApiFileAsyncClient.runApiClient();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        System.in.read();
    }
}
