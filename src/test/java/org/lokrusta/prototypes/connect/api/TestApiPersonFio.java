package org.lokrusta.prototypes.connect.api;

import lombok.Data;

@Api
public interface TestApiPersonFio {

    String getFio(@ApiArg("firstName") String firstName, @ApiArg("lastName") String lastName);

    @Data
    public static class Person {
        private String firstName;
        private String lastName;
        private String middleName;
    }
}
