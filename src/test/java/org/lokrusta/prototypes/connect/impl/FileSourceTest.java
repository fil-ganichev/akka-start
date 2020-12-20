package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.javadsl.TestSink;
import akka.testkit.javadsl.TestKit;
import org.junit.jupiter.api.Test;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
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

class FileSourceTest extends TestBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ActorSystem actorSystem;
    private final TestKit probe;


    public FileSourceTest() {
        actorSystem = ActorSystem.create("QuickStartTest");
        probe = new TestKit(actorSystem);

    }

    @Test
    void when_fileApiCallsSource_thenMapToStringCallArguments() throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        List<String> expected = Files.readAllLines(testFile).subList(0, 3);
        List<String> target = new ArrayList<>();
        FileSource fileSource = new FileSource(testFile);
        fileSource.afterPropertiesSet();
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> fileSourceConnector = fileSource.getStageConnector();
        Source.empty(ArgsWrapper.class)
                .via(fileSourceConnector)
                .take(3)
                .runWith(Sink.foreach(arg -> {
                    target.add(((ArgsWrapperImpl.StringApiCallArguments)
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
        FileSource fileSource = new FileSource(testFile);
        fileSource.afterPropertiesSet();
        Flow<ArgsWrapper, ArgsWrapper, NotUsed> fileSourceConnector = fileSource.getStageConnector();
        Source.empty(ArgsWrapper.class)
                .via(fileSourceConnector)
                .runWith(TestSink.probe(actorSystem), actorSystem)
                .request(3)
                .expectNextN(asScala(argsExpected).toList());
    }
}
