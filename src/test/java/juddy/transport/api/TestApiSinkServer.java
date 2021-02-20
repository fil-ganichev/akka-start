package juddy.transport.api;

import juddy.transport.api.common.ApiBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@ApiBean(TestApiSink.class)
@Component
public class TestApiSinkServer implements TestApiSink {

    private final List objects = new ArrayList();
    private final AtomicLong processed = new AtomicLong();

    @Override
    public void process(Object object) {
        objects.add(object);
        processed.incrementAndGet();
    }

    public void check(Object... values) {
        assertThat(objects).containsExactly(values);
    }

    public void reset() {
        objects.clear();
    }

    public Callable<Boolean> processed(long amount) {
        return () -> processed.get() >= amount;
    }
}
