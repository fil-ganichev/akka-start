package org.lokrusta.prototypes.connect.api;

public interface ProxiedStage {

    <T> T getProxy(Class<T> clazz);
}
