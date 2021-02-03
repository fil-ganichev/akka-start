package juddy.transport.impl.helper.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    private String fio;
    private List<Document> documents;
}
