package juddy.transport.impl.kafka.producer;

import akka.NotUsed;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Flow;
import com.typesafe.config.Config;
import juddy.transport.impl.context.ApiEngineContext;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.error.ApiEngineException;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Function;

import static juddy.transport.impl.common.MessageConstants.API_ENGINE_CONTEXT_NOT_AVAILABLE;

public class ProducerFactory {

    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;

    public <T> ProducerSettings<String, T> initProducerProperties(Map<String, String> producerProperties,
                                                                  Serializer<T> serializer) {

        Config producerConfig = getApiEngineContext()
                .getActorSystem()
                .settings()
                .config()
                .getConfig("akka.kafka.producer");

        ProducerSettings<String, T> producerSettings =
                ProducerSettings.create(producerConfig, new StringSerializer(), serializer)
                        .withProperties(producerProperties);

        return producerSettings;
    }

    public <T, R> Flow<T, ProducerMessage.Results<String, R, NotUsed>, NotUsed> createProducerFlow(
            ProducerSettings<String, R> producerSettings,
            Function<T, ProducerMessage.Envelope<String, R, NotUsed>> dataConverter) {
        return Flow.<T>create()
                .map(dataConverter::apply)
                .via(Producer.flexiFlow(producerSettings));
    }

    private ApiEngineContext getApiEngineContext() {
        if (apiEngineContextProvider == null) {
            throw new ApiEngineException(API_ENGINE_CONTEXT_NOT_AVAILABLE);
        }
        return apiEngineContextProvider.getApiEngineContext();
    }
}
