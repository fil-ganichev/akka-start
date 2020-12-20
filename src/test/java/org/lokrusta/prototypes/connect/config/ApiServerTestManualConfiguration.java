package org.lokrusta.prototypes.connect.config;

import org.lokrusta.prototypes.connect.api.*;
import org.lokrusta.prototypes.connect.impl.ApiEngineImpl;
import org.lokrusta.prototypes.connect.impl.ApiServerImpl;
import org.lokrusta.prototypes.connect.impl.FileSource;
import org.lokrusta.prototypes.connect.utils.FileSourceHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.mockito.Mockito.spy;

@Configuration
@ComponentScan("org.lokrusta.prototypes.connect.api")
public class ApiServerTestManualConfiguration {

    @Bean
    public ApiServerImpl apiServer() {
        return ApiServerImpl.of(Arrays.asList(TestApi.class), true);
    }

    @Bean
    public ApiServerImpl apiServerPhaseOne() {
        return ApiServerImpl.of(Arrays.asList(TestApiPhaseOne.class), true);
    }

    @Bean
    public ApiServerImpl apiServerPhaseTwo() {
        return ApiServerImpl.of(Arrays.asList(TestApiPhaseTwo.class), false);
    }

    @Bean
    public ApiServerImpl sinkServer() {
        return ApiServerImpl.of(Arrays.asList(TestApiSink.class), false);
    }

    @Bean
    public TestApi testApi(ApiServerImpl apiServer) {
        return apiServer.getProxy(TestApi.class);
    }

    @Bean
    public TestApiPhaseOne testApiPhaseOne(ApiServerImpl apiServerPhaseOne) {
        return apiServerPhaseOne.getProxy(TestApiPhaseOne.class);
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
    public TestApiPhaseTwoServer testApiPhaseTwoServer() {
        return spy(new TestApiPhaseTwoServer());
    }

    @Bean
    public ApiEngine apiEngine(ApiServerImpl apiServer) {
        return ApiEngineImpl.of(apiServer).run();
    }

    @Bean
    public ApiEngine apiEngineTwoPhases(ApiServerImpl apiServerPhaseOne, ApiServerImpl apiServerPhaseTwo, ApiServerImpl sinkServer) {
        return ApiEngineImpl.of(apiServerPhaseOne).connect(apiServerPhaseTwo).connect(sinkServer).run();
    }

    @Bean
    public ApiEngine apiEngineFromSource(ApiServerImpl sinkServer, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource).connect(sinkServer);
    }
}
