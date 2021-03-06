package juddy.transport.api;

import juddy.transport.api.args.ApiArg;
import juddy.transport.api.common.Api;
import lombok.Data;

@Api
public interface TestApiPersonFio {

    String getFio(@ApiArg("firstName") String firstName, @ApiArg("lastName") String lastName);

    @Data
    class Person {
        private String firstName;
        private String lastName;
        private String middleName;
    }
}
