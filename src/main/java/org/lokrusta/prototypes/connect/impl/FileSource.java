package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.alpakka.file.javadsl.FileTailSource;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;
import org.springframework.beans.factory.InitializingBean;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

public class FileSource extends ApiSourceImpl<String> implements InitializingBean {

    private final FileSourceProperties fileSourceProperties;
    private final Source<String, NotUsed> lines;

    public FileSource(FileSourceProperties fileSourceProperties) {
        this.fileSourceProperties = fileSourceProperties;
        this.lines = createSource();
    }

    public FileSource(String fileName) {
        this(FileSourceProperties.of(fileName));
    }

    public FileSource(Path filePath) {
        this(FileSourceProperties.of(filePath));
    }

    @Override
    public void init() {
    }

    @Override
    protected Source createSource() {
        Source<String, NotUsed> lines = FileTailSource.createLines(getFilePath(),
                fileSourceProperties.getMaxLineSize(),
                Duration.ofMillis(fileSourceProperties.getPollingInterval()),
                fileSourceProperties.getDelimiter(),
                fileSourceProperties.getCharset());
        return lines;
    }

    @Override
    protected Source getSource() {
        return lines;
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector(Source<String, NotUsed> source) {
        return Flow.of(ArgsWrapper.class).merge(source.map(ArgsWrapperImpl::of)).map(this::next);
    }

    private Path getFilePath() {
        return fileSourceProperties.getFilePath() == null
                ? FileSystems.getDefault().getPath(fileSourceProperties.getFileName())
                : fileSourceProperties.getFilePath();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.stageConnector = createConnector(this.lines);
    }
}
