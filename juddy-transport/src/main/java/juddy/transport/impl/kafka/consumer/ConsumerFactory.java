package juddy.transport.impl.kafka.consumer;

import akka.Done;
import akka.NotUsed;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.context.ApiEngineContext;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.error.ApiCallDeserializationException;
import juddy.transport.impl.error.ApiEngineException;
import juddy.transport.impl.error.KafkaException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static juddy.transport.impl.common.MessageConstants.API_ENGINE_CONTEXT_NOT_AVAILABLE;

public class ConsumerFactory {

    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;
    @Autowired
    private ApiSerializer apiSerializer;

    public <T> ConsumerProperties<T> initConsumerProperties(Map<String, String> consumerProperties,
                                                            Deserializer<T> deserializer) {
        return initConsumerProperties(consumerProperties, 0, deserializer);

    }

    public <T> ConsumerProperties<T> initConsumerProperties(Map<String, String> consumerProperties,
                                                            int batchSize,
                                                            Deserializer<T> deserializer) {
        Config consumerConfig = getApiEngineContext()
                .getActorSystem()
                .settings()
                .config()
                .getConfig("akka.kafka.consumer");

        ConsumerSettings<String, T> consumerSettings = ConsumerSettings.create(consumerConfig,
                new StringDeserializer(),
                deserializer)
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

        return new ConsumerProperties<>(consumerSettings, committerSettings);
    }

    public <T> Source<ConsumerMessage.CommittableMessage<String, T>, Consumer.Control> createCommitableConsumer(
            ConsumerSettings<String, T> consumerSettings, Set<String> topics) {
        return Consumer.commitWithMetadataSource(
                consumerSettings,
                Subscriptions.topics(topics),
                record -> Long.toString(record.timestamp()));
    }

    public <T> Source<ConsumerRecord<String, T>, Consumer.Control> createPlainConsumer(
            ConsumerSettings<String, T> consumerSettings, Set<String> topics) {
        return Consumer.plainSource(consumerSettings,
                Subscriptions.topics(topics));
    }

    public <T, R> ConsumerSource createPlainConsumerSource(ConsumerSettings<String, T> consumerSettings,
                                                           Set<String> topics,
                                                           Function<ConsumerRecord<String, T>, R> dataConverter) {

        AtomicReference<Consumer.Control> sourceControl = new AtomicReference<>();

        Flow<R, R, NotUsed> source = Flow.<R>create()
                .merge(createPlainConsumer(consumerSettings, topics)
                        .mapMaterializedValue(
                                c -> {
                                    sourceControl.set(c);
                                    return c;
                                })
                        .map(dataConverter::apply));

        return new DefaultConsumerSource(source, sourceControl);
    }

    public ConsumerSource createPlainConsumerSource(ConsumerSettings<String, Message> consumerSettings,
                                                    Set<String> topics) {
        return createPlainConsumerSource(consumerSettings, topics, this::argsWrapperFrom);
    }

    public <T, R> ConsumerSource createCommitableConsumerSource(
            ConsumerSettings<String, T> consumerSettings,
            Set<String> topics,
            Function<ConsumerMessage.CommittableMessage<String, T>, R> dataConverter) {

        AtomicReference<Consumer.Control> sourceControl = new AtomicReference<>();

        Flow<R, R, NotUsed> source = Flow.<R>create()
                .merge(createCommitableConsumer(consumerSettings, topics)
                        .mapMaterializedValue(
                                c -> {
                                    sourceControl.set(c);
                                    return c;
                                })
                        .map(dataConverter::apply));

        return new DefaultConsumerSource(source, sourceControl);
    }

    public ConsumerSource createCommitableConsumerSource(ConsumerSettings<String, Message> consumerSettings,
                                                         Set<String> topics) {
        return createCommitableConsumerSource(consumerSettings, topics, this::argsWrapperFrom);
    }

    private ArgsWrapper argsWrapperFrom(
            ConsumerMessage.CommittableMessage<String, Message> committableMessage) {
        try {
            return apiSerializer.parameterFromBase64String(committableMessage.record().value().getBase64Json())
                    .withAdditional(committableMessage);
        } catch (NoSuchMethodException e) {
            throw new ApiCallDeserializationException(e);
        }
    }

    private <T> ArgsWrapper argsWrapperFrom(ConsumerRecord<String, Message> consumerRecord) {
        try {
            return apiSerializer.parameterFromBase64String(consumerRecord.value().getBase64Json());
        } catch (NoSuchMethodException e) {
            throw new ApiCallDeserializationException(e);
        }
    }

    private ApiEngineContext getApiEngineContext() {
        if (apiEngineContextProvider == null) {
            throw new ApiEngineException(API_ENGINE_CONTEXT_NOT_AVAILABLE);
        }
        return apiEngineContextProvider.getApiEngineContext();
    }

    class DefaultConsumerSource<T> implements ConsumerSource {

        private final Flow<T, T, NotUsed> source;
        private final AtomicReference<Consumer.Control> sourceControl;

        private final Executor shutDownThreadPool = Executors.newCachedThreadPool();

        DefaultConsumerSource(Flow<T, T, NotUsed> source,
                              AtomicReference<Consumer.Control> sourceControl) {
            this.source = source;
            this.sourceControl = sourceControl;
        }

        @Override
        public Flow<T, T, NotUsed> source() {
            return source;
        }

        @Override
        public CompletionStage<Done> shutDown() {
            Consumer.Control control = sourceControl.get();
            if (control != null) {
                if (control instanceof Consumer.DrainingControl) {
                    return ((Consumer.DrainingControl) sourceControl.get()).drainAndShutdown(shutDownThreadPool);
                } else {
                    return sourceControl.get().shutdown();
                }
            }
            throw new KafkaException("Consumer is not running. Shutdown not available");
        }
    }
}
