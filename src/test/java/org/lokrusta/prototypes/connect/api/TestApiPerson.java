package org.lokrusta.prototypes.connect.api;

import lombok.Data;

@Api
public interface TestApiPerson {

    String getFio(Person person);

    @Data
    public static class Person {
        private String firstName;
        private String lastName;
        private String middleName;
    }
}
