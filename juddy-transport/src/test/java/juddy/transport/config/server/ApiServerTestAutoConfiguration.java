package juddy.transport.config.server;

import juddy.transport.api.*;
import juddy.transport.api.engine.ApiEngine;
import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.engine.ApiEngineFactory;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.source.file.FileSource;
import juddy.transport.impl.source.file.JsonFileSource;
import juddy.transport.impl.test.source.FileSourceHelper;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import juddy.transport.utils.GenderPersonSource;
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
        return ApiEngineImpl.of(apiEngineFactory.apiProxiedServer(Map.of(TestApiPhaseOne.class,
                TestApiPhaseOneServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPhaseTwo.class, TestApiPhaseTwoServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class)));
    }

    @Bean
    public ApiEngine apiEngineFromSource(ApiEngineFactory apiEngineFactory, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource).connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class,
                TestApiSinkServer.class)));
    }

    @Bean
    public ApiEngine apiEngineFromSourceTwoPhases(ApiEngineFactory apiEngineFactory, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSimpleSplitter.class,
                        TestApiSimpleSplitterServer.class)))
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
    public ApiEngine apiEngineFromJsonSourceWithMultiplyArguments(ApiEngineFactory apiEngineFactory,
                                                                  JsonFileSource jsonFileSource) {
        return ApiEngineImpl.of(jsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPersonFio.class, TestApiPersonFioServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))
                        .withErrorListener(e -> logger.error(e.toString(), e)));
    }

    @Bean
    public ApiEngine apiEngineFromSourceWithMultiplyApi(ApiEngineFactory apiEngineFactory,
                                                        GenderPersonSource customJsonFileSource) {
        return ApiEngineImpl.of(customJsonFileSource)
                .connect(apiEngineFactory.apiServer(Map.of(TestApiGenderPerson.class, TestApiGenderPersonServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))
                        .withErrorListener(e -> logger.error(e.toString(), e)));
    }

    @Bean
    public FileSourceHelper fileSourceHelper() throws IOException, URISyntaxException {
        return new FileSourceHelper("fileSource/api-calls-source.txt");
    }

    @Bean
    public FileSource fileSource(FileSourceHelper fileSourceHelper) {
        return fileSourceHelper.getFileSource();
    }

    @Bean
    public JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper(ApiSerializer apiSerializer)
            throws IOException, URISyntaxException {
        return new JsonFileSourceHelper<>("fileSource/person-source.json", TestApiPerson.Person.class, apiSerializer);
    }

    @Bean
    public JsonFileSource<TestApiPerson.Person> jsonFileSource(
            JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper) {
        return jsonFileSourceHelper.getJsonFileSource();
    }

    @Bean
    public JsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper(
            ApiSerializer apiSerializer) throws Exception {
        return new JsonFileSourceHelper<>("fileSource/person-gender-source.json", TestApiGenderPerson.Person.class,
                apiSerializer, GenderPersonSource.class);
    }

    @Bean
    public GenderPersonSource customJsonFileSource(
            JsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper) {
        return (GenderPersonSource) customJsonFileSourceHelper.getJsonFileSource();
    }
}
