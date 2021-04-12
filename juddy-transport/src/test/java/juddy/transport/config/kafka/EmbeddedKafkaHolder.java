package juddy.transport.config.kafka;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

public class EmbeddedKafkaHolder {

    private static final EmbeddedKafkaBroker EMBEDDED_KAFKA_BROKER = createKafkaBroker();

    public EmbeddedKafkaBroker getEmbeddedKafkaBroker() {
        return EMBEDDED_KAFKA_BROKER;
    }

    private static EmbeddedKafkaBroker createKafkaBroker() {
        AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(KafkaBrokerConfiguration.class);
        EmbeddedKafkaBroker kafkaEmbedded = context.getBean(EmbeddedKafkaBroker.class);
        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        return kafkaEmbedded;
    }
}
