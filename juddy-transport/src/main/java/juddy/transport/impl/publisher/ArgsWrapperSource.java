package juddy.transport.impl.publisher;

import akka.NotUsed;
import akka.stream.javadsl.JavaFlowSupport;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import lombok.Getter;

import java.util.concurrent.SubmissionPublisher;

@Getter
public class ArgsWrapperSource {

    private final SubmissionPublisher<ArgsWrapper> submissionPublisher = new SubmissionPublisher<>();

    private final Source<ArgsWrapper, NotUsed> apiSource =
            JavaFlowSupport.Source.<ArgsWrapper>asSubscriber()
                    .mapMaterializedValue(
                            subscriber -> {
                                submissionPublisher.subscribe(subscriber);
                                return NotUsed.getInstance();
                            });

    public void submit(ArgsWrapper argsWrapper) {
        submissionPublisher.submit(argsWrapper);
    }
}
