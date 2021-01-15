package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

public class TcpFileAsyncPowerServer extends TcpPointBase {

    private static final int API_SIZE = 1024;
    private static final int THREADS_AMOUNT = 4;
    private static final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();
    private static final Map<AsynchronousSocketChannel, ReadCompletionHandler> readHandlers = new ConcurrentHashMap<>();
    private static final Map<AsynchronousSocketChannel, WriteCompletionHandler> writeHandlers = new ConcurrentHashMap<>();
    private static final Map<AsynchronousSocketChannel, Semaphore> readSemaphores = new ConcurrentHashMap<>();
    private static final Map<AsynchronousSocketChannel, Semaphore> writeSemaphores = new ConcurrentHashMap<>();
    //private static final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private static final ExecutorService forkJoinPool = Executors.newFixedThreadPool(THREADS_AMOUNT);

    public TcpFileAsyncPowerServer(ObjectMapper objectMapper) throws IOException {
        super(objectMapper);
    }

    void runApiServer() throws IOException, ExecutionException, InterruptedException {
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8889));
        AcceptCompletionHandler acceptCompletionHandler = new AcceptCompletionHandler(serverSocketChannel);
        serverSocketChannel.accept(null, acceptCompletionHandler);
        System.in.read();
    }

    private void nextResponse(AsynchronousSocketChannel socketChannel) throws IOException, InterruptedException {
        Message message;
        while ((message = messageQueue.poll()) != null) {
            String mesStr = messageToString(message);
            byte[] mesBytes = mesStr.getBytes("utf-8");
            ByteBuffer buffer = ByteBuffer.wrap(mesBytes);
            writeSemaphores.computeIfAbsent(socketChannel, c -> new Semaphore(1)).acquire();
            socketChannel.write(buffer, buffer, writeHandlers.computeIfAbsent(socketChannel, c -> new WriteCompletionHandler(c)));
        }
    }

    private void processCall(ByteBuffer buffer) {
        buffer.flip();
        byte[] content = new byte[buffer.remaining()];
        buffer.get(content);
        String data = new String(buffer.array()).trim();
        List<Message> messages = messagesFromString(data);
        for (Message message : messages) {
            if (message.getMessageType() == Message.MessageType.REQUEST) {
                //System.out.println(String.format("Api call with correlationId: %d. Message: %s", message.getCorrelationId(), messageToString(message)));
                CustomMethodParameter customMethodParameter = parameterFromBase64String(message.getBase64Json());
                customMethodParameter.setName(customMethodParameter.getName().concat(" ").concat("Ganichev"));
                message.setBase64Json(toBase64String(customMethodParameter));
                work(POWER_BASE);
                messageQueue.add(message);
            }
        }
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

    private void nextRequest(AsynchronousSocketChannel socketChannel) throws InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(API_SIZE);
        readSemaphores.computeIfAbsent(socketChannel, c -> new Semaphore(1)).acquire();
        socketChannel.read(buffer, buffer, readHandlers.computeIfAbsent(socketChannel, c -> new ReadCompletionHandler(c)));
    }

    private class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

        private final AsynchronousSocketChannel socketChannel;

        private ReadCompletionHandler(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @SneakyThrows
        @Override
        public void completed(Integer bytesRead, ByteBuffer attachment) {
            //System.out.printf("Server read. Bytes read %d%n", bytesRead);
            readSemaphores.computeIfAbsent(socketChannel, c -> new Semaphore(1)).release();
            forkJoinPool.execute(() -> {
                try {
                    processCall(attachment);
                    nextResponse(socketChannel);
                } catch (Exception e) {
                    throw new ApiException(e);
                }
            });
        }

        @Override
        public void failed(Throwable t, ByteBuffer attachment) {
            t.printStackTrace();
        }
    }

    private class WriteCompletionHandler implements CompletionHandler<Integer, ByteBuffer> {

        private final AsynchronousSocketChannel socketChannel;

        private WriteCompletionHandler(AsynchronousSocketChannel socketChannel) {
            this.socketChannel = socketChannel;
        }

        @SneakyThrows
        @Override
        public void completed(Integer bytesWritten, ByteBuffer attachment) {
            //System.out.printf("Server write completed: %d bytes wrote%n", bytesWritten);
            writeSemaphores.computeIfAbsent(socketChannel, c -> new Semaphore(1)).release();
            forkJoinPool.execute(() -> {
                try {
                    nextRequest(socketChannel);
                } catch (Exception e) {
                    throw new ApiException(e);
                }
            });
        }

        @Override
        public void failed(Throwable t, ByteBuffer attachment) {
            t.printStackTrace();
        }
    }

    private class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
        private final AsynchronousServerSocketChannel serverSocketChannel;

        AcceptCompletionHandler(AsynchronousServerSocketChannel serverSocketChannel) {
            this.serverSocketChannel = serverSocketChannel;
        }

        @SneakyThrows
        @Override
        public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
            serverSocketChannel.accept(null, this);
            nextRequest(socketChannel);
        }

        @Override
        public void failed(Throwable t, Void attachment) {
        }
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TcpFileAsyncPowerServer tcpFileAsyncServer = new TcpFileAsyncPowerServer(objectMapper);
        tcpFileAsyncServer.runApiServer();
    }
}
