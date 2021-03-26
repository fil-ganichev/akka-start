package juddy.transport.impl.source.kafka;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.impl.common.RunnableStage;
import juddy.transport.impl.error.KafkaException;
import juddy.transport.impl.source.ApiSourceImpl;
import juddy.transport.impl.source.kafka.runner.KafkaSourceRunner;
import juddy.transport.impl.source.kafka.runner.RecoverableKafkaSourceRunner;
import juddy.transport.impl.source.kafka.runner.SimpleKafkaSourceRunner;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class KafkaSource extends ApiSourceImpl<String> implements RunnableStage {

    private final Map<String, String> consumerProperties;
    private final int batchSize;
    private final Set<String> topics;
    private final RecoverableKafkaSourceRunner.RecoverOptions recoverOptions;

    private KafkaSourceRunner kafkaSourceRunner;

    public KafkaSource(Map<String, String> consumerProperties, Set<String> topics, int batchSize,
                       RecoverableKafkaSourceRunner.RecoverOptions recoverOptions) {
        this.consumerProperties = consumerProperties;
        this.topics = topics;
        this.batchSize = batchSize;
        this.recoverOptions = recoverOptions;
    }

    public KafkaSource(Map<String, String> consumerProperties, Set<String> topics, int batchSize) {
        this(consumerProperties, topics, batchSize, null);
    }

    public KafkaSource(Map<String, String> consumerProperties, Set<String> topics,
                       RecoverableKafkaSourceRunner.RecoverOptions recoverOptions) {
        this(consumerProperties, topics, 0, recoverOptions);
    }

    public KafkaSource(Map<String, String> consumerProperties, Set<String> topics) {
        this(consumerProperties, topics, 0, null);
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class);
    }

    @Override
    protected Source<String, NotUsed> createSource() {
        throw new UnsupportedOperationException("createSource() not supported in KafkaSource");
    }

    @Override
    protected Source<String, NotUsed> getSource() {
        throw new UnsupportedOperationException("getSource() not supported in KafkaSource");
    }

    @Override
    public void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor) {
        Config consumerConfig = getApiEngineContext()
                .getActorSystem()
                .settings()
                .config()
                .getConfig("akka.kafka.consumer");

        ConsumerSettings<String, String> consumerSettings = ConsumerSettings.create(consumerConfig,
                new StringDeserializer(),
                new StringDeserializer())
                .withProperties(consumerProperties);

        Config commiterConfig = getApiEngineContext()
                .getActorSystem()
                .settings()
                .config()
                .getConfig("akka.kafka.committer");

        CommitterSettings committerSettings = CommitterSettings.create(commiterConfig);
        if (batchSize > 0) {
            committerSettings.withMaxBatch(batchSize);
        }

        Flow<ConsumerMessage.CommittableMessage<String, String>, ConsumerMessage.CommittableOffset, NotUsed> business =
                Flow.<ConsumerMessage.CommittableMessage<String, String>>create()
                        .log(logTitle("value from source"))
                        .map(this::argsWrapperFrom)
                        .map(this::next)
                        .via(graphProcessor)
                        .map(this::checkError)
                        .mapError(new PFBuilder<Throwable, Throwable>()
                                .match(Exception.class, this::onError)
                                .build())
                        .map(this::committableOffsetFrom);

        if (recoverOptions != null) {
            kafkaSourceRunner = new RecoverableKafkaSourceRunner(recoverOptions,
                    getApiEngineContext());
        } else {
            kafkaSourceRunner = new SimpleKafkaSourceRunner(getApiEngineContext());
        }
        kafkaSourceRunner.run(business,
                consumerSettings,
                committerSettings,
                topics);
    }

    public <T> CompletionStage<T> shutDown() {
        if (kafkaSourceRunner == null) {
            throw new KafkaException("Kafka source was not started. May be ApiEngine.run() was not called?");
        }
        return kafkaSourceRunner.shutDown();
    }

    private ArgsWrapper argsWrapperFrom(ConsumerMessage.CommittableMessage<String, String> committableMessage) {
        return ArgsWrapper.of(committableMessage.record().value())
                .withAdditional(committableMessage);
    }

    private ConsumerMessage.CommittableOffset committableOffsetFrom(ArgsWrapper argsWrapper) {
        return ((ConsumerMessage.CommittableMessage) argsWrapper.getAdditional()).committableOffset();
    }
}
