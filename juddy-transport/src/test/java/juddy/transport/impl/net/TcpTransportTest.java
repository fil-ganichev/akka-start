package juddy.transport.impl.net;

import juddy.transport.api.TestApiFioEnricher;
import juddy.transport.api.TestApiPerson;
import juddy.transport.api.TestApiPersonServer;
import juddy.transport.config.net.TcpTransportTestConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:throwsCount"})
@SpringJUnitConfig(TcpTransportTestConfiguration.class)
class TcpTransportTest {

    @Autowired
    private ApiEngineImpl apiEngineClient;
    @Autowired
    private ApiEngineImpl apiEngineClientResultAsSource;
    @Autowired
    private TestApiFioEnricher testApiFioEnricher;
    @Autowired
    private JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper;

    private TestApiPersonServer testApiPersonServer = new TestApiPersonServer();

    @Test
    void when_readSourceAndCallRemoteApiServerViaTcp_then_sinkOk() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineClient.findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineClient.run();
        await().atMost(1, TimeUnit.SECONDS).until(
                testApiSinkServer.processed(jsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(fioArray(jsonFileSourceHelper.getValues()));
    }

    @Test
    void when_readSourceAndCallRemoteApiServerViaTcpAndGetResultAsNewSource_then_sinkOk() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineClientResultAsSource
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineClientResultAsSource.run();
        await().atMost(1, TimeUnit.SECONDS).until(
                testApiSinkServer.processed(jsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(fioArray(jsonFileSourceHelper.getValues()));
    }

    @Test
    void when_readSourceAndCallRemoteApiServerViaTcpAndException_then_processException() {
        try {
            doThrow(new RuntimeException("Error while processing string"))
                    .when(testApiFioEnricher).enrichFio(argThat(s -> s.startsWith("Шевцов")));
            TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineClient
                    .findServerBean(TestApiSink.class);
            testApiSinkServer.reset();
            apiEngineClient.run();
            await().atMost(1, TimeUnit.SECONDS).until(
                    testApiSinkServer.processed(1));
            testApiSinkServer.check(fioArray(jsonFileSourceHelper.getValues().subList(0, 1)));
        } finally {
            Mockito.reset(testApiFioEnricher);
        }
    }

    private String[] fioArray(List<TestApiPerson.Person> personList) {
        return personList.stream()
                .map(person -> testApiFioEnricher.enrichFio(testApiPersonServer.getFio(person)))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
