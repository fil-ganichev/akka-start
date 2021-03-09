package juddy.transport.impl.server;

import juddy.transport.api.TestApi;
import juddy.transport.api.TestApiPhaseOne;
import juddy.transport.api.TestApiPhaseTwo;
import juddy.transport.config.server.ApiServerTestManualConfiguration;
import juddy.transport.test.sink.TestApiSinkServer;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:throwsCount"})
@SpringJUnitConfig(ApiServerTestManualConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ApiServerManualConfigExceptionTest {

    @Autowired
    private TestApiSinkServer testApiSinkServer;
    @Autowired
    private TestApi testApi;
    @Autowired
    private TestApiPhaseTwo testApiPhaseTwo;
    @Autowired
    private TestApiPhaseOne testApiPhaseOne;

    @Test
    void when_callApiServerWithException_then_throwIt() {
        assertThrows(ExecutionException.class, () -> testApi.split(null).get());
    }

    @Test
    void when_callApiServerAndNextOneWithException_then_stopFlow()
            throws InterruptedException, ExecutionException, TimeoutException {
        testApiSinkServer.reset();
        when(testApiPhaseTwo.size(anyList())).thenThrow(new RuntimeException("Test"));
        List<String> cities = testApiPhaseOne.split("Москва, Минск, Киев, Таллин, Рига, Кишинев")
                .get(500, TimeUnit.MILLISECONDS);
        assertThrows(ConditionTimeoutException.class,
                () -> await().atMost(500, TimeUnit.MILLISECONDS).until(() -> false));
        testApiSinkServer.check();
    }
}
