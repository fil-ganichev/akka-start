package juddy.transport.api;

import juddy.transport.api.common.Api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApi {

    CompletableFuture<List<String>> split(String source);
}
