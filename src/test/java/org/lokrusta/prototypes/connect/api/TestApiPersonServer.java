package org.lokrusta.prototypes.connect.api;

@ApiBean(TestApiPerson.class)
public class TestApiPersonServer implements TestApiPerson {

    private static char SPACE_DELIMITER = ' ';

    @Override
    public String getFio(Person person) {
        return person.getFirstName()
                + SPACE_DELIMITER
                + person.getLastName()
                + SPACE_DELIMITER
                + person.getMiddleName();
    }
}
