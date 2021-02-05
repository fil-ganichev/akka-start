package juddy.transport.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import juddy.transport.api.args.ApiCallArguments;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ObjectApiCallArguments<T> implements ApiCallArguments {

    private T value;

    @Override
    public Object getResult() {
        return value;
    }

    @Override
    @JsonIgnore
    public Type getArgsType() {
        return Type.OBJECT;
    }
}
