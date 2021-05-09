package juddy.transport.config.kafka;

import juddy.transport.api.TestApiFioEnricher;
import juddy.transport.api.TestApiPerson;
import juddy.transport.api.TestApiPersonServer;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.common.TransportMode;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.kafka.config.KafkaConfiguration;
import juddy.transport.impl.kafka.serialize.MessageJsonSerializer;
import juddy.transport.impl.server.ApiServerImpl;
import juddy.transport.impl.source.file.JsonFileSource;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.impl.utils.yaml.YamlPropertySourceFactory;
import juddy.transport.test.sink.TestApiMock;
import juddy.transport.test.sink.TestApiMockFactory;
import juddy.transport.test.sink.TestApiSink;
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
import java.util.Collections;
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
    public ApiEngineImpl apiEngineFromKafkaSource(ApiEngineFactory apiEngineFactory, JsonFileSource jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPerson.class, TestApiPersonServer.class)))
                .connect(apiEngineFactory.kafkaClientTransport(clientProducerProperties(),
                        clientConsumerProperties(),
                        "TEST_KAFKA_TRANSPORT",
                        TransportMode.RESULT))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean(initMethod = "run")
    public ApiEngineImpl apiEngineServer(ApiEngineFactory apiEngineFactory,
                                         ApiServerImpl testApiFioEnricherServer) {
        return ApiEngineImpl.of(apiEngineFactory.kafkaServerTransport(serverProducerProperties(),
                serverConsumerProperties(),
                "TEST_KAFKA_TRANSPORT"))
                .connect(testApiFioEnricherServer)
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

    @Bean
    public TestApiFioEnricher testApiFioEnricher() {
        return spy(new TestApiFioEnricher() {
        });
    }

    @Bean
    public ApiServerImpl testApiFioEnricherServer(TestApiFioEnricher testApiFioEnricher) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiFioEnricher.class, testApiFioEnricher));
    }

    private Map<String, String> defaultConsumerProperties() {
        Map<String, String> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        return consumerProperties;
    }

    private Map<String, String> defaultProducerProperties() {
        Map<String, String> producerProperties = new HashMap<>();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                MessageJsonSerializer.class.getName());
        return producerProperties;
    }

    private Map<String, String> clientConsumerProperties() {
        Map<String, String> consumerProperties = new HashMap<>(defaultConsumerProperties());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "testClient");
        return consumerProperties;
    }

    private Map<String, String> clientProducerProperties() {
        Map<String, String> producerProperties = new HashMap<>(defaultProducerProperties());
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "testClientId");
        return producerProperties;
    }

    private Map<String, String> serverConsumerProperties() {
        Map<String, String> consumerProperties = new HashMap<>(defaultConsumerProperties());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "serverClient");
        return consumerProperties;
    }

    private Map<String, String> serverProducerProperties() {
        Map<String, String> producerProperties = new HashMap<>(defaultProducerProperties());
        producerProperties.put(ProducerConfig.CLIENT_ID_CONFIG, "serverClientId");
        return producerProperties;
    }
}
