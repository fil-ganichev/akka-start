package juddy.transport.test.sink;

import juddy.transport.api.common.Api;

@Api
public interface TestApiMock {

    default <T> T process(T value) {
        return value;
    }
}
