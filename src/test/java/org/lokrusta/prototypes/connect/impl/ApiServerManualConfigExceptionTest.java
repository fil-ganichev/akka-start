package org.lokrusta.prototypes.connect.impl;

import org.junit.jupiter.api.Test;
import org.lokrusta.prototypes.connect.api.TestApi;
import org.lokrusta.prototypes.connect.api.TestApiPhaseOne;
import org.lokrusta.prototypes.connect.api.TestApiPhaseTwo;
import org.lokrusta.prototypes.connect.api.TestApiSinkServer;
import org.lokrusta.prototypes.connect.config.ApiServerTestManualConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

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
    void when_callApiServerAndNextOneWithException_then_stopFlow() throws InterruptedException, ExecutionException {
        testApiSinkServer.reset();
        when(testApiPhaseTwo.size(anyList())).thenThrow(new RuntimeException("Test"));
        List<String> cities = testApiPhaseOne.split("Москва, Минск, Киев, Таллин, Рига, Кишинев").get();
        Thread.sleep(100);
        testApiSinkServer.check();
    }
}
