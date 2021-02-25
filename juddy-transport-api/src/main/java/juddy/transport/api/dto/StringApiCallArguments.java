package juddy.transport.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StringApiCallArguments extends ObjectApiCallArguments<String> {

    public StringApiCallArguments(String value) {
        super(value);
    }

    @Override
    @JsonIgnore
    public Type getArgsType() {
        return Type.STRING;
    }
}
