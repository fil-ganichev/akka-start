package org.lokrusta.prototypes.connect.config.server;

import org.lokrusta.prototypes.connect.api.*;
import org.lokrusta.prototypes.connect.impl.ApiEngineFactory;
import org.lokrusta.prototypes.connect.impl.ApiEngineImpl;
import org.lokrusta.prototypes.connect.impl.FileSource;
import org.lokrusta.prototypes.connect.impl.JsonFileSource;
import org.lokrusta.prototypes.connect.source.CustomJsonFileSource;
import org.lokrusta.prototypes.connect.utils.CustomJsonFileSourceHelper;
import org.lokrusta.prototypes.connect.utils.FileSourceHelper;
import org.lokrusta.prototypes.connect.utils.JsonFileSourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Configuration
@ComponentScan("org.lokrusta.prototypes.connect.api")
public class ApiServerTestAutoConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public ApiEngine apiEngine(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiServer(Map.of(TestApi.class, TestApiServer.class), true)).run();
    }

    @Bean
    public ApiEngine apiEngineTwoPhases(ApiEngineFactory apiEngineFactory) {
        return ApiEngineImpl.of(apiEngineFactory.apiServer(Map.of(TestApiPhaseOne.class, TestApiPhaseOneServer.class), true))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiPhaseTwo.class, TestApiPhaseTwoServer.class)))
                .connect(apiEngineFactory.apiServer(Map.of(TestApiSink.class, TestApiSinkServer.class))).run();
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
    public ApiEngineFactory apiEngineFactory() {
        return new ApiEngineFactory();
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
    public JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper() throws IOException, URISyntaxException {
        return new JsonFileSourceHelper(TestApiPerson.Person.class);
    }

    @Bean
    public JsonFileSource jsonFileSource(JsonFileSourceHelper jsonFileSourceHelper) {
        return jsonFileSourceHelper.getJsonFileSource();
    }

    @Bean
    public CustomJsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper() throws IOException, URISyntaxException {
        return new CustomJsonFileSourceHelper(TestApiGenderPerson.Person.class);
    }

    @Bean
    public CustomJsonFileSource customJsonFileSource(CustomJsonFileSourceHelper customJsonFileSourceHelper) {
        return customJsonFileSourceHelper.getJsonFileSource();
    }
}
