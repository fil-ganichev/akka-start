package juddy.transport.api;

import juddy.transport.api.common.Api;

@Api
public interface TestApiSink {

    void process(Object object);
}
