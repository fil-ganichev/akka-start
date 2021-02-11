package juddy.transport.impl.serializer.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    String serial;
    String number;
}
