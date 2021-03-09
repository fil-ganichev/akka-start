package juddy.transport.impl.test.source;

import juddy.transport.impl.source.file.FileSource;
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

    public FileSourceHelper(String fileName) throws IOException, URISyntaxException {
        Path testFile = Paths.get(ClassLoader.getSystemResource(fileName).toURI());
        values = Files.readAllLines(testFile)
                .stream()
                .map(s -> s.concat("\r"))
                .collect(Collectors.toList());
        fileSource = new FileSource(testFile);
    }
}
