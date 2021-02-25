package juddy.transport.test.sink;

import juddy.transport.api.common.Api;

@Api
public interface TestApiSink {

    void process(Object object);
}
