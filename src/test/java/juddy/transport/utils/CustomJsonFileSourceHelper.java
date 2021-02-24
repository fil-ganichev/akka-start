package juddy.transport.utils;

import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.source.CustomJsonFileSource;
import lombok.Getter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class CustomJsonFileSourceHelper<T> {

    private final CustomJsonFileSource jsonFileSource;
    private final List<T> values;

    public CustomJsonFileSourceHelper(Class<T> clazz, ApiSerializer apiSerializer)
            throws IOException, URISyntaxException {
        Path testFile = Paths.get(ClassLoader.getSystemResource("person-gender-source.json").toURI());
        values = Files.readAllLines(testFile)
                .stream()
                .map(s -> apiSerializer.fromString(s, clazz))
                .collect(Collectors.toList());
        jsonFileSource = new CustomJsonFileSource(testFile, clazz);
    }

    public List<T> getValues() {
        return values;
    }
}
