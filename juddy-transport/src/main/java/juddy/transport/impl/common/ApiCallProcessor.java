package juddy.transport.impl.common;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.impl.error.ApiException;
import juddy.transport.impl.publisher.ArgsWrapperSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ApiCallProcessor {

    private final ArgsWrapperSource argsWrapperSource = new ArgsWrapperSource();
    private final Map<String, CompletableFuture<ArgsWrapper>> responses = new ConcurrentHashMap<>();

    public CompletableFuture<ArgsWrapper> request(ArgsWrapper argsWrapper) {
        argsWrapper.setCorrelationId(UUID.randomUUID().toString());
        CompletableFuture<ArgsWrapper> response = new CompletableFuture<>();
        responses.put(argsWrapper.getCorrelationId(), response);
        argsWrapperSource.submit(argsWrapper);
        return response;
    }

    public void response(ArgsWrapper argsWrapper) {
        CompletableFuture<ArgsWrapper> response = responses.remove(argsWrapper.getCorrelationId());
        if (response != null) {
            if (argsWrapper.getException() != null) {
                response.completeExceptionally(argsWrapper.getException());
            } else {
                response.complete(argsWrapper);
            }
        } else {
            throw new ApiException(String.format("Request with correlationId %s not found",
                    argsWrapper.getCorrelationId()));
        }
    }

    public Source<ArgsWrapper, NotUsed> clientApiSource() {
        return argsWrapperSource.getApiSource();
    }
}
