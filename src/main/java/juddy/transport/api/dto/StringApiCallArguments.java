package juddy.transport.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class StringApiCallArguments extends ObjectApiCallArguments<String> {

    private String value;

    @Override
    public Object getResult() {
        return value;
    }

    @Override
    @JsonIgnore
    public Type getArgsType() {
        return Type.STRING;
    }
}
