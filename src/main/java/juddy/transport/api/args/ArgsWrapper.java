package juddy.transport.api.args;

import juddy.transport.api.dto.ArrayApiCallArguments;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.api.dto.StringApiCallArguments;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Класс объекта  для агрументов вызова API (межсервисного и внутрисервисного)
 *
 * @author Филипп Ганичев
 */
@Data
@EqualsAndHashCode
public final class ArgsWrapper {

    private ApiCallArguments apiCallArguments;
    private CallInfo<?> callInfo;
    private String correlationId;
    private Exception exception;

    private ArgsWrapper(ApiCallArguments apiCallArguments, CallInfo<?> callInfo) {
        this.apiCallArguments = apiCallArguments;
        this.callInfo = callInfo;
    }

    private ArgsWrapper(ApiCallArguments apiCallArguments) {
        this.apiCallArguments = apiCallArguments;
    }

    private ArgsWrapper(Exception e) {
        this.exception = e;
    }

    public static ArgsWrapper of(String arg) {
        return new ArgsWrapper(new StringApiCallArguments(arg), null);
    }

    public static ArgsWrapper of(ApiCallArguments apiCallArguments) {
        return new ArgsWrapper(apiCallArguments);
    }

    public static ArgsWrapper of(Exception e) {
        return new ArgsWrapper(e);
    }

    public static <T> ArgsWrapper of(T value) {
        return new ArgsWrapper(new ObjectApiCallArguments<T>(value));
    }

    public static <T> ArgsWrapper of(T[] value) {
        return new ArgsWrapper(new ArrayApiCallArguments(value));
    }

    @SuppressWarnings("checkstyle:hiddenField")
    public ArgsWrapper withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
}
