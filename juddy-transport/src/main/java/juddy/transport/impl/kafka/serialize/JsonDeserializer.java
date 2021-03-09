package juddy.transport.impl.kafka.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import juddy.transport.impl.common.json.ObjectMapperUtils;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

public class JsonDeserializer<T> implements Deserializer {

    private final ObjectMapper objectMapper;
    private final Class<T> objectClass;

    public JsonDeserializer(Class<T> objectClass) {
        this(objectClass, ObjectMapperUtils.createObjectMapper());
    }

    public JsonDeserializer(Class<T> objectClass, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.objectClass = objectClass;
    }

    @Override
    public T deserialize(String topic, byte[] messageBytes) {
        return internalDeserialize(messageBytes);
    }

    @Override
    public T deserialize(String topic, Headers headers, byte[] messageBytes) {
        return internalDeserialize(messageBytes);
    }

    private T internalDeserialize(byte[] messageBytes) {
        try {
            return objectMapper.readValue(messageBytes, objectClass);
        } catch (Exception e) {
            throw new KafkaDeserializationException(e);
        }
    }
}
