package juddy.transport.impl.context;

import akka.actor.ActorSystem;

import static juddy.transport.api.common.CommonConstants.JUDDY_TRANSPORT;

public class ApiEngineContextProvider {

    private volatile ApiEngineContext apiEngineContext;

    public ApiEngineContext getApiEngineContext() {
        if (apiEngineContext == null) {
            synchronized (this) {
                if (apiEngineContext == null) {
                    apiEngineContext = createApiEngineContext();
                }
            }
        }
        return apiEngineContext;
    }

    private ApiEngineContext createApiEngineContext() {
        return ApiEngineContext.builder().actorSystem(ActorSystem.create(JUDDY_TRANSPORT)).build();
    }
}
