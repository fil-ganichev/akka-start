package org.lokrusta.samples.akka.start;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.util.concurrent.CompletionStage;

public class TcpServer {


    public static void main(String args[]) {
        welcomeServer();
    }

    //echo -n 'Line of text' | nc 127.0.0.1 8889
    private static void simpleServer() {
        ActorSystem system = ActorSystem.create("QuickStart");
        final Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(system).bind("127.0.0.1", 8889);
        connections.runForeach(
                connection -> {
                    System.out.println("New connection from: " + connection.remoteAddress());

                    final Flow<ByteString, ByteString, NotUsed> echo =
                            Flow.of(ByteString.class)
                                    .via(
                                            Framing.delimiter(
                                                    ByteString.fromString("\n"), 256, FramingTruncation.DISALLOW))
                                    .map(ByteString::utf8String)
                                    .map(s -> s + "!!!\n")
                                    .map(ByteString::fromString);

                    connection.handleWith(echo, system);
                },
                system);
    }

    private static void welcomeServer() {
        ActorSystem system = ActorSystem.create("QuickStart");
        final Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(system).bind("127.0.0.1", 8889);
        connections
                .to(
                        Sink.foreach(
                                (Tcp.IncomingConnection connection) -> {
                                    // server logic, parses incoming commands
                                    final Flow<String, String, NotUsed> commandParser =
                                            Flow.<String>create()
                                                    .takeWhile(elem -> !elem.equals("BYE"))
                                                    .map(elem -> elem + "!");

                                    final String welcomeMsg =
                                            "Welcome to: "
                                                    + connection.localAddress()
                                                    + " you are: "
                                                    + connection.remoteAddress()
                                                    + "!";

                                    final Source<String, NotUsed> welcome = Source.single(welcomeMsg);
                                    final Flow<ByteString, ByteString, NotUsed> serverLogic =
                                            Flow.of(ByteString.class)
                                                    .via(
                                                            Framing.delimiter(
                                                                    ByteString.fromString("\n"), 256, FramingTruncation.DISALLOW))
                                                    .map(ByteString::utf8String)
                                                    .via(commandParser)
                                                    .merge(welcome)
                                                    .map(s -> s + "\n")
                                                    .map(ByteString::fromString);

                                    connection.handleWith(serverLogic, system);
                                }))
                .run(system);
    }
}
