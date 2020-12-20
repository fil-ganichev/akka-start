package org.lokrusta.samples.akka.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private MessageType messageType;
    private long correlationId;
    private String base64Json;

    @AllArgsConstructor
    static enum MessageType {

        WELCOME(0),
        REQUEST(1);

        private int type;
    }
}
