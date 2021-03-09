package juddy.transport.impl.source.file;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.alpakka.file.javadsl.FileTailSource;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.impl.source.ApiSourceImpl;

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
        Source<String, NotUsed> sourceLines = FileTailSource.createLines(getFilePath(),
                fileSourceProperties.getMaxLineSize(),
                Duration.ofMillis(fileSourceProperties.getPollingInterval()),
                fileSourceProperties.getDelimiter(),
                fileSourceProperties.getCharset());
        return sourceLines;
    }

    @Override
    protected Source<String, NotUsed> getSource() {
        return lines;
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class)
                .merge(getSource().map(ArgsWrapper::of))
                .log(logTitle("value from source"))
                .map(this::next)
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    private Path getFilePath() {
        return fileSourceProperties.getFilePath() == null
                ? FileSystems.getDefault().getPath(fileSourceProperties.getFileName())
                : fileSourceProperties.getFilePath();
    }
}
