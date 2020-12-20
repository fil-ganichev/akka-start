package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

public class TcpPointBase {

    public static final int POWER_BASE = 1000000;

    private final ObjectMapper objectMapper;

    public TcpPointBase(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected <T> T fromString(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    protected String toString(Object source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    protected String messageToString(Message source) {
        Message copy = Message.builder()
                .messageType(source.getMessageType())
                .correlationId(source.getCorrelationId())
                .base64Json(new String(Base64.getDecoder().decode(source.getBase64Json().getBytes())))
                .build();
        return toString(copy);
    }

    protected Message messageFromString(String source) {
        return fromString(source, Message.class);
    }

    protected CustomMethodParameter parameterFromBase64String(String source) {
        return fromString(new String(Base64.getDecoder().decode(source.getBytes())), CustomMethodParameter.class);
    }

    protected String toBase64String(Object source) {
        return new String(Base64.getEncoder().encode(toString(source).getBytes()));
    }

    protected void work(int power) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < power; i++) {
            long result = factorial(ThreadLocalRandom.current().nextInt(1, 25));
        }
        long end = System.currentTimeMillis();
        //System.out.printf("Время выполнения: %.3f%n", ((double)(end - start) / 1000));
    }

    private long factorial(int power) {
        if (power == 1) return power;
        else return power * factorial(power - 1);
    }
}
