package juddy.transport.api;

@ApiBean(TestApiPersonFio.class)
public class TestApiPersonFioServer implements TestApiPersonFio {

    private static char SPACE_DELIMITER = ' ';

    @Override
    public String getFio(String firstName, String lastName) {
        return firstName + SPACE_DELIMITER + lastName;
    }
}
