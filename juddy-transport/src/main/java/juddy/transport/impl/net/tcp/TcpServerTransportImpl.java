package juddy.transport.impl.net.tcp;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.common.RunnableStage;
import juddy.transport.impl.common.StageBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletionStage;

import static juddy.transport.impl.common.MessageConstants.INCOMING_MESSAGE;
import static juddy.transport.impl.common.MessageConstants.OUTGOING_MESSAGE;

public final class TcpServerTransportImpl extends StageBase implements ApiTransport, RunnableStage {

    private final String host;
    private final int port;
    @Autowired
    private ApiSerializer apiSerializer;

    private TcpServerTransportImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TcpServerTransportImpl of(String host, int port) {
        return new TcpServerTransportImpl(host, port);
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class);
    }

    @Override
    public void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {
        ActorSystem actorSystem = getApiEngineContext().getActorSystem();
        Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(actorSystem).bind(host, port);
        Message welcomeMsg = apiSerializer.welcome();
        Source<Message, NotUsed> welcome = Source.single(welcomeMsg);

        Flow<ByteString, ByteString, NotUsed> serverLogic = Flow.of(ByteString.class)
                .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                .map(ByteString::utf8String)
                .map(apiSerializer::messageFromString)
                .log(logTitle(INCOMING_MESSAGE))
                .map(Message::getBase64Json)
                .map(apiSerializer::parameterFromBase64String)
                .via(graphProcessor)
                .map(this::checkError)
                .map(apiSerializer::messageFromArgs)
                .merge(welcome)
                .map(apiSerializer::messageToString)
                .log(logTitle(OUTGOING_MESSAGE))
                .map(ByteString::fromString)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());

        connections.via(Flow.of(Tcp.IncomingConnection.class).map(
                (Tcp.IncomingConnection connection) -> {
                    connection.handleWith(serverLogic, actorSystem);
                    return connection;
                })).run(actorSystem);
    }
}
