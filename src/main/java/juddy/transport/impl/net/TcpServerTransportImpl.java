package juddy.transport.impl.net;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import juddy.transport.impl.common.ApiSerialilizer;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.StageBase;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.api.args.ArgsWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletionStage;

public final class TcpServerTransportImpl extends StageBase implements ApiTransport {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String host;
    private final int port;
    @Autowired
    private ApiSerialilizer apiSerialilizer;

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

    public void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {
        ActorSystem actorSystem = getApiEngineContext().getActorSystem();
        Source<Tcp.IncomingConnection, CompletionStage<Tcp.ServerBinding>> connections =
                Tcp.get(actorSystem).bind(host, port);
        Message welcomeMsg = apiSerialilizer.welcome();
        Source<Message, NotUsed> welcome = Source.single(welcomeMsg);

        Flow<ByteString, ByteString, NotUsed> serverLogic = Flow.of(ByteString.class)
                .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                .map(ByteString::utf8String)
                .map(apiSerialilizer::messageFromString)
                .log(logTitle("incoming message"))
                .map(Message::getBase64Json)
                .map(apiSerialilizer::parameterFromBase64String)
                .via(graphProcessor)
                .map(this::checkError)
                .map(apiSerialilizer::messageFromArgs)
                .merge(welcome)
                .map(apiSerialilizer::messageToString)
                .log(logTitle("outgoing message"))
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
