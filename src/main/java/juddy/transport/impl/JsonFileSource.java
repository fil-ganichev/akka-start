package juddy.transport.impl;

import juddy.transport.api.ArgsWrapper;
import juddy.transport.api.dto.StringApiCallArguments;

import java.nio.file.Path;
import java.util.function.Function;

public class JsonFileSource<T> extends FileSource {

    private final Class<T> objectClass;

    public JsonFileSource(FileSourceProperties fileSourceProperties, Class<T> objectClass) {
        super(fileSourceProperties);
        this.objectClass = objectClass;
        withArgsConverter(getArgsConverter());
    }

    public JsonFileSource(String fileName, Class<T> objectClass) {
        super(fileName);
        this.objectClass = objectClass;
        withArgsConverter(getArgsConverter());
    }

    public JsonFileSource(Path filePath, Class<T> objectClass) {
        super(filePath);
        this.objectClass = objectClass;
        withArgsConverter(getArgsConverter());
    }

    protected Function<ArgsWrapper, ArgsWrapper> getArgsConverter() {
        return argsWrapper -> ArgsWrapperImpl.of(ApiHelper.fromString(
                ((StringApiCallArguments) argsWrapper.getApiCallArguments()).getValue(),
                objectClass));
    }
}
