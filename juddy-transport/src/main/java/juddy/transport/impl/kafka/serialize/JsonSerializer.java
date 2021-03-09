package juddy.transport.impl.kafka.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import juddy.transport.impl.common.json.ObjectMapperUtils;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper;

    public JsonSerializer() {
        this(ObjectMapperUtils.createObjectMapper());
    }

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(String topic, T value) {
        return internalSerialize(value);
    }

    @Override
    public byte[] serialize(String topic, Headers headers, T value) {
        return internalSerialize(value);
    }

    private byte[] internalSerialize(T value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new KafkaSerializationException(e);
        }
    }
}
