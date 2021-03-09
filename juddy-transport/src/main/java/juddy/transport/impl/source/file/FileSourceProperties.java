package juddy.transport.impl.source.file;

import lombok.Builder;
import lombok.Data;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Data
@Builder
public class FileSourceProperties {

    private static final int DEFAULT_MAX_LINE_SIZE = 1024;
    private static final String DEFAULT_DELIMITER = "\n";
    private static final int DEFAULT_POLLING_INTERVAL = 250;

    private String fileName;
    private int maxLineSize;
    private int pollingInterval;
    private String delimiter;
    private Charset charset;
    private Path filePath;

    public static FileSourceProperties of(String fileName) {
        return FileSourceProperties.builder()
                .charset(StandardCharsets.UTF_8)
                .maxLineSize(DEFAULT_MAX_LINE_SIZE)
                .pollingInterval(DEFAULT_POLLING_INTERVAL)
                .delimiter(DEFAULT_DELIMITER)
                .fileName(fileName)
                .build();
    }

    public static FileSourceProperties of(Path filePath) {
        return FileSourceProperties.builder()
                .charset(StandardCharsets.UTF_8)
                .maxLineSize(DEFAULT_MAX_LINE_SIZE)
                .pollingInterval(DEFAULT_POLLING_INTERVAL)
                .delimiter(DEFAULT_DELIMITER)
                .filePath(filePath)
                .build();
    }
}
