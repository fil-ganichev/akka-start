package juddy.transport.impl.context;

import akka.actor.ActorSystem;

public class ApiEngineContextProvider {

    private static volatile ApiEngineContext apiEngineContext;

    public static ApiEngineContext getApiEngineContext() {
        if (apiEngineContext == null) {
            synchronized (ApiEngineContextProvider.class) {
                if (apiEngineContext == null) {
                    apiEngineContext = createApiEngineContext();
                }
            }
        }
        return apiEngineContext;
    }

    private static ApiEngineContext createApiEngineContext() {
        return ApiEngineContext.builder().actorSystem(ActorSystem.create("QuickStart")).build();
    }
}
