package juddy.transport.impl.source;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import juddy.transport.api.source.ApiSource;
import juddy.transport.impl.common.StageBase;

public abstract class ApiSourceImpl<In> extends StageBase implements ApiSource<In> {

    protected abstract Source<In, NotUsed> createSource();

    protected abstract Source<In, NotUsed> getSource();
}
