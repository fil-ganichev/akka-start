package juddy.transport.config.net;

import juddy.transport.api.TestApiFioEnricher;
import juddy.transport.api.TestApiPerson;
import juddy.transport.api.TestApiPersonServer;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.common.TransportMode;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.server.ApiServerImpl;
import juddy.transport.impl.source.JsonFileSource;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.spy;

@Configuration
@Import(StartConfiguration.class)
@ComponentScan("juddy.transport.api")
public class TcpTransportTestConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public ApiEngine apiEngineClient(ApiEngineFactory apiEngineFactory,
                                     JsonFileSource<TestApiPerson.Person> jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPerson.class, TestApiPersonServer.class)))
                .connect(apiEngineFactory.tcpClientTransport("127.0.0.1", 8887, TransportMode.RESULT))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public ApiEngine apiEngineClientResultAsSource(ApiEngineFactory apiEngineFactory,
                                                   JsonFileSource<TestApiPerson.Person> jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPerson.class, TestApiPersonServer.class)))
                .connect(apiEngineFactory.tcpClientTransport("127.0.0.1", 8887, TransportMode.RESULT_AS_SOURCE))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)))
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean(initMethod = "run")
    public ApiEngine apiEngineServer(ApiEngineFactory apiEngineFactory,
                                     ApiServerImpl testApiFioEnricherServer) {
        return ApiEngineImpl.of(apiEngineFactory.tcpServerTransport("127.0.0.1", 8887))
                .connect(testApiFioEnricherServer)
                .withErrorListener(e -> logger.error(e.toString(), e));
    }

    @Bean
    public JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper(ApiSerializer apiSerializer)
            throws IOException, URISyntaxException {
        return new JsonFileSourceHelper<>("person-source.json", TestApiPerson.Person.class, apiSerializer);
    }

    @Bean
    public JsonFileSource<TestApiPerson.Person> jsonFileSource(
            JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper) {
        return jsonFileSourceHelper.getJsonFileSource();
    }

    @Bean
    public TestApiFioEnricher testApiFioEnricher() {
        return spy(new TestApiFioEnricher() {
        });
    }

    @Bean
    public ApiServerImpl testApiFioEnricherServer(TestApiFioEnricher testApiFioEnricher) {
        return ApiServerImpl.of(Collections.singletonMap(TestApiFioEnricher.class, testApiFioEnricher));
    }
}
