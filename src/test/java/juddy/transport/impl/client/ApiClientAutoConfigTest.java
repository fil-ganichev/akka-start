package juddy.transport.impl.client;

import juddy.transport.api.TestApi;
import juddy.transport.config.client.ApiClientTestAutoConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(ApiClientTestAutoConfiguration.class)
public class ApiClientAutoConfigTest {

    @Autowired
    private ApiEngineImpl apiEngineClient;

    @Test
    void when_callRemoteApiServerViaTcp_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        TestApi testApi = apiEngineClient.findProxy(TestApi.class);
        List<String> cities = testApi.split("Москва, Минск, Киев, Таллин, Рига, Кишинев").get(500, TimeUnit.MILLISECONDS);
        assertThat(cities).containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }
}
