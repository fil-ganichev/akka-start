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
import juddy.transport.impl.context.ApiEngineContext;

import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SimpleKafkaSourceRunner implements KafkaSourceRunner {

    private final ApiEngineContext apiEngineContext;

    private Consumer.DrainingControl<Done> sourceControl;
    private final Executor shutDownThreadPool = Executors.newCachedThreadPool();

    public SimpleKafkaSourceRunner(ApiEngineContext apiEngineContext) {
        this.apiEngineContext = apiEngineContext;
    }

    @Override
    public void run(Flow<ConsumerMessage.CommittableMessage<String, String>,
            ConsumerMessage.CommittableOffset, NotUsed> business,
                    ConsumerSettings<String, String> consumerSettings,
                    CommitterSettings committerSettings,
                    Set<String> topics) {

        sourceControl = Consumer.commitWithMetadataSource(
                consumerSettings,
                Subscriptions.topics(topics),
                record -> Long.toString(record.timestamp()))
                .via(business)
                .toMat(Committer.sink(committerSettings), Consumer::createDrainingControl)
                .run(apiEngineContext.getActorSystem());
    }

    @Override
    public CompletionStage<Done> shutDown() {
        return sourceControl.drainAndShutdown(shutDownThreadPool);
    }
}
