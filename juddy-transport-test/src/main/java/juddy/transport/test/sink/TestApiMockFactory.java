package juddy.transport.test.sink;

import static org.mockito.Mockito.spy;

public final class TestApiMockFactory {

    private TestApiMockFactory() {
    }

    public static TestApiMock mockServer() {
        return spy(new TestApiMock() {
        });
    }
}
