package juddy.transport.config.kafka;

import juddy.transport.impl.utils.yaml.YamlPropertySourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

@Configuration
@PropertySource(value = "classpath:kafka-properties.yml", factory = YamlPropertySourceFactory.class)
public class KafkaBrokerConfiguration {

    @Bean(destroyMethod = "destroy")
    public EmbeddedKafkaBroker embeddedKafkaBroker(@Value("${kafka.listeners}") String listeners,
                                                   @Value("${kafka.port}") String port,
                                                   @Value("${kafka.log.dirs}") String logDirs) {
        return new EmbeddedKafkaBroker(1, true, 1)
                .brokerProperty("listeners", listeners)
                .brokerProperty("port", port)
                .brokerProperty("log.dirs", logDirs);
    }
}
