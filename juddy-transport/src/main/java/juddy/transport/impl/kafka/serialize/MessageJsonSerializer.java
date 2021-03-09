package juddy.transport.impl.kafka.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import juddy.transport.impl.args.Message;

public class MessageJsonSerializer extends JsonSerializer<Message> {

    public MessageJsonSerializer() {
    }

    public MessageJsonSerializer(ObjectMapper objectMapper) {
        super(objectMapper);
    }
}
