package juddy.transport.impl.common;

import akka.NotUsed;
import akka.stream.javadsl.JavaFlowSupport;
import akka.stream.javadsl.Source;
import juddy.transport.impl.error.ApiException;
import juddy.transport.api.args.ArgsWrapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SubmissionPublisher;

public class ApiCallProcessor {

    private final SubmissionPublisher<ArgsWrapper> submissionPublisher = new SubmissionPublisher<>();
    private final Map<String, CompletableFuture<ArgsWrapper>> responses = new ConcurrentHashMap<>();

    public Source<ArgsWrapper, NotUsed> clientApiSource() {
        Source<ArgsWrapper, NotUsed> apiSource =
                JavaFlowSupport.Source.<ArgsWrapper>asSubscriber()
                        .mapMaterializedValue(
                                subscriber -> {
                                    submissionPublisher.subscribe(subscriber);
                                    return NotUsed.getInstance();
                                });
        return apiSource;
    }

    public CompletableFuture<ArgsWrapper> request(ArgsWrapper argsWrapper) {
        argsWrapper.setCorrelationId(UUID.randomUUID().toString());
        CompletableFuture<ArgsWrapper> response = new CompletableFuture<>();
        responses.put(argsWrapper.getCorrelationId(), response);
        submissionPublisher.submit(argsWrapper);
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
            throw new ApiException(String.format("Request with correlationId %s not found", argsWrapper.getCorrelationId()));
        }
    }
}
