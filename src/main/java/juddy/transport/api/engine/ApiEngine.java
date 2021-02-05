package juddy.transport.api.engine;

/**
 * Интерфейс машины выполнения (соответствует Graph в akka) akka-коннектора
 *
 * @author Филипп Ганичев
 */
public interface ApiEngine {

    ApiEngine run();
}
