package org.lokrusta.prototypes.connect.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    private Message.MessageType messageType;
    private String correlationId;
    private String base64Json;

    @AllArgsConstructor
    public static enum MessageType {

        WELCOME(0),
        REQUEST(1);

        private int type;
    }
}
