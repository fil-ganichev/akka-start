package org.lokrusta.prototypes.connect.impl.client;

import org.junit.jupiter.api.Test;
import org.lokrusta.prototypes.connect.api.TestApi;
import org.lokrusta.prototypes.connect.config.client.ApiClientTestAutoConfiguration;
import org.lokrusta.prototypes.connect.impl.ApiEngineImpl;
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
    @Autowired
    private ApiEngineImpl apiEngineServer;

    @Test
    void when_callRemoteApiServerViaTcp_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        TestApi testApi = apiEngineClient.findProxy(TestApi.class);
        List<String> cities = testApi.split("Москва, Минск, Киев, Таллин, Рига, Кишинев").get(500, TimeUnit.MILLISECONDS);
        assertThat(cities).containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }
}
