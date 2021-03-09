package juddy.transport.config.kafka;

import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.server.ApiServerImpl;
import juddy.transport.impl.source.kafka.KafkaSource;
import juddy.transport.impl.source.kafka.runner.RecoverableKafkaSourceRunner;
import juddy.transport.test.sink.TestApiMock;
import juddy.transport.test.sink.TestApiMockFactory;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.spy;

@Configuration
@Import({StartConfiguration.class, EmbeddedKafkaConfiguration.class})
public class KafkaSourceTestConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public KafkaSource kafkaSource() {
        return new KafkaSource(defaultConsumerProperties(),
                Collections.singleton("when_readKafkaSource_then_ok_topic"));
    }

    @Bean(initMethod = "run")
    public ApiEngine apiEngineFromKafkaSource(ApiEngineFactory apiEngineFactory, KafkaSource kafkaSource) {
        return ApiEngineImpl.of(kafkaSource).connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class,
                TestApiSinkServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public KafkaSource rollbackKafkaSource() {
        return new KafkaSource(defaultConsumerProperties(),
                Collections.singleton("when_processingException_then_rollback_topic"), 4);
    }

    @Bean
    public ApiEngine apiEngineFromRollbackKafkaSource(ApiServerImpl rollbackTestApiSinkServer,
                                                      KafkaSource rollbackKafkaSource) {
        return ApiEngineImpl.of(rollbackKafkaSource).connect(rollbackTestApiSinkServer)
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public KafkaSource autoCommitKafkaSource() {
        Map<String, String> consumerProperties = defaultConsumerProperties();
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return new KafkaSource(consumerProperties,
                Collections.singleton("when_processingWithAutoCommit_then_allItemsCommited_topic"), 1);
    }

    @Bean
    public ApiEngine apiEngineFromAutoCommitKafkaSource(ApiServerImpl autoCommitTestApiSinkServer,
                                                        KafkaSource autoCommitKafkaSource) {
        return ApiEngineImpl.of(autoCommitKafkaSource).connect(autoCommitTestApiSinkServer)
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public ApiEngine apiEngineFromRecoverableKafkaSource(ApiServerImpl recoverTestApiMockServer,
                                                         ApiServerImpl recoverTestApiSinkServer,
                                                         KafkaSource recoverableKafkaSource) {
        return ApiEngineImpl.of(recoverableKafkaSource)
                .connect(recoverTestApiMockServer)
                .connect(recoverTestApiSinkServer)
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public KafkaSource recoverableKafkaSource() {
        return new KafkaSource(defaultConsumerProperties(),
                Collections.singleton("when_recoverableKafkaSource_then_itRecover_topic"),
                1,
                RecoverableKafkaSourceRunner.RecoverOptions.builder()
                        .minBackoff(Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(1))
                        .randomFactor(0.1)
                        .maxRestarts(5)
                        .build());
    }

    @Bean
    public TestApiSinkServer testApiSink() {
        return spy(new TestApiSinkServer());
    }

    @Bean
    public ApiServerImpl rollbackTestApiSinkServer(TestApiSinkServer testApiSink) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiSink.class, testApiSink));
    }

    @Bean
    public ApiServerImpl autoCommitTestApiSinkServer(TestApiSinkServer testApiSink) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiSink.class, testApiSink));
    }

    @Bean
    public ApiServerImpl recoverTestApiSinkServer(TestApiSinkServer testApiSink) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiSink.class, testApiSink));
    }

    @Bean
    public TestApiMock testApiMock() {
        return TestApiMockFactory.mockServer();
    }

    @Bean
    public ApiServerImpl recoverTestApiMockServer(TestApiMock testApiMock) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiMock.class, testApiMock));
    }

    private Map<String, String> defaultConsumerProperties() {
        Map<String, String> consumerProperties = new HashMap<>();
        consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:3333");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return consumerProperties;
    }
}
