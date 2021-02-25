package juddy.transport.impl.context;

import akka.actor.ActorSystem;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiEngineContext {

    private ActorSystem actorSystem;
}
