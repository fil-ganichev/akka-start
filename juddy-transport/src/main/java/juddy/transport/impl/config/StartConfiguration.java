package juddy.transport.impl.config;

import juddy.transport.impl.common.ApiSerializer;
import juddy.transport.impl.context.ApiEngineContextProvider;
import juddy.transport.impl.engine.ApiEngineFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartConfiguration {

    @Bean
    public ApiEngineContextProvider apiEngineContextProvider() {
        return new ApiEngineContextProvider();
    }

    @Bean
    public ApiEngineFactory apiEngineFactory() {
        return new ApiEngineFactory();
    }

    @Bean
    public ApiSerializer apiSerializer() {
        return new ApiSerializer();
    }
}
