package juddy.transport.api;

/**
 * Интерфейс объекта обертки для агрументов вызова API (межсервисного и внутрисервисного)
 *
 * @author Филипп Ганичев
 */
public interface ArgsWrapper {

    ApiCallArguments getApiCallArguments();

    CallInfo getCallInfo();

    String getCorrelationId();

    void setCorrelationId(String correlationId);

    Exception getException();
}
