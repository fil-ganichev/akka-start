package juddy.transport.api;

public interface ProxiedStage {

    <T> T getProxy(Class<T> clazz);
}
