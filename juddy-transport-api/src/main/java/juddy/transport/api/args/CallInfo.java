package juddy.transport.api.args;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@Builder
public class CallInfo<T> {

    private Class<T> apiClass;
    private Method apiMethod;
}
