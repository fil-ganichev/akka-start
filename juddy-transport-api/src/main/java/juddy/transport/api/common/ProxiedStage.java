package juddy.transport.api.common;

public interface ProxiedStage {

    <T> T getProxy(Class<T> clazz);
}
