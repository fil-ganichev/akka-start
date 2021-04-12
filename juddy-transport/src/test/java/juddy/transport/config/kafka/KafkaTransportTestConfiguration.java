package juddy.transport.config.kafka;

import juddy.transport.api.TestApiPerson;
import juddy.transport.api.TestApiPersonServer;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.common.TransportMode;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.kafka.config.KafkaConfiguration;
import juddy.transport.impl.kafka.serialize.MessageJsonSerializer;
import juddy.transport.impl.source.file.JsonFileSource;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.impl.utils.yaml.YamlPropertySourceFactory;
import juddy.transport.test.sink.TestApiMock;
import juddy.transport.test.sink.TestApiMockFactory;
import juddy.transport.test.sink.TestApiSinkServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.spy;

@Configuration
@Import({StartConfiguration.class, KafkaConfiguration.class, EmbeddedKafkaConfiguration.class})
@PropertySource(value = "classpath:kafka-properties.yml", factory = YamlPropertySourceFactory.class)
public class KafkaTransportTestConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${kafka.servers}")
    private String kafkaServers;

    @Bean
    public ApiEngine apiEngineFromKafkaSource(ApiEngineFactory apiEngineFactory, JsonFileSource jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPerson.class, TestApiPersonServer.class)))
                .connect(apiEngineFactory.kafkaClientTransport(defaultProducerProperties(),
                        defaultConsumerProperties(),
                        "TEST_KAFKA_TRANSPORT",
                        TransportMode.RESULT))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public TestApiSinkServer testApiSink() {
        return spy(new TestApiSinkServer());
    }

    @Bean
    public TestApiMock testApiMock() {
        return TestApiMockFactory.mockServer();
    }

    @Bean
    public JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper(ApiSerializer apiSerializer)
            throws IOException, URISyntaxException {
        return new JsonFileSourceHelper<>("fileSource/person-source.json", TestApiPerson.Person.class, apiSerializer);
    }

    @Bean
    public JsonFileSource<TestApiPerson.Person> jsonFileSource(
            JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper) {
        return jsonFileSourceHelper.getJsonFileSource();
    }

    private Map<String, String> defaultConsumerProperties() {
        Map<String, String> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return consumerProperties;
    }

    private Map<String, String> defaultProducerProperties() {
        Map<String, String> producerProperties = new HashMap<>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "testClientId");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                MessageJsonSerializer.class.getName());
        return producerProperties;
    }
}
