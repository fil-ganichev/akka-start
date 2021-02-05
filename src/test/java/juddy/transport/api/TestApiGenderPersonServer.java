package juddy.transport.api;

import juddy.transport.api.common.ApiBean;

@ApiBean(TestApiGenderPerson.class)
public class TestApiGenderPersonServer implements TestApiGenderPerson {

    private static char SPACE_DELIMITER = ' ';

    public String getMaleFio(TestApiGenderPersonServer.Person person) {
        return "Господин " + person.getFirstName() + SPACE_DELIMITER + person.getLastName() + SPACE_DELIMITER + person.getMiddleName();
    }

    public String getFeMaleFio(TestApiGenderPersonServer.Person person) {
        return "Госпожа " + person.getFirstName() + SPACE_DELIMITER + person.getLastName() + SPACE_DELIMITER + person.getMiddleName();
    }
}
