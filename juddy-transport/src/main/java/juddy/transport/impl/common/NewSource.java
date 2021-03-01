package juddy.transport.impl.common;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;

public interface NewSource {

    Flow<ArgsWrapper, ArgsWrapper, NotUsed> getNewSource();

    boolean enabled();
}
