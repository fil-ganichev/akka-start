package juddy.transport.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApiPhaseOne {

    CompletableFuture<List<String>> split(String source);
}
