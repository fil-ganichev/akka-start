package juddy.transport.utils;

import juddy.transport.impl.FileSource;
import lombok.Getter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class FileSourceHelper {

    private final FileSource fileSource;
    private final List<String> values;

    public FileSourceHelper() throws IOException, URISyntaxException {
        Path testFile = Paths.get(ClassLoader.getSystemResource("api-calls-source.txt").toURI());
        values = Files.readAllLines(testFile)
                .stream()
                .map(s -> s.concat("\r"))
                .collect(Collectors.toList());
        fileSource = new FileSource(testFile);
    }
}
