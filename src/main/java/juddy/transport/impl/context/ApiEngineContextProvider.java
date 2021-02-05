package juddy.transport.impl.context;

import akka.actor.ActorSystem;
import org.springframework.stereotype.Component;

import static juddy.transport.api.common.CommonConstants.JUDDY_TRANSPORT;

@Component
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
        return ApiEngineContext.builder().actorSystem(ActorSystem.create(JUDDY_TRANSPORT)).build();
    }
}
