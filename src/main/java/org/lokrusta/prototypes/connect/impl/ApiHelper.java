package org.lokrusta.prototypes.connect.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lokrusta.prototypes.connect.impl.common.ApiCallDeserializationException;
import org.lokrusta.prototypes.connect.impl.common.ApiException;
import org.lokrusta.prototypes.connect.api.ApiSource;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.Stage;

import java.util.Base64;

public class ApiHelper {

    private static final ObjectMapper objectMapper = createMapper();

    public static boolean isSource(Stage stage) {
        return stage instanceof ApiSource;
    }

    public static ArgsWrapper deserialize(String callInfo) {
        try {
            return objectMapper.readValue(callInfo, ArgsWrapperImpl.class);
        } catch (Exception e) {
            throw new ApiCallDeserializationException(e);
        }
    }

    public static String serialize(ArgsWrapper argsWrapper) {
        try {
            return new String(Base64.getEncoder().encode(objectMapper.writeValueAsBytes(argsWrapper)));
        } catch (Exception e) {
            throw new ApiCallDeserializationException(e);
        }
    }

    public static <T> T fromString(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static String toString(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public static String messageToString(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(new String(Base64.getDecoder().decode(source.getBase64Json().getBytes())))
                .build();
        return toString(copy);
    }

    public static Message messageFromString(String source) {
        return fromString(source, Message.class);
    }

    public static ArgsWrapper parameterFromBase64String(String source) {
        return fromString(new String(Base64.getDecoder().decode(source.getBytes())), ArgsWrapperImpl.class);
    }

    public static Message messageFromArgs(ArgsWrapper args) {
        Message message = Message.builder()
                .messageType(Message.MessageType.REQUEST)
                .correlationId(args.getCorrelationId())
                .base64Json(serialize(args))
                .build();
        return message;
    }

    private static ObjectMapper createMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper;
    }
}
