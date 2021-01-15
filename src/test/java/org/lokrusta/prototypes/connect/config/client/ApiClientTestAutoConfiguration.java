package org.lokrusta.prototypes.connect.config.client;

import org.lokrusta.prototypes.connect.api.ApiEngine;
import org.lokrusta.prototypes.connect.api.TestApi;
import org.lokrusta.prototypes.connect.api.TestApiServer;
import org.lokrusta.prototypes.connect.impl.ApiEngineFactory;
import org.lokrusta.prototypes.connect.impl.ApiEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Map;

@Configuration
@ComponentScan("org.lokrusta.prototypes.connect.api")
public class ApiClientTestAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public ApiEngine apiEngineClient(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiClient(Arrays.asList(TestApi.class)))
                .connect(apiEngineFactory.tcpClientTransport("127.0.0.1", 8889))
                .withErrorListener(e -> logger.error(e.toString(), e))
                .run();
    }

    @Bean
    public ApiEngine apiEngineServer(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.tcpServerTransport("127.0.0.1", 8889)
                .withErrorListener(e -> logger.error(e.toString(), e)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApi.class, TestApiServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e))
                .run();
    }

    @Bean
    public ApiEngineFactory apiEngineFactory() {
        return new ApiEngineFactory();
    }
}
