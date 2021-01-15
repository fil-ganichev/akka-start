package org.lokrusta.prototypes.connect.impl.server;

import org.junit.jupiter.api.Test;
import org.lokrusta.prototypes.connect.api.TestApi;
import org.lokrusta.prototypes.connect.config.server.ApiServerTestAutoConfiguration;
import org.lokrusta.prototypes.connect.impl.ApiEngineImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringJUnitConfig(ApiServerTestAutoConfiguration.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class ApiServerAutoConfigExceptionTest {

    @Autowired
    private ApiEngineImpl apiEngine;

    @Test
    void when_callApiServerWithException_then_throwIt() {
        TestApi testApi = apiEngine.findProxy(TestApi.class);
        assertThrows(ExecutionException.class, () -> testApi.split(null).get());
        apiEngine.run();
    }
}
