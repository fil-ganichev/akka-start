package juddy.transport.impl.server;

import juddy.transport.api.TestApi;
import juddy.transport.api.TestApiPhaseOne;
import juddy.transport.api.TestApiPhaseTwo;
import juddy.transport.test.sink.TestApiSinkServer;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.config.server.ApiServerTestManualConfiguration;
import juddy.transport.impl.test.source.FileSourceHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:throwsCount"})
@SpringJUnitConfig(ApiServerTestManualConfiguration.class)
class ApiServerManualConfigTest {

    @Autowired
    private ApiEngine apiEngineFromSource;
    @Autowired
    private TestApiSinkServer testApiSinkServer;
    @Autowired
    private FileSourceHelper fileSourceHelper;
    @Autowired
    private TestApi testApi;
    @Autowired
    private TestApiPhaseTwo testApiPhaseTwo;
    @Autowired
    private TestApiPhaseOne testApiPhaseOne;

    // Получаем строки из TestSource, преобразуем из в вызов единственного метода API, проверяем результат
    @Test
    void when_readFileSourceAndRunServerApi_then_ok() {
        testApiSinkServer.reset();
        apiEngineFromSource.run();
        await().atMost(1, TimeUnit.SECONDS).until(
                testApiSinkServer.processed(fileSourceHelper.getValues().size()));
        testApiSinkServer.check(fileSourceHelper.getValues().toArray(new String[0]));
    }

    // Вызываем сервер явно, получаем результат
    @Test
    void when_callApiServer_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        List<String> cities = testApi.split("Москва, Минск, Киев, Таллин, Рига, Кишинев")
                .get(500, TimeUnit.MILLISECONDS);
        assertThat(cities).containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }

    // Вызываем сервер явно, получаем результат, далее еще один вызов
    @Test
    void when_callApiServerAndNextOne_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        testApiSinkServer.reset();
        List<String> cities = testApiPhaseOne.split("Москва, Минск, Киев, Таллин, Рига, Кишинев")
                .get(500, TimeUnit.MILLISECONDS);
        await().atMost(1, TimeUnit.SECONDS).until(
                testApiSinkServer.processed(1));
        testApiSinkServer.check(6);
    }
}
