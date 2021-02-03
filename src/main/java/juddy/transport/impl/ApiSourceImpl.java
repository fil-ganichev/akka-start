package juddy.transport.impl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import juddy.transport.api.ApiSource;

public abstract class ApiSourceImpl<In> extends StageBase implements ApiSource<In> {

    protected abstract Source<In, NotUsed> createSource();

    protected abstract Source<In, NotUsed> getSource();
}
