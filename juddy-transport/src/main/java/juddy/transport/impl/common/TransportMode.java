package juddy.transport.impl.common;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TransportMode {

    API_CALL(0),
    RESULT(1),
    RESULT_AS_SOURCE(2);

    private int mode;
}
