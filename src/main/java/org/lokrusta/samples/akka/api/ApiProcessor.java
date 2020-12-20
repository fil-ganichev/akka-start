package org.lokrusta.samples.akka.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;

public class ApiProcessor {

    private final ArrayBlockingQueue<Pair<Message, CompletableFuture<Message>>> apiCalls;
    private final Map<Long, Pair<Message, CompletableFuture<Message>>> responses;
    private final ObjectMapper objectMapper;

    public ApiProcessor(int queueSize, ObjectMapper objectMapper) {
        this.apiCalls = new ArrayBlockingQueue<>(queueSize);
        this.responses = new ConcurrentHashMap<>();
        this.objectMapper = objectMapper;
    }

    public CompletionStage<Message> request(CustomMethodParameter customMethodParameter) {
        Pair<Message, CompletableFuture<Message>> response = fromParameter(customMethodParameter);
        try {
            apiCalls.put(response);
        } catch (Exception e) {
            throw new ApiException(e);
        }
        return response.getRight();
    }

    public Pair<Message, CompletableFuture<Message>> req(CustomMethodParameter customMethodParameter) {
        Pair<Message, CompletableFuture<Message>> response = fromParameter(customMethodParameter);
        responses.put(response.getKey().getCorrelationId(), response);
        return response;
    }

    public void response(Message message) {
        Pair<Message, CompletableFuture<Message>> response = responses.remove(message.getCorrelationId());
        response.getRight().complete(message);
    }

    public Message next() {
        try {
            Pair<Message, CompletableFuture<Message>> response = apiCalls.take();
            responses.put(response.getLeft().getCorrelationId(), response);
            return response.getLeft();
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    public int queueSize() {
        return responses.size();
    }

    private String toBase64(Object source) {
        try {
            return new String(Base64.getEncoder().encode(objectMapper.writeValueAsBytes(source)));
        } catch (Exception e) {
            throw new ApiException(e);
        }
    }

    private Pair<Message, CompletableFuture<Message>> fromParameter(CustomMethodParameter customMethodParameter) {
        Message message = Message.builder()
                .messageType(Message.MessageType.REQUEST)
                .base64Json(toBase64(customMethodParameter))
                .correlationId(ThreadLocalRandom.current().nextLong())
                .build();
        return Pair.of(message, new CompletableFuture<>());
    }

}
