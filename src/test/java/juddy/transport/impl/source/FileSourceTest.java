package juddy.transport.impl.source;

import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.dto.StringApiCallArguments;
import juddy.transport.impl.TestBase;
import juddy.transport.impl.args.ArgsWrapperImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static scala.collection.JavaConverters.asScala;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSourceTest extends TestBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ActorSystem actorSystem = ActorSystem.create("QuickStartTest");

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArguments() throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        List<String> expected = Files.readAllLines(testFile).subList(0, 3);
        List<String> target = new ArrayList<>();
        FileSource fileSource = fileSource(testFile);
        Source.empty(ArgsWrapper.class)
                .via(fileSource.getStageConnector())
                .take(3)
                .runWith(Sink.foreach(arg -> target.add(((StringApiCallArguments)
                        arg.getApiCallArguments())
                        .getValue()
                        .replace("\r", ""))), actorSystem);
        Thread.sleep(1000);
        assertThat(expected).isEqualTo(target);
    }

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArgumentsByTestSink() throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        List<String> expected = Files.readAllLines(testFile);
        List<ArgsWrapper> argsExpected = expected
                .stream()
                .limit(3)
                .map(s -> s.concat("\r"))
                .map(ArgsWrapperImpl::of)
                .collect(Collectors.toList());
        FileSource fileSource = fileSource(testFile);
        Source.empty(ArgsWrapper.class)
                .via(fileSource.getStageConnector())
                .runWith(TestSink.probe(actorSystem), actorSystem)
                .request(3)
                .expectNextN(asScala(argsExpected).toList());
    }
}
