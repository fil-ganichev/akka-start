package juddy.transport.impl.net.kafka;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.pf.PFBuilder;
import akka.kafka.ConsumerMessage;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.net.ApiTransport;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.common.RunnableStage;
import juddy.transport.impl.common.StageBase;
import juddy.transport.impl.kafka.consumer.ConsumerFactory;
import juddy.transport.impl.kafka.consumer.ConsumerProperties;
import juddy.transport.impl.kafka.consumer.ConsumerSource;
import juddy.transport.impl.kafka.producer.PartitionGenerator;
import juddy.transport.impl.kafka.producer.PartitionGenerators;
import juddy.transport.impl.kafka.producer.ProducerFactory;
import juddy.transport.impl.kafka.serialize.MessageJsonDeserializer;
import juddy.transport.impl.kafka.serialize.MessageJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.javatuples.Triplet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import java.util.Collections;
import java.util.Map;

import static juddy.transport.impl.common.MessageConstants.INCOMING_MESSAGE;
import static juddy.transport.impl.net.kafka.KafkaConstants.TRANSPORT_REQUEST_TOPIC_SUFFIX;
import static juddy.transport.impl.net.kafka.KafkaConstants.TRANSPORT_RESPONSE_TOPIC_SUFFIX;

public class KafkaServerTransportImpl extends StageBase implements ApiTransport, RunnableStage {

    private final Map<String, String> producerProperties;
    private final Map<String, String> consumerProperties;
    private final PartitionGenerator partitionGenerator;
    private final String requestTopicName;
    private final String responseTopicName;

    @Autowired
    private ApiSerializer apiSerializer;
    @Autowired
    private ConsumerFactory consumerFactory;
    @Autowired
    private ProducerFactory producerFactory;

    protected KafkaServerTransportImpl(@NonNull Map<String, String> producerProperties,
                                       @NonNull Map<String, String> consumerProperties,
                                       @NonNull String transportId,
                                       @NonNull PartitionGenerator partitionGenerator) {
        this.producerProperties = producerProperties;
        this.consumerProperties = consumerProperties;
        this.requestTopicName = transportId.concat(TRANSPORT_REQUEST_TOPIC_SUFFIX);
        this.responseTopicName = transportId.concat(TRANSPORT_RESPONSE_TOPIC_SUFFIX);
        this.partitionGenerator = partitionGenerator;
    }

    public static KafkaServerTransportImpl of(Map<String, String> producerProperties,
                                              Map<String, String> consumerProperties,
                                              String transportId,
                                              PartitionGenerator partitionGenerator) {
        return new KafkaServerTransportImpl(producerProperties,
                consumerProperties,
                transportId,
                partitionGenerator);
    }

    public static KafkaServerTransportImpl of(Map<String, String> producerProperties,
                                              Map<String, String> consumerProperties,
                                              String transportId) {
        return new KafkaServerTransportImpl(producerProperties,
                consumerProperties,
                transportId,
                PartitionGenerators.singlePartitionGenerator());
    }

    @Override
    public void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {

        ActorSystem actorSystem = getApiEngineContext().getActorSystem();

        ProducerSettings<String, Message> producerSettings = producerFactory
                .initProducerProperties(producerProperties, new MessageJsonSerializer());

        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        ConsumerProperties<Message> consumerSettings =
                consumerFactory.initConsumerProperties(consumerProperties,
                        new MessageJsonDeserializer());

        ConsumerSource consumerSource = consumerFactory.createTransactionalConsumerSource(
                consumerSettings.getConsumerSettings(),
                Collections.singleton(requestTopicName));

        Flow<ConsumerMessage.TransactionalMessage<String, Message>,
                Triplet<String, Message, ConsumerMessage.PartitionOffset>, NotUsed> business =
                Flow.<ConsumerMessage.TransactionalMessage<String, Message>>create()
                        .log(logTitle(INCOMING_MESSAGE))
                        .map(this::argsWrapperFrom)
                        .map(this::next)
                        .via(graphProcessor)
                        .map(this::checkError)
                        .mapError(new PFBuilder<Throwable, Throwable>()
                                .match(Exception.class, this::onError)
                                .build())
                        .map(this::transactionalMessageFrom);

        Source.<ConsumerMessage.TransactionalMessage<String, Message>>empty()
                .via(consumerSource.source())
                .via(business)
                .map(triplet ->
                        ProducerMessage.single(
                                new ProducerRecord<>(responseTopicName,
                                        partitionGenerator.partition(responseTopicName,
                                                triplet.getValue0(),
                                                triplet.getValue1()),
                                        triplet.getValue0(),
                                        triplet.getValue1()),
                                triplet.getValue2()))
                .to(producerFactory.transactionalSink(producerSettings, responseTopicName))
                .run(actorSystem);
    }

    private ArgsWrapper argsWrapperFrom(ConsumerMessage.TransactionalMessage<String, Message> transactionalMessage)
            throws NoSuchMethodException {
        Message message = transactionalMessage.record().value();
        String base64Json = message.getBase64Json();
        ArgsWrapper argsWrapper = apiSerializer.parameterFromBase64String(base64Json);
        argsWrapper.withAdditional(Triplet.with(transactionalMessage.record().key(), null,
                transactionalMessage.partitionOffset()));
        return argsWrapper;
    }

    private Triplet<String, Message, ConsumerMessage.PartitionOffset> transactionalMessageFrom(
            ArgsWrapper argsWrapper) {
        Message message = apiSerializer.messageFromArgs(argsWrapper);
        Triplet<String, Message, ConsumerMessage.PartitionOffset> old =
                (Triplet<String, Message, ConsumerMessage.PartitionOffset>) argsWrapper.getAdditional();
        return Triplet.with(old.getValue0(), message, old.getValue2());
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class);
    }
}
