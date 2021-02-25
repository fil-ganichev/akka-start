package juddy.transport.api;

import juddy.transport.api.common.ApiBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiBean(TestApiPhaseOne.class)
@Component
public class TestApiPhaseOneServer {

    public List<String> split(String source) {
        return Arrays.stream(source.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
