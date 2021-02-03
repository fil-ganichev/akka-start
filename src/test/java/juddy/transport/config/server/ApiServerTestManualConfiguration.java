package juddy.transport.config.server;

import juddy.transport.api.*;
import juddy.transport.impl.ApiEngineImpl;
import juddy.transport.impl.ApiProxiedServerImpl;
import juddy.transport.impl.ApiServerImpl;
import juddy.transport.impl.FileSource;
import juddy.transport.utils.FileSourceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import static org.mockito.Mockito.spy;

@Configuration
@ComponentScan("juddy.transport.api")
public class ApiServerTestManualConfiguration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public ApiProxiedServerImpl apiServer() {
        return ApiProxiedServerImpl.of(Arrays.asList(TestApi.class));
    }

    @Bean
    public ApiProxiedServerImpl apiServerPhaseOne() {
        return ApiProxiedServerImpl.of(Arrays.asList(TestApiPhaseOne.class));
    }

    @Bean
    public ApiServerImpl apiServerPhaseTwo() {
        return ApiServerImpl.of(Arrays.asList(TestApiPhaseTwo.class));
    }

    @Bean
    public ApiServerImpl sinkServer() {
        return ApiServerImpl.of(Arrays.asList(TestApiSink.class));
    }

    @Bean
    public TestApi testApi(ProxiedStage apiServer) {
        return apiServer.getProxy(TestApi.class);
    }

    @Bean
    public TestApiPhaseOne testApiPhaseOne(ProxiedStage apiServerPhaseOne) {
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
    public ApiEngine apiEngine(ApiProxiedServerImpl apiServer) {
        return ApiEngineImpl.of(apiServer).run();
    }

    @Bean
    public ApiEngine apiEngineTwoPhases(ApiProxiedServerImpl apiServerPhaseOne, ApiServerImpl apiServerPhaseTwo, ApiServerImpl sinkServer) {
        return ApiEngineImpl.of(apiServerPhaseOne)
                .connect(apiServerPhaseTwo)
                .connect(sinkServer)
                .withErrorListener(e -> logger.error(e.toString(), e)).run();
    }

    @Bean
    public ApiEngine apiEngineFromSource(ApiServerImpl sinkServer, FileSource fileSource) {
        return ApiEngineImpl.of(fileSource).connect(sinkServer);
    }
}