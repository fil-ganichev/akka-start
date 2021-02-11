package juddy.transport.api.args;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import juddy.transport.api.dto.ArrayApiCallArguments;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.api.dto.StringApiCallArguments;
import lombok.AllArgsConstructor;

/**
 * Интерфейс объекта инкапсулирующего агрумент вызова API (межсервисного и внутрисервисного)
 *
 * @author Филипп Ганичев
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "argsType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ArrayApiCallArguments.class, name = "ARRAY"),
        @JsonSubTypes.Type(value = StringApiCallArguments.class, name = "STRING"),
        @JsonSubTypes.Type(value = ObjectApiCallArguments.class, name = "OBJECT")
})
public interface ApiCallArguments {

    @JsonIgnore
    Object getResult();

    Type getArgsType();

    @AllArgsConstructor
    enum Type {
        OBJECT(0),
        STRING(1),
        ARRAY(2);

        int type;
    }
}
