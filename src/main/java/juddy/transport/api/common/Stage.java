package juddy.transport.api.common;

import juddy.transport.api.args.ArgsWrapper;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Общий интерфейс для всех компонент akka-коннектора
 *
 * @author Филипп Ганичев
 */
public interface Stage {

    ArgsWrapper next(ArgsWrapper argsWrapper);

    void init();

    Stage withArgsConverter(Function<ArgsWrapper, ArgsWrapper> argsConverter);

    Stage withErrorListener(Consumer<Exception> errorListener);
}
