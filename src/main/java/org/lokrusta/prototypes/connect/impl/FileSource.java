package org.lokrusta.prototypes.connect.impl;

import akka.NotUsed;
import akka.stream.alpakka.file.javadsl.FileTailSource;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import org.lokrusta.prototypes.connect.api.ArgsWrapper;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.Duration;

public class FileSource extends ApiSourceImpl<String> {

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
    protected Source createSource() {
        Source<String, NotUsed> lines = FileTailSource.createLines(getFilePath(),
                fileSourceProperties.getMaxLineSize(),
                Duration.ofMillis(fileSourceProperties.getPollingInterval()),
                fileSourceProperties.getDelimiter(),
                fileSourceProperties.getCharset());
        return lines;
    }

    @Override
    protected Source<String, NotUsed> getSource() {
        return lines;
    }

    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class)
                .merge(getSource().map(ArgsWrapperImpl::of))
                .log(logTitle("value from source"))
                .map(this::next);
    }

    private Path getFilePath() {
        return fileSourceProperties.getFilePath() == null
                ? FileSystems.getDefault().getPath(fileSourceProperties.getFileName())
                : fileSourceProperties.getFilePath();
    }
}