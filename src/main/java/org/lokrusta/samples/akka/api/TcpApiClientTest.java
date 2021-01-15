package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.*;
import akka.stream.javadsl.Flow;
import akka.util.ByteString;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.impl.ApiCallProcessor;
import org.lokrusta.prototypes.connect.impl.ApiHelper;
import org.lokrusta.prototypes.connect.impl.ArgsWrapperImpl;
import org.lokrusta.prototypes.connect.impl.Message;

import java.io.IOException;
import java.util.concurrent.*;

public class TcpApiClientTest {

    private final ApiCallProcessor apiCallProcessor;

    private final static BlockingQueue<ArgsWrapper> queue = new ArrayBlockingQueue<>(1000);

    private final static SubmissionPublisher<ArgsWrapper> submissionPublisher = new SubmissionPublisher<>();

    public TcpApiClientTest(ApiCallProcessor apiCallProcessor) {
        this.apiCallProcessor = apiCallProcessor;
    }

    void runApiClient() {


        ActorSystem system = ActorSystem.create("QuickStart");

        //final Flow<ArgsWrapper, ArgsWrapper, NotUsed> clientFlow = Flow.of(ArgsWrapper.class).log("").merge(apiCallProcessor.clientApiSource()).log("");

        //final Flow<ArgsWrapper, ArgsWrapper, NotUsed> clientFlow = Flow.of(ArgsWrapper.class).log("").merge(Source.repeat("")
        //        .map(v -> ArgsWrapperImpl.of(new String[]{"Москва, Минск, Киев, Таллин, Рига, Кишинев"})
        //                .withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"))).log("");

        //final Flow<ArgsWrapper, ArgsWrapper, NotUsed> clientFlow = Flow.of(ArgsWrapper.class).log("").merge(Source.single("")
        //        .map(v -> ArgsWrapperImpl.of(new String[]{"Москва, Минск, Киев, Таллин, Рига, Кишинев"})
        //                .withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"))).log("");

        //final Flow<ArgsWrapper, ArgsWrapper, NotUsed> clientFlow = Flow.of(ArgsWrapper.class)
        //        .merge(Source.repeat("").map(v -> queue.take()))
        //        .log("");

        Source<ArgsWrapper, NotUsed> apiSource =
                JavaFlowSupport.Source.<ArgsWrapper>asSubscriber()
                        .mapMaterializedValue(
                                subscriber -> {
                                    submissionPublisher.subscribe(subscriber);
                                    return NotUsed.getInstance();
                                });

        final Flow<ArgsWrapper, ArgsWrapper, NotUsed> clientFlow = Flow.of(ArgsWrapper.class)
                .merge(apiSource)
                .log("");

        final Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(system).outgoingConnection("127.0.0.1", 8889);

        Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class).log("")
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE)).log("")
                        .map(ByteString::utf8String).log("")
                        .map(ApiHelper::messageFromString).log("")
                        .map(message -> {
                            if (message.getMessageType() == Message.MessageType.REQUEST) {
                                ArgsWrapper argsWrapper = ApiHelper.parameterFromBase64String(message.getBase64Json());
                                apiCallProcessor.response(argsWrapper);
                            }
                            return ByteString.emptyByteString();
                        }).log("");

        final Flow<ArgsWrapper, ArgsWrapper, NotUsed> tcp = Flow.of(ArgsWrapper.class).log("")
                .map(ApiHelper::messageFromArgs).log("")
                .map(ApiHelper::messageToString).log("")
                .map(ByteString::fromString).log("")
                .via(connection).log("")
                .via(repl).log("")
                .map(s -> (ArgsWrapper) (ArgsWrapperImpl.of((String) null))).log("");

        Source.empty(ArgsWrapper.class).via(clientFlow).via(tcp).run(system);
        System.out.println("TcpApiClientTest shutdown...");
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        final ApiCallProcessor apiCallProcessor = new ApiCallProcessor();
        final TcpApiClientTest tcpApiClient = new TcpApiClientTest(apiCallProcessor);
        tcpApiClient.runApiClient();
        //apiCallProcessor.request(ArgsWrapperImpl.of(new String[]{"Москва, Минск, Киев, Таллин, Рига, Кишинев"}).withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"));
        //queue.put(ArgsWrapperImpl.of(new String[]{"Москва, Минск, Киев, Таллин, Рига, Кишинев"}).withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"));
        submissionPublisher.submit(ArgsWrapperImpl.of(new String[]{"Москва, Минск, Киев, Таллин, Рига, Кишинев"}).withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"));
        submissionPublisher.submit(ArgsWrapperImpl.of(new String[]{"Подольск"}).withCorrelationId("3852ea56-11f4-4c2a-aa1b-0322f1f35cdc"));
        System.in.read();
    }

}
