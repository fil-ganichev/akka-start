package juddy.transport.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@Builder
public class CallInfo<T> {

    private Class<T> apiClass;
    private Method apiMethod;
}
