package org.lokrusta.prototypes.connect.api;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiBean(TestApi.class)
@Component
public class TestApiServer {

    public List<String> split(String source) {
        return Arrays.asList(source.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
