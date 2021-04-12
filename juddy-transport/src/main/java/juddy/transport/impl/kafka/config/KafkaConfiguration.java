package juddy.transport.impl.kafka.config;

import juddy.transport.impl.kafka.consumer.ConsumerFactory;
import juddy.transport.impl.kafka.producer.ProducerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfiguration {

    @Bean
    public ConsumerFactory consumerFactory() {
        return new ConsumerFactory();
    }

    @Bean
    public ProducerFactory producerFactory() {
        return new ProducerFactory();
    }
}
