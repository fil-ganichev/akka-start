package juddy.transport.api;

import juddy.transport.api.common.Api;

@Api
public interface TestApiFioEnricher {

    default String enrichFio(String fio) {
        return "ФИО: ".concat(fio);
    }
}
