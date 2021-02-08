package juddy.transport.impl.net;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.JsonFraming;
import akka.stream.javadsl.Tcp;
import akka.util.ByteString;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.impl.args.ArgsWrapperImpl;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.ApiCallProcessor;
import juddy.transport.impl.common.ApiSerialilizer;
import juddy.transport.impl.common.StageBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletionStage;

public class TcpClientTransportImpl extends StageBase implements ApiTransport {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String host;
    private final int port;
    private ApiCallProcessor apiCallProcessor;
    @Autowired
    private ApiSerialilizer apiSerialilizer;

    protected TcpClientTransportImpl(ApiCallProcessor apiCallProcessor, String host, int port) {
        this.apiCallProcessor = apiCallProcessor;
        this.host = host;
        this.port = port;
    }

    protected TcpClientTransportImpl(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static TcpClientTransportImpl of(ApiCallProcessor apiCallProcessor, String host, int port) {
        return new TcpClientTransportImpl(apiCallProcessor, host, port);
    }

    public static TcpClientTransportImpl of(String host, int port) {
        return new TcpClientTransportImpl(host, port);
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        Flow<ByteString, ByteString, CompletionStage<Tcp.OutgoingConnection>> connection =
                Tcp.get(getApiEngineContext().getActorSystem()).outgoingConnection(host, port);

        Flow<ByteString, ByteString, NotUsed> repl =
                Flow.of(ByteString.class)
                        .via(JsonFraming.objectScanner(Integer.MAX_VALUE))
                        .map(ByteString::utf8String)
                        .map(apiSerialilizer::messageFromString)
                        .log(logTitle("incoming message"))
                        .map(message -> {
                            if (message.getMessageType() == Message.MessageType.REQUEST) {
                                ArgsWrapper argsWrapper = apiSerialilizer.parameterFromBase64String(message.getBase64Json());
                                apiCallProcessor.response(argsWrapper);
                            }
                            return ByteString.emptyByteString();
                        });

        return Flow.of(ArgsWrapper.class)
                .map(this::next)
                .map(apiSerialilizer::messageFromArgs)
                .map(apiSerialilizer::messageToString)
                .log(logTitle("outgoing message"))
                .map(ByteString::fromString)
                .via(connection)
                .via(repl)
                .map(s -> (ArgsWrapper) (ArgsWrapperImpl.of((String) null)))
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    public TcpClientTransportImpl withApiCallProcessor(ApiCallProcessor apiCallProcessor) {
        this.apiCallProcessor = apiCallProcessor;
        return this;
    }
}
