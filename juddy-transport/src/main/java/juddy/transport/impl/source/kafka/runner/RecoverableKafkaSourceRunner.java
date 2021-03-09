package juddy.transport.impl.source.kafka.runner;

import akka.Done;
import akka.NotUsed;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Committer;
import akka.kafka.javadsl.Consumer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RestartSource;
import akka.stream.javadsl.Source;
import juddy.transport.impl.context.ApiEngineContext;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class RecoverableKafkaSourceRunner implements KafkaSourceRunner {

    private final RecoverOptions recoverOptions;
    private final ApiEngineContext apiEngineContext;
    private final AtomicReference<Consumer.Control> restartSourceControl = new AtomicReference<>();
    private CompletionStage<Done> restartSourceCompletion;

    public RecoverableKafkaSourceRunner(RecoverOptions recoverOptions,
                                        ApiEngineContext apiEngineContext) {
        this.recoverOptions = recoverOptions;
        this.apiEngineContext = apiEngineContext;
    }

    @Override
    public void run(Flow<ConsumerMessage.CommittableMessage<String, String>,
            ConsumerMessage.CommittableOffset, NotUsed> business,
                    ConsumerSettings<String, String> consumerSettings,
                    CommitterSettings committerSettings,
                    Set<String> topics) {

        Source<ConsumerMessage.CommittableOffset, Consumer.Control> source = Consumer.commitWithMetadataSource(
                consumerSettings,
                Subscriptions.topics(topics),
                record -> Long.toString(record.timestamp()))
                .mapMaterializedValue(
                        c -> {
                            restartSourceControl.set(c);
                            return c;
                        })
                .via(business);

        restartSourceCompletion = RestartSource.onFailuresWithBackoff(recoverOptions.getMinBackoff(),
                recoverOptions.getMaxBackoff(),
                recoverOptions.getRandomFactor(),
                recoverOptions.getMaxRestarts(),
                () -> source)
                .runWith(Committer.sink(committerSettings), apiEngineContext.getActorSystem());
    }

    @Override
    public void shutDown() {
        restartSourceControl.get().drainAndShutdown(restartSourceCompletion,
                apiEngineContext.getActorSystem().getDispatcher());
    }

    @Data
    @Builder
    public static class RecoverOptions {

        private Duration minBackoff;
        private Duration maxBackoff;
        private double randomFactor;
        private int maxRestarts;
    }
}
