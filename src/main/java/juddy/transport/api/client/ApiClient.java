package juddy.transport.api.client;

import juddy.transport.api.common.Stage;
import juddy.transport.impl.common.ApiCallProcessor;

/**
 * Интерфейс клиентского компонента akka-коннектора
 *
 * @author Филипп Ганичев
 */
public interface ApiClient extends Stage {

    ApiCallProcessor getApiCallProcessor();
}
