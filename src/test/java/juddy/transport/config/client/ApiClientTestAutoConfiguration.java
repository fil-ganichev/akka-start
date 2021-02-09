package juddy.transport.config.client;

import juddy.transport.api.TestApi;
import juddy.transport.api.TestApiServer;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Arrays;
import java.util.Map;

@Configuration
@Import(StartConfiguration.class)
@ComponentScan("juddy.transport.api")
public class ApiClientTestAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean(initMethod = "run")
    public ApiEngine apiEngineClient(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiClient(Arrays.asList(TestApi.class)))
                .connect(apiEngineFactory.tcpClientTransport("127.0.0.1", 8889))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean(initMethod = "run")
    public ApiEngine apiEngineServer(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.tcpServerTransport("127.0.0.1", 8889)
                .withErrorListener(e -> logger.error(e.toString(), e)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApi.class, TestApiServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }
}
