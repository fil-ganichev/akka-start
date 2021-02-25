package juddy.transport.test.sink;

import juddy.transport.api.common.ApiBean;
import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

@ApiBean(TestApiSink.class)
public class TestApiSinkServer implements TestApiSink {

    private final List objects = new ArrayList();
    private final AtomicLong processed = new AtomicLong();

    @Override
    public void process(Object object) {
        objects.add(object);
        processed.incrementAndGet();
    }

    public void check(Object... values) {
        Assertions.assertThat(objects).containsExactly(values);
    }

    public void reset() {
        objects.clear();
    }

    public Callable<Boolean> processed(long amount) {
        return () -> processed.get() >= amount;
    }
}
