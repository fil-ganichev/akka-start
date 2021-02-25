package juddy.transport.impl.test.source;

import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.source.JsonFileSource;
import lombok.Getter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class JsonFileSourceHelper<T> {

    private final JsonFileSource<T> jsonFileSource;
    private final List<T> values;

    public JsonFileSourceHelper(String fileName, Class<T> clazz, ApiSerializer apiSerializer)
            throws IOException, URISyntaxException {
        Path testFile = Paths.get(ClassLoader.getSystemResource(fileName).toURI());
        values = readObjects(testFile, clazz, apiSerializer);
        jsonFileSource = new JsonFileSource<>(testFile, clazz);
    }

    public JsonFileSourceHelper(String fileName, Class<T> clazz, ApiSerializer apiSerializer,
                                Class<? extends JsonFileSource<T>> sourceClazz) throws Exception {
        Path testFile = Paths.get(ClassLoader.getSystemResource(fileName).toURI());
        values = readObjects(testFile, clazz, apiSerializer);
        jsonFileSource = sourceClazz.getConstructor(Path.class, Class.class).newInstance(testFile, clazz);
    }

    public List<T> getValues() {
        return values;
    }

    protected List<T> readObjects(Path testFile, Class<T> clazz, ApiSerializer apiSerializer) throws IOException {
        return Files.readAllLines(testFile)
                .stream()
                .map(s -> apiSerializer.fromString(s, clazz))
                .collect(Collectors.toList());
    }
}
