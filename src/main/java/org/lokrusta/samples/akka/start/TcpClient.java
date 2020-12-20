package org.lokrusta.samples.akka.start;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.util.ByteString;

import java.util.concurrent.CompletionStage;

public class TcpClient {

    public static void main(String args[]) {
        chart();
    }

    private static void chart() {
        ActorSystem system = ActorSystem.create("QuickStart");
        final Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(system).outgoingConnection("127.0.0.1", 8888);

        final Flow<String, ByteString, NotUsed> replParser =
                Flow.<String>create()
                        .takeWhile(elem -> !elem.equals("q"))
                        .concat(Source.single("BYE")) // will run after the original flow completes
                        .map(elem -> ByteString.fromString(elem + "\n"));

        final Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class)
                        .via(Framing.delimiter(ByteString.fromString("\n"), 256, FramingTruncation.DISALLOW))
                        .map(ByteString::utf8String)
                        .map(
                                text -> {
                                    System.out.println("Server: " + text);
                                    return "next";
                                })
                        .map(elem -> System.console().readLine())
                        .via(replParser);

        CompletionStage<Tcp.OutgoingConnection> connectionCS = connection.join(repl).run(system);
    }
}
