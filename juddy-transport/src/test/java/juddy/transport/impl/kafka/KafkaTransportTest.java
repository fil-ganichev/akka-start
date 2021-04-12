package juddy.transport.impl.kafka;

import juddy.transport.api.engine.ApiEngine;
import juddy.transport.config.kafka.KafkaTransportTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@EnabledIf(expression = "#{environment['spring.profiles.active'] == 'kafka'}")
@SuppressWarnings("checkstyle:methodName")
@SpringJUnitConfig(KafkaTransportTestConfiguration.class)
class KafkaTransportTest {

    @Autowired
    private ApiEngine apiEngineFromKafkaSource;

    @Test
    void checkApiClient() {
        apiEngineFromKafkaSource.run();
    }
}
