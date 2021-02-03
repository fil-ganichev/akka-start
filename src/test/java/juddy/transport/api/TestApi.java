package juddy.transport.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApi {

    CompletableFuture<List<String>> split(String source);
}
