package org.lokrusta.prototypes.connect.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import org.lokrusta.prototypes.connect.api.dto.ArrayApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.ObjectApiCallArguments;
import org.lokrusta.prototypes.connect.api.dto.StringApiCallArguments;

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
    static enum Type {
        OBJECT(0),
        STRING(1),
        ARRAY(2);

        int type;
    }
}
