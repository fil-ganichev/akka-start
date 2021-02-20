package juddy.transport.impl.client.simple;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import juddy.transport.api.TestApi;
import juddy.transport.api.args.ApiCallArguments;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.dto.ArrayApiCallArguments;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.client.ApiClientImpl;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.net.TcpClientTransportImpl;
import juddy.transport.impl.net.TcpServerTransportImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:hiddenField"})
@Configuration
@Import(StartConfiguration.class)
@SpringJUnitConfig(ApiClientSimpleTest.class)
class ApiClientSimpleTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String source = "Москва, Минск, Киев, Таллин, Рига, Кишинев";
    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;
    @Autowired
    private ApiClientImpl apiClient;
    @Autowired
    private TcpClientTransportImpl tcpClientTransport;
    @Autowired
    private TcpServerTransportImpl tcpServerTransport;

    @BeforeEach
    void setUp() throws Exception {
        runTcpServer(tcpServerTransport, argsWrapper -> {
            ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
            String arg = (String) ((ArrayApiCallArguments) apiCallArguments).getValues()[0];
            ArgsWrapper result = ArgsWrapper.of(new ObjectApiCallArguments(Arrays.asList(arg.split(", "))))
                    .withCorrelationId(argsWrapper.getCorrelationId());
            result.setCallInfo(null);
            return result;
        });
    }

    @Test
    void when_apiClientCall_thenOk() {
        List<Object> results = new ArrayList<>();
        AtomicLong steps = new AtomicLong();
        Source.empty(ArgsWrapper.class)
                .via(apiClient.getStageConnector())
                .runWith(Sink.foreach(arg -> {
                    results.add(arg.getApiCallArguments().getResult());
                    steps.incrementAndGet();
                }), getActorSystem());
        TestApi testApi = apiClient.getProxy(TestApi.class);
        testApi.split(source);
        await().atMost(1, TimeUnit.SECONDS).until(() -> steps.get() >= 1);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0)).isEqualTo(new Object[]{source});
    }

    @Test
    void when_apiCallApiClientAndTcpClientTransport_thenOk() throws Exception {
        Source.empty(ArgsWrapper.class)
                .via(apiClient.getStageConnector())
                .via(tcpClientTransport.getStageConnector())
                .run(getActorSystem());
        TestApi testApi = apiClient.getProxy(TestApi.class);
        List<String> results = testApi.split(source).get(500, TimeUnit.MILLISECONDS);
        assertThat(results).containsExactly(source.split(", "));
    }

    @Bean
    public ApiClientImpl apiClient() {
        return ApiClientImpl.of(Arrays.asList(TestApi.class));
    }

    @Bean
    public TcpClientTransportImpl tcpClientTransport(ApiClientImpl apiClient) throws Exception {
        return TcpClientTransportImpl.of(apiClient.getApiCallProcessor(), "127.0.0.1", 8889);
    }

    @Bean
    public TcpServerTransportImpl tcpServerTransport() {
        return TcpServerTransportImpl.of("127.0.0.1", 8889);
    }

    private TcpServerTransportImpl runTcpServer(TcpServerTransportImpl tcpServerTransport,
                                                UnaryOperator<ArgsWrapper> processor) throws Exception {
        tcpServerTransport.run(Flow.of(ArgsWrapper.class).map(processor::apply));
        return tcpServerTransport;
    }

    private ActorSystem getActorSystem() {
        return apiEngineContextProvider.getApiEngineContext().getActorSystem();
    }
}
