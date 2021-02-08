package juddy.transport.config.server;

import juddy.transport.api.*;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.common.ApiSerialilizer;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.source.FileSource;
import juddy.transport.impl.source.JsonFileSource;
import juddy.transport.source.CustomJsonFileSource;
import juddy.transport.utils.CustomJsonFileSourceHelper;
import juddy.transport.utils.FileSourceHelper;
import juddy.transport.utils.JsonFileSourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Configuration
@Import(StartConfiguration.class)
@ComponentScan("juddy.transport.api")
public class ApiServerTestAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean(initMethod = "run")
    public ApiEngine apiEngine(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiProxiedServer(Map.of(TestApi.class, TestApiServer.class)));
    }

    @Bean(initMethod = "run")
    public ApiEngine apiEngineTwoPhases(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiProxiedServer(Map.of(TestApiPhaseOne.class, TestApiPhaseOneServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPhaseTwo.class, TestApiPhaseTwoServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)));
    }

    @Bean
    public ApiEngine apiEngineFromSource(ApiEngineFactory apiEngineFactory, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource).connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)));
    }

    @Bean
    public ApiEngine apiEngineFromSourceTwoPhases(ApiEngineFactory apiEngineFactory, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSimpleSplitter.class, TestApiSimpleSplitterServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)));
    }

    @Bean
    public ApiEngine apiEngineFromJsonSource(ApiEngineFactory apiEngineFactory, JsonFileSource jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPerson.class, TestApiPersonServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))
                        .withErrorListener(e -> logger.error(e.toString(), e)));
    }

    @Bean
    public ApiEngine apiEngineFromJsonSourceWithMultiplyArguments(ApiEngineFactory apiEngineFactory, JsonFileSource jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPersonFio.class, TestApiPersonFioServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))
                        .withErrorListener(e -> logger.error(e.toString(), e)));
    }

    @Bean
    public ApiEngine apiEngineFromSourceWithMultiplyApi(ApiEngineFactory apiEngineFactory, CustomJsonFileSource customJsonFileSource) {
        return ApiEngineImpl.of(customJsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiGenderPerson.class, TestApiGenderPersonServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))
                        .withErrorListener(e -> logger.error(e.toString(), e)));
    }

    @Bean
    public FileSourceHelper fileSourceHelper() throws IOException, URISyntaxException {
        return new FileSourceHelper();
    }

    @Bean
    public FileSource fileSource(FileSourceHelper fileSourceHelper) {
        return fileSourceHelper.getFileSource();
    }

    @Bean
    public JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper(ApiSerialilizer apiSerialilizer) throws IOException, URISyntaxException {
        return new JsonFileSourceHelper(TestApiPerson.Person.class, apiSerialilizer);
    }

    @Bean
    public JsonFileSource jsonFileSource(JsonFileSourceHelper jsonFileSourceHelper) {
        return jsonFileSourceHelper.getJsonFileSource();
    }

    @Bean
    public CustomJsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper(ApiSerialilizer apiSerialilizer) throws IOException, URISyntaxException {
        return new CustomJsonFileSourceHelper(TestApiGenderPerson.Person.class, apiSerialilizer);
    }

    @Bean
    public CustomJsonFileSource customJsonFileSource(CustomJsonFileSourceHelper customJsonFileSourceHelper) {
        return customJsonFileSourceHelper.getJsonFileSource();
    }
}
