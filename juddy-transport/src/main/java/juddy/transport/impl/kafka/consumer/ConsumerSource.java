package juddy.transport.impl.kafka.consumer;

import akka.NotUsed;
import akka.stream.javadsl.Flow;

import java.util.concurrent.CompletionStage;

public interface ConsumerSource {

    <T> Flow<T, T, NotUsed> source();

    <T> CompletionStage<T> shutDown();
}
