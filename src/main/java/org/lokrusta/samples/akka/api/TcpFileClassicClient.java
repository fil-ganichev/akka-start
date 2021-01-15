package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TcpFileClassicClient extends TcpPointBase {

    //private static final int LOG_STEP = 100000;
    private static final int LOG_STEP = 1000;
    private static final String FILE_NAME = "C:/tmp/data.txt";
    private static final int THREADS_AMOUNT = 8;
    private static final int CALL_QUEUE_SIZE = 50000;
    private static final int API_PORTION = 1000;
    private static final int SO_TIMEOUT = 500;

    private ApiProcessor apiProcessor;
    private final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
    private final Counter callsCounter = simpleMeterRegistry.counter("callsCounter");
    private final Counter totalTimeCounter = simpleMeterRegistry.counter("totalTimeCounter");
    private long startTimeMs;
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(THREADS_AMOUNT, THREADS_AMOUNT, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<>(CALL_QUEUE_SIZE), new SocketThreadFactory());

    TcpFileClassicClient(ObjectMapper objectMapper) {
        super(objectMapper);
        apiProcessor = new ApiProcessor(1, objectMapper);
    }

    void runApiClient() throws IOException {
        Path path = Paths.get(FILE_NAME);
        startTimeMs = System.currentTimeMillis();
        try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
            String currentLine = null;
            List<String> strings = new ArrayList<>();
            while ((currentLine = reader.readLine()) != null) {
                strings.add(currentLine);
                if (strings.size() >= API_PORTION) {
                    final List<String> process = new ArrayList<>(strings);
                    strings.clear();
                    executorService.execute(() -> {
                        SocketThreadFactory.SocketThread socketThread = (SocketThreadFactory.SocketThread) Thread.currentThread();
                        Socket clientSocket = socketThread.getClientSocket();
                        try {
                            clientSocket.setSoTimeout(SO_TIMEOUT);
                            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            for (String next : process) {
                                Message message = callApi(next);
                                out.println(this.toString(message));
                            }
                            try {
                                String answer;
                                while ((answer = in.readLine()) != null) {
                                    Message message = messageFromString(answer);
                                    apiProcessor.response(message);
                                }
                            } catch (SocketTimeoutException e) {
                                //System.out.println(e);
                            }

                        } catch (Exception e) {
                            throw new ApiException(e);
                        }
                    });
                }
            }
        }
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
                System.out.printf("API calls: %.0f, Total time, sec: %.0f. Queue size: %d%n", callsCounter.count(), totalTimeCounter.count() / 1000, apiProcessor.queueSize());
            }
            callsCounter.increment();
            return message;
        });
        return response.getLeft();
    }


    private class SocketThreadFactory implements ThreadFactory {

        @SneakyThrows
        @Override
        public Thread newThread(Runnable r) {
            return new SocketThread(r);
        }

        private class SocketThread extends Thread {
            private final Socket clientSocket;

            public SocketThread(Runnable r) throws IOException {
                super(r);
                clientSocket = new Socket("localhost", 8889);
            }

            public Socket getClientSocket() {
                return clientSocket;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TcpFileClassicClient tcpFileClassicClient = new TcpFileClassicClient(objectMapper);
        tcpFileClassicClient.runApiClient();
    }
}
