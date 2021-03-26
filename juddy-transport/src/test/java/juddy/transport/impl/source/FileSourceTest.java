package juddy.transport.impl.source;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.dto.StringApiCallArguments;
import juddy.transport.impl.config.StartConfiguration;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.source.file.FileSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static juddy.transport.common.Constants.API_TIMEOUT_MS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static scala.collection.JavaConverters.asScala;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:hiddenField"})
@Configuration
@Import(StartConfiguration.class)
@SpringJUnitConfig(FileSourceTest.class)
class FileSourceTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ApiEngineContextProvider apiEngineContextProvider;
    @Autowired
    private FileSource fileSource;

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArguments() throws Exception {
        Path testFile = testFile();
        List<String> expected = Files.readAllLines(testFile).subList(0, 3);
        List<String> target = new ArrayList<>();
        AtomicLong steps = new AtomicLong();
        Source.empty(ArgsWrapper.class)
                .via(fileSource.getStageConnector())
                .take(3)
                .runWith(Sink.foreach(arg -> {
                    target.add(((StringApiCallArguments)
                            arg.getApiCallArguments())
                            .getValue()
                            .replace("\r", ""));
                    steps.incrementAndGet();
                }), getActorSystem());
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(() -> steps.get() >= 3);
        assertThat(expected).isEqualTo(target);
    }

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArgumentsByTestSink() throws Exception {
        Path testFile = testFile();
        List<String> expected = Files.readAllLines(testFile);
        List<ArgsWrapper> argsExpected = expected
                .stream()
                .limit(3)
                .map(s -> s.concat("\r"))
                .map(ArgsWrapper::of)
                .collect(Collectors.toList());
        Source.empty(ArgsWrapper.class)
                .via(fileSource.getStageConnector())
                .runWith(TestSink.probe(getActorSystem()), getActorSystem())
                .request(3)
                .expectNextN(asScala(argsExpected).toList());
    }

    @Bean
    public FileSource fileSource() throws Exception {
        FileSource fileSource = new FileSource(testFile());
        fileSource.afterPropertiesSet();
        return fileSource;
    }

    private Path testFile() throws URISyntaxException {
        return Paths.get(ClassLoader.getSystemResource("fileSource/api-calls-source.txt").toURI());
    }

    private ActorSystem getActorSystem() {
        return apiEngineContextProvider.getApiEngineContext().getActorSystem();
    }
}
