package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TcpFileClassicServer extends TcpPointBase {

    private static final int THREADS_AMOUNT = 8;
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(THREADS_AMOUNT, THREADS_AMOUNT, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new SocketThreadFactory(new ServerSocket(8888)));

    public TcpFileClassicServer(ObjectMapper objectMapper) throws IOException {
        super(objectMapper);
    }

    public void runApiServer() {
        for (int i = 0; i < THREADS_AMOUNT; i++)
            threadPoolExecutor.execute(() -> {
                SocketThreadFactory.SocketThread socketThread = (SocketThreadFactory.SocketThread) Thread.currentThread();
                try {
                    Socket clientSocket = socketThread.getServerSocket().accept();
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    while (!socketThread.isInterrupted()) {
                        String nextLine = in.readLine();
                        Message message = messageFromString(nextLine);
                        if (message.getMessageType() == Message.MessageType.REQUEST) {
                            //System.out.println(String.format("Api call with correlationId: %d. Message: %s", message.getCorrelationId(), messageToString(message)));
                            CustomMethodParameter customMethodParameter = parameterFromBase64String(message.getBase64Json());
                            customMethodParameter.setName(customMethodParameter.getName().concat(" ").concat("Ganichev"));
                            message.setBase64Json(toBase64String(customMethodParameter));
                            work(POWER_BASE);
                            out.println(messageToString(message));
                        }

                    }
                } catch (IOException e) {
                    try {
                        socketThread.getServerSocket().close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    throw new ApiException(e);
                }
            });
    }

    public static void main(String[] args) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TcpFileClassicServer tcpFileClassicServer = new TcpFileClassicServer(objectMapper);
        tcpFileClassicServer.runApiServer();
    }


    private class SocketThreadFactory implements ThreadFactory {

        private ServerSocket serverSocket;

        public SocketThreadFactory(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @SneakyThrows
        @Override
        public Thread newThread(Runnable r) {
            return new TcpFileClassicServer.SocketThreadFactory.SocketThread(r, serverSocket);
        }

        private class SocketThread extends Thread {
            private final ServerSocket serverSocket;

            public SocketThread(Runnable r, ServerSocket serverSocket) throws IOException {
                super(r);
                this.serverSocket = serverSocket;
            }

            public ServerSocket getServerSocket() {
                return serverSocket;
            }
        }
    }
}
