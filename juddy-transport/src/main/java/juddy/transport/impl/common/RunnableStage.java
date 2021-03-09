package juddy.transport.impl.common;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;

public interface RunnableStage {

    void run(Flow<ArgsWrapper, ArgsWrapper, NotUsed> graphProcessor);
}
