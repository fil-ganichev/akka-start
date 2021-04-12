package juddy.transport.impl.kafka.consumer;

import akka.kafka.CommitterSettings;
import akka.kafka.ConsumerSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumerProperties<T> {

    private ConsumerSettings<String, T> consumerSettings;
    private CommitterSettings committerSettings;
}
