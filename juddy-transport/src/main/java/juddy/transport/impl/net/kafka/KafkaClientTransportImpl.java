package juddy.transport.impl.net.kafka;

import akka.NotUsed;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.*;
import juddy.transport.impl.error.KafkaTransportException;
import juddy.transport.impl.kafka.consumer.ConsumerFactory;
import juddy.transport.impl.kafka.consumer.ConsumerProperties;
import juddy.transport.impl.kafka.consumer.ConsumerSource;
import juddy.transport.impl.kafka.producer.PartitionGenerator;
import juddy.transport.impl.kafka.producer.PartitionGenerators;
import juddy.transport.impl.kafka.producer.ProducerFactory;
import juddy.transport.impl.kafka.serialize.MessageJsonDeserializer;
import juddy.transport.impl.kafka.serialize.MessageJsonSerializer;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Map;

import static juddy.transport.impl.common.MessageConstants.INCOMING_MESSAGE;
import static juddy.transport.impl.common.MessageConstants.OUTGOING_MESSAGE;
import static juddy.transport.impl.net.kafka.KafkaConstants.TRANSPORT_REQUEST_TOPIC_SUFFIX;
import static juddy.transport.impl.net.kafka.KafkaConstants.TRANSPORT_RESPONSE_TOPIC_SUFFIX;

public class KafkaClientTransportImpl extends StageBaseWithCallProcessor implements ApiTransport, NewSource {

    private final Map<String, String> producerProperties;
    private final Map<String, String> consumerProperties;
    private final PartitionGenerator partitionGenerator;
    private final String requestTopicName;
    private final String responseTopicName;
    @Getter
    private final TransportMode transportMode;

    @Autowired
    private ApiSerializer apiSerializer;

    private ApiCallProcessor apiCallProcessor;
    @Autowired
    private ConsumerFactory consumerFactory;
    @Autowired
    private ProducerFactory producerFactory;

    protected KafkaClientTransportImpl(@NonNull Map<String, String> producerProperties,
                                       @NonNull Map<String, String> consumerProperties,
                                       @NonNull String transportId,
                                       @NonNull TransportMode transportMode,
                                       @NonNull PartitionGenerator partitionGenerator) {
        this.producerProperties = producerProperties;
        this.consumerProperties = consumerProperties;
        this.requestTopicName = transportId.concat(TRANSPORT_REQUEST_TOPIC_SUFFIX);
        this.responseTopicName = transportId.concat(TRANSPORT_RESPONSE_TOPIC_SUFFIX);
        if (transportMode != TransportMode.API_CALL && transportMode != TransportMode.RESULT) {
            throw new KafkaTransportException(String.format("Invalid TransportMode %s", transportMode));
        }
        this.transportMode = transportMode;
        this.partitionGenerator = partitionGenerator;
    }

    public static KafkaClientTransportImpl of(Map<String, String> producerProperties,
                                              Map<String, String> consumerProperties,
                                              String transportId,
                                              TransportMode transportMode,
                                              PartitionGenerator partitionGenerator) {
        return new KafkaClientTransportImpl(producerProperties, consumerProperties,
                transportId, transportMode, partitionGenerator);
    }

    public static KafkaClientTransportImpl of(Map<String, String> producerProperties,
                                              Map<String, String> consumerProperties,
                                              String transportId,
                                              TransportMode transportMode) {
        return new KafkaClientTransportImpl(producerProperties, consumerProperties,
                transportId, transportMode, PartitionGenerators.singlePartitionGenerator());
    }

    @Override
    public Flow<ArgsWrapper, ArgsWrapper, NotUsed> getNewSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @SuppressWarnings("checkstyle:hiddenField")
    @Override
    public KafkaClientTransportImpl withApiCallProcessor(ApiCallProcessor apiCallProcessor) {
        this.apiCallProcessor = apiCallProcessor;
        return this;
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {

        ProducerSettings<String, Message> producerSettings = producerFactory
                .initProducerProperties(producerProperties, new MessageJsonSerializer());

        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        ConsumerProperties<Message> consumerSettings =
                consumerFactory.initConsumerProperties(consumerProperties,
                        new MessageJsonDeserializer());

        ConsumerSource consumerSource = consumerFactory.createPlainConsumerSource(
                consumerSettings.getConsumerSettings(),
                Collections.singleton(responseTopicName));

        return Flow.of(ArgsWrapper.class)
                .map(this::next)
                .map(apiSerializer::messageFromArgs)
                .log(logTitle(OUTGOING_MESSAGE))
                .via(producerFactory.createProducerFlow(producerSettings, this::toProducerMessage))
                .filter(record -> false)
                .map(nodata -> ArgsWrapper.empty(String.class))
                .via(consumerSource.source())
                .map(argsWrapper -> {
                    if (transportMode == TransportMode.API_CALL) {
                        apiCallProcessor.response(argsWrapper);
                    }
                    return argsWrapper;
                })
                .log(logTitle(INCOMING_MESSAGE));
    }

    private ProducerMessage.Envelope<String, Message, NotUsed> toProducerMessage(Message message) {

        ProducerMessage.Envelope<String, Message, NotUsed> producerMessage =
                ProducerMessage.single(new ProducerRecord<>(requestTopicName,
                        partitionGenerator.partition(requestTopicName, message.getCorrelationId(), message),
                        message.getCorrelationId(), message));
        return producerMessage;
    }
}
