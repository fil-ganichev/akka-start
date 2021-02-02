package org.lokrusta.prototypes.connect.impl.client.simple;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.lokrusta.prototypes.connect.api.ApiCallArguments;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.TestApi;
import org.lokrusta.prototypes.connect.api.dto.ArrayApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.impl.ApiClientImpl;
import org.lokrusta.prototypes.connect.impl.ArgsWrapperImpl;
import org.lokrusta.prototypes.connect.impl.TcpClientTransportImpl;
import org.lokrusta.prototypes.connect.impl.TestBase;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContext;
import org.lokrusta.prototypes.connect.impl.context.ApiEngineContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApiClientSimpleTest extends TestBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ApiEngineContext apiEngineContext = ApiEngineContextProvider.getApiEngineContext();
    private final String source = "Москва, Минск, Киев, Таллин, Рига, Кишинев";

    @BeforeAll
    void setUp() throws Exception {
        runTcpServer("127.0.0.1", 8889, argsWrapper -> {
            ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
            String arg = (String) ((ArrayApiCallArguments) apiCallArguments).getValues()[0];
            ArgsWrapperImpl result = ArgsWrapperImpl.of(new ObjectApiCallArguments(Arrays.asList(arg.split(", "))))
                    .withCorrelationId(argsWrapper.getCorrelationId());
            result.setCallInfo(null);
            return result;
        });
    }

    @Test
    void when_apiClientCall_thenOk() throws Exception {
        List<Object> results = new ArrayList<>();
        ApiClientImpl apiClientImpl = apiClient(Arrays.asList(TestApi.class));
        apiClientImpl.afterPropertiesSet();
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> apiClientConnector = getConnector(apiClientImpl);
        Source.empty(ArgsWrapper.class)
                .via(apiClientConnector)
                .runWith(Sink.foreach(arg -> results.add(arg.getApiCallArguments().getResult())), apiEngineContext.getActorSystem());
        TestApi testApi = apiClientImpl.getProxy(TestApi.class);
        testApi.split(source);
        Thread.sleep(1000);
        assertThat(results.size()).isEqualTo(1);
        assertThat(results.get(0)).isEqualTo(new Object[]{source});
    }

    @Test
    void when_apiCallApiClientAndTcpClientTransport_thenOk() throws Exception {
        ApiClientImpl apiClientImpl = apiClient(Arrays.asList(TestApi.class));
        apiClientImpl.afterPropertiesSet();
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> apiClientConnector = getConnector(apiClientImpl);
        TcpClientTransportImpl tcpClientTransportImpl = tcpClientTransport(getApiCallProcessor(apiClientImpl), "127.0.0.1", 8889);
        tcpClientTransportImpl.afterPropertiesSet();
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> tcpClientTransportConnector = getConnector(tcpClientTransportImpl);
        Source.empty(ArgsWrapper.class)
                .via(apiClientConnector)
                .via(tcpClientTransportConnector)
                .run(apiEngineContext.getActorSystem());
        TestApi testApi = apiClientImpl.getProxy(TestApi.class);
        List<String> results = testApi.split(source).get(500, TimeUnit.MILLISECONDS);
        assertThat(results).containsExactly(source.split(", "));
    }
}
