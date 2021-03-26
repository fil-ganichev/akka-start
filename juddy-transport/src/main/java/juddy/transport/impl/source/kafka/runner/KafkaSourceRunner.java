package juddy.transport.impl.source.kafka.runner;

import akka.NotUsed;
import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerMessage;
import akka.kafka.ConsumerSettings;
import akka.stream.javadsl.Flow;

import java.util.Set;
import java.util.concurrent.CompletionStage;

public interface KafkaSourceRunner {

    void run(Flow<ConsumerMessage.CommittableMessage<String, String>,
            ConsumerMessage.CommittableOffset, NotUsed> business,
             ConsumerSettings<String, String> consumerSettings,
             CommitterSettings committerSettings,
             Set<String> topics);

    <T> CompletionStage<T> shutDown();
}
