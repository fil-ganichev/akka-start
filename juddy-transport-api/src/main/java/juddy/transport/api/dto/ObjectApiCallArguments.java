package juddy.transport.api.dto;

import juddy.transport.api.args.ApiCallArguments;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ObjectApiCallArguments<T> implements ApiCallArguments {

    protected T value;

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
