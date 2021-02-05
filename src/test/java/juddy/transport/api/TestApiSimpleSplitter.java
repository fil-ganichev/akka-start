package juddy.transport.api;

import juddy.transport.api.common.Api;

import java.util.List;

@Api
public interface TestApiSimpleSplitter {

    List<String> split(String source);
}
