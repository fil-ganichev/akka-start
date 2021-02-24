package juddy.transport.api;

import juddy.transport.api.common.ApiBean;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiBean(TestApiSimpleSplitter.class)
public class TestApiSimpleSplitterServer implements TestApiSimpleSplitter {

    @Override
    public List<String> split(String source) {
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
