package org.lokrusta.prototypes.connect.api;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ApiBean(TestApiPhaseOne.class)
@Component
public class TestApiPhaseOneServer {

    public List<String> split(String source) {
        return Arrays.asList(source.split(","))
                .stream()
                .map(String::trim)
                .collect(Collectors.toList());
    }
}
