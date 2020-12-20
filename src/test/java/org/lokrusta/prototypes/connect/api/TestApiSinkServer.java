package org.lokrusta.prototypes.connect.api;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ApiBean(TestApiSink.class)
@Component
public class TestApiSinkServer implements TestApiSink {

    private final List objects = new ArrayList();

    @Override
    public void process(Object object) {
        objects.add(object);
    }

    public void check(Object... values) {
        assertThat(objects).containsExactly(values);
    }

    public void reset() {
        objects.clear();
    }
}
