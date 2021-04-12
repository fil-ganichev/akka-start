package juddy.transport.impl.net.tcp;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.*;
import juddy.transport.impl.publisher.ArgsWrapperSource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletionStage;

import static juddy.transport.impl.common.MessageConstants.INCOMING_MESSAGE;
import static juddy.transport.impl.common.MessageConstants.OUTGOING_MESSAGE;

public class TcpClientTransportImpl extends StageBaseWithCallProcessor implements ApiTransport, NewSource {

    @Getter
    private final String host;
    @Getter
    private final int port;
    @Getter
    private TransportMode transportMode;

    private ApiCallProcessor apiCallProcessor;
    private ArgsWrapperSource argsWrapperSource;
    @Autowired
    private ApiSerializer apiSerializer;

    protected TcpClientTransportImpl(ApiCallProcessor apiCallProcessor, String host, int port,
                                     TransportMode transportMode) {
        this.apiCallProcessor = apiCallProcessor;
        this.host = host;
        this.port = port;
        this.transportMode = transportMode;
        this.argsWrapperSource = new ArgsWrapperSource();
    }

    protected TcpClientTransportImpl(String host, int port) {
        this(null, host, port, TransportMode.API_CALL);
    }

    protected TcpClientTransportImpl(String host, int port, TransportMode transportMode) {
        this(null, host, port, transportMode);
    }

    public static TcpClientTransportImpl of(ApiCallProcessor apiCallProcessor, String host, int port) {
        return new TcpClientTransportImpl(apiCallProcessor, host, port, TransportMode.API_CALL);
    }

    public static TcpClientTransportImpl of(ApiCallProcessor apiCallProcessor, String host, int port,
                                            TransportMode transportMode) {
        return new TcpClientTransportImpl(apiCallProcessor, host, port, transportMode);
    }

    public static TcpClientTransportImpl of(String host, int port, TransportMode transportMode) {
        return new TcpClientTransportImpl(host, port, transportMode);
    }

    public static TcpClientTransportImpl of(String host, int port) {
        return new TcpClientTransportImpl(host, port);
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(getApiEngineContext().getActorSystem()).outgoingConnection(host, port);

        Flow<ByteString, ArgsWrapper, NotUsed> repl =
                Flow.of(ByteString.class)
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                        .map(ByteString::utf8String)
                        .map(apiSerializer::messageFromString)
                        .log(logTitle(INCOMING_MESSAGE))
                        .filter(message -> message.getMessageType() == Message.MessageType.REQUEST)
                        .map(message -> {
                            ArgsWrapper argsWrapper = apiSerializer.parameterFromBase64String(
                                    message.getBase64Json());
                            if (transportMode == TransportMode.API_CALL) {
                                apiCallProcessor.response(argsWrapper);
                            } else if (transportMode == TransportMode.RESULT_AS_SOURCE) {
                                argsWrapperSource.submit(argsWrapper);
                            }
                            return argsWrapper;
                        });

        return Flow.of(ArgsWrapper.class)
                .map(this::next)
                .map(apiSerializer::messageFromArgs)
                .map(apiSerializer::messageToString)
                .log(logTitle(OUTGOING_MESSAGE))
                .map(ByteString::fromString)
                .via(connection)
                .via(repl)
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    @SuppressWarnings("checkstyle:hiddenField")
    @Override
    public TcpClientTransportImpl withApiCallProcessor(ApiCallProcessor apiCallProcessor) {
        this.apiCallProcessor = apiCallProcessor;
        return this;
    }

    @Override
    public Flow<ArgsWrapper, ArgsWrapper, NotUsed> getNewSource() {
        return Flow.of(ArgsWrapper.class)
                .merge(argsWrapperSource.getApiSource())
                .log(logTitle("submitted message"));
    }

    @Override
    public boolean enabled() {
        return transportMode == TransportMode.RESULT_AS_SOURCE;
    }
}
