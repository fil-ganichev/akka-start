package juddy.transport.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Api
public interface TestApiGenderPerson {

    String getMaleFio(TestApiGenderPerson.Person person);

    String getFeMaleFio(TestApiGenderPerson.Person person);

    @Data
    class Person {
        private String firstName;
        private String lastName;
        private String middleName;
        private Gender gender;
    }

    @AllArgsConstructor
    enum Gender {

        MALE(0),
        FEMALE(1);

        int value;
    }
}
