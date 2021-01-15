package org.lokrusta.prototypes.connect.impl.source;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.lokrusta.prototypes.connect.api.dto.StringApiCallArguments;
import org.lokrusta.prototypes.connect.impl.ArgsWrapperImpl;
import org.lokrusta.prototypes.connect.impl.FileSource;
import org.lokrusta.prototypes.connect.impl.TestBase;
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
    private ActorSystem actorSystem = ActorSystem.create("QuickStartTest");

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArguments() throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        List<String> expected = Files.readAllLines(testFile).subList(0, 3);
        List<String> target = new ArrayList<>();
        FileSource fileSource = fileSource(testFile);
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> fileSourceConnector = getConnector(fileSource);
        Source.empty(ArgsWrapper.class)
                .via(fileSourceConnector)
                .take(3)
                .runWith(Sink.foreach(arg -> {
                    target.add(((StringApiCallArguments)
                            arg.getApiCallArguments())
                            .getValue()
                            .replace("\r", ""));
                }), actorSystem);
        Thread.sleep(1000);
        assertThat(expected).isEqualTo(target);
    }

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArgumentsByTestSink2() throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        List<String> expected = Files.readAllLines(testFile);
        List<ArgsWrapper> argsExpected = expected
                .stream()
                .limit(3)
                .map(s -> s.concat("\r"))
                .map(ArgsWrapperImpl::of)
                .collect(Collectors.toList());
        FileSource fileSource = fileSource(testFile);
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> fileSourceConnector = getConnector(fileSource);
        Source.empty(ArgsWrapper.class)
                .via(fileSourceConnector)
                .runWith(TestSink.probe(actorSystem), actorSystem)
                .request(3)
                .expectNextN(asScala(argsExpected).toList());
    }
}
