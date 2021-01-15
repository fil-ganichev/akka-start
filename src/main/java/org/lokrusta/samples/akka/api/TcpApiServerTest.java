package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.impl.ApiHelper;
import org.lokrusta.prototypes.connect.impl.ArgsWrapperImpl;
import org.lokrusta.prototypes.connect.impl.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TcpApiServerTest {

    void runApiServer() {

        ActorSystem actorSystem = ActorSystem.create("QuickStart2");
        Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(actorSystem).bind("127.0.0.1", 8889);
        org.lokrusta.prototypes.connect.impl.Message welcomeMsg = ApiHelper.welcome();
        Source<org.lokrusta.prototypes.connect.impl.Message, NotUsed> welcome = Source.single(welcomeMsg);

        Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor = Flow.of(ArgsWrapper.class)
                .map(value -> {
                    System.out.println("Server call: " + value);
                    return ArgsWrapperImpl.of(new String[]{"Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев"}).withCorrelationId(value.getCorrelationId());
                });

        final Flow<ByteString, ByteString, NotUsed> serverLogic = Flow.of(ByteString.class).log("")
                .via(JsonFraming.objectScanner(Integer.MAX_VALUE)).log("")
                .map(ByteString::utf8String).log("")
                .map(ApiHelper::messageFromString).log("")
                .map(Message::getBase64Json).log("")
                .map(ApiHelper::parameterFromBase64String).log("")
                .via(graphProcessor).log("")
                .map(ApiHelper::messageFromArgs).log("")
                .merge(welcome).log("")
                .map(ApiHelper::messageToString).log("")
                .map(ByteString::fromString).log("");

        connections.log("").via(Flow.of(Tcp.IncomingConnection.class).log("").map(
                (Tcp.IncomingConnection connection) -> {
                    connection.handleWith(serverLogic, actorSystem);
                    return connection;
                })).log("").run(actorSystem);
    }

    public static void main(String args[]) {
        TcpApiServerTest tcpApiServer = new TcpApiServerTest();
        tcpApiServer.runApiServer();
    }
}
