package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.samples.akka.api.ApiException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

public class ApiCallProcessor {

    private static final int QUEUE_SIZE = 1000;
    private final BlockingQueue<ArgsWrapper> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final Map<String, CompletableFuture<ArgsWrapper>> responses = new HashMap<>();

    public Source<ArgsWrapper, NotUsed> clientApiSource() {
        Source<ArgsWrapper, NotUsed> source = Source.repeat("").map(s -> take());
        return source;
    }

    public CompletableFuture<ArgsWrapper> request(ArgsWrapper argsWrapper) {
        try {
            argsWrapper.setCorrelationId(UUID.randomUUID().toString());
            CompletableFuture<ArgsWrapper> response = new CompletableFuture<>();
            queue.put(argsWrapper);
            responses.put(argsWrapper.getCorrelationId(), response);
            return response;
        } catch (InterruptedException e) {
            throw new ApiException(e);
        }
    }

    public void response(ArgsWrapper argsWrapper) {
        CompletableFuture<ArgsWrapper> response = responses.remove(argsWrapper.getCorrelationId());
        if (argsWrapper.getException() != null) {
            response.completeExceptionally(argsWrapper.getException());
        } else {
            response.complete(argsWrapper);
        }
    }

    private ArgsWrapper take() throws InterruptedException {
        return queue.take();
    }

}
