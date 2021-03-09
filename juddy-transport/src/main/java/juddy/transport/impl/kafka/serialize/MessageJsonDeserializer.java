package juddy.transport.impl.kafka.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import juddy.transport.impl.args.Message;

public class MessageJsonDeserializer extends JsonDeserializer<Message> {

    public MessageJsonDeserializer() {
        super(Message.class);
    }

    public MessageJsonDeserializer(ObjectMapper objectMapper) {
        super(Message.class, objectMapper);
    }
}
