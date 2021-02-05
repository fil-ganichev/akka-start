package juddy.transport.impl.error;

import juddy.transport.api.args.ArgsWrapper;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class ErrorProcessor {

    private Consumer<Exception> errorListener;

    public Exception onError(Exception e) throws Exception {
        if (errorListener != null) {
            errorListener.accept(e);
        }
        return e;
    }

    public ArgsWrapper checkError(ArgsWrapper argsWrapper) throws Exception {
        if (argsWrapper.getException() != null) {
            throw argsWrapper.getException();
        }
        return argsWrapper;
    }
}
