package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TcpFileAsyncServer extends TcpPointBase {

    private static final int API_SIZE = 1024;
    private static final Queue<Message> messageQueue = new ConcurrentLinkedQueue<>();

    public TcpFileAsyncServer(ObjectMapper objectMapper) throws IOException {
        super(objectMapper);
    }

    void runApiServer() throws IOException {
        AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8888));
        AcceptCompletionHandler acceptCompletionHandler = new AcceptCompletionHandler(serverSocketChannel);
        serverSocketChannel.accept(null, acceptCompletionHandler);
        System.in.read();
    }

    private void nextResponse(AsynchronousSocketChannel socketChannel) throws IOException {
        Message message;
        while ((message = messageQueue.poll()) != null) {
            String mesStr = messageToString(message);
            byte[] mesBytes = mesStr.getBytes("utf-8");
            ByteBuffer buffer = ByteBuffer.wrap(mesBytes);
            socketChannel.write(buffer, buffer, new WriteCompletionHandler(socketChannel));
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

    private void nextRequest(AsynchronousSocketChannel socketChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(API_SIZE);
        socketChannel.read(buffer, buffer, new ReadCompletionHandler(socketChannel));
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
            processCall(attachment);
            nextResponse(socketChannel);
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
            nextRequest(socketChannel);
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
        TcpFileAsyncServer tcpFileAsyncServer = new TcpFileAsyncServer(objectMapper);
        tcpFileAsyncServer.runApiServer();
    }
}
