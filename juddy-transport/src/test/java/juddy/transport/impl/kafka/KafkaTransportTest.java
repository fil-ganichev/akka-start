package juddy.transport.impl.kafka;

import juddy.transport.api.TestApiFioEnricher;
import juddy.transport.api.TestApiPerson;
import juddy.transport.api.TestApiPersonServer;
import juddy.transport.config.kafka.KafkaTransportTestConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static juddy.transport.common.Constants.KAFKA_CONSUMER_TIMEOUT_MS;
import static org.awaitility.Awaitility.await;

@EnabledIf(expression = "#{environment['spring.profiles.active'] == 'kafka'}")
@SuppressWarnings("checkstyle:methodName")
@SpringJUnitConfig(KafkaTransportTestConfiguration.class)
class KafkaTransportTest {

    @Autowired
    private ApiEngineImpl apiEngineFromKafkaSource;
    @Autowired
    private JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper;
    @Autowired
    private TestApiFioEnricher testApiFioEnricher;

    private TestApiPersonServer testApiPersonServer = new TestApiPersonServer();

    @Test
    void when_readSourceAndCallRemoteApiServerViaKafka_then_sinkOk() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromKafkaSource
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromKafkaSource.run();
        await().atMost(KAFKA_CONSUMER_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(jsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(fioArray(jsonFileSourceHelper.getValues()));
    }

    private String[] fioArray(List<TestApiPerson.Person> personList) {
        return personList.stream()
                .map(person -> testApiFioEnricher.enrichFio(testApiPersonServer.getFio(person)))
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
