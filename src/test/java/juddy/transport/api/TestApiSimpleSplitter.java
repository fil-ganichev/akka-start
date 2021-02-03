package juddy.transport.api;

import java.util.List;

@Api
public interface TestApiSimpleSplitter {

    List<String> split(String source);
}
