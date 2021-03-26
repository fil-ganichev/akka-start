package juddy.transport.impl.server;

import juddy.transport.api.TestApi;
import juddy.transport.api.TestApiGenderPerson;
import juddy.transport.api.TestApiPerson;
import juddy.transport.config.server.ApiServerTestAutoConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.impl.test.source.FileSourceHelper;
import juddy.transport.impl.test.source.JsonFileSourceHelper;
import juddy.transport.test.sink.TestApiSink;
import juddy.transport.test.sink.TestApiSinkServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static juddy.transport.common.Constants.API_TIMEOUT_MS;
import static juddy.transport.common.Constants.RPC_SYNC_TIMEOUT_MS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SuppressWarnings({"checkstyle:methodName", "checkstyle:throwsCount"})
@SpringJUnitConfig(ApiServerTestAutoConfiguration.class)
class ApiServerAutoConfigTest {

    private static final char SPACE_DELIMITER = ' ';

    @Autowired
    private ApiEngineImpl apiEngine;
    @Autowired
    private ApiEngineImpl apiEngineFromSource;
    @Autowired
    private ApiEngineImpl apiEngineFromSourceTwoPhases;
    @Autowired
    private ApiEngineImpl apiEngineFromJsonSource;
    @Autowired
    private ApiEngineImpl apiEngineFromJsonSourceWithMultiplyArguments;
    @Autowired
    private ApiEngineImpl apiEngineFromSourceWithMultiplyApi;
    @Autowired
    private FileSourceHelper fileSourceHelper;
    @Autowired
    private JsonFileSourceHelper<TestApiPerson.Person> jsonFileSourceHelper;
    @Autowired
    private JsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper;

    // Вызываем сервер явно, получаем результат
    @Test
    void when_callApiServer_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        TestApi testApi = apiEngine.findProxy(TestApi.class);
        List<String> cities = testApi.split("Москва, Минск, Киев, Таллин, Рига, Кишинев")
                .get(RPC_SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertThat(cities).containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }

    // Получаем строки из TestSource, преобразуем из в вызов единственного метода API, проверяем результат
    @Test
    void when_readFileSourceAndRunServerApi_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSource.findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSource.run();
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(fileSourceHelper.getValues().size()));
        testApiSinkServer.check(fileSourceHelper.getValues().toArray(
                new String[0]));
    }

    // Получаем строки, преобразуем из в вызов единственного метода API, далее еще один вызов API, проверяем результат
    @Test
    void when_readFileSourceAndRunServerApiAndNextOne_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSourceTwoPhases
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSourceTwoPhases.run();
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(fileSourceHelper.getValues().size()));
        testApiSinkServer.check(fileSourceHelper
                .getValues()
                .stream()
                .map(source -> Arrays.stream(source.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList())
                .toArray(new List[fileSourceHelper
                        .getValues().size()]));
    }

    // Получаем строки, преобразуем из в вызов единственного метода API, с параметром объектом json из строки,
    // далее еще один вызов API, проверяем результат
    @Test
    void when_readJsonFileSourceAndRunServerApiAndNextOne_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromJsonSource
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromJsonSource.run();
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(jsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(jsonFileSourceHelper
                .getValues()
                .stream()
                .map(this::getFio)
                .collect(Collectors.toList())
                .toArray(new String[jsonFileSourceHelper.getValues().size()]));
    }

    // Получаем строки, преобразуем в json-объект, далее  вызов единственного метода API
    // с параметрами=полям объекта json, далее еще один вызов API, проверяем результат
    @Test
    void when_readJsonFileSourceAndRunServerApiWithSeveralArgumentsAndNextOne_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromJsonSourceWithMultiplyArguments
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromJsonSourceWithMultiplyArguments.run();
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(jsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(jsonFileSourceHelper
                .getValues()
                .stream()
                .map(this::getShortFio)
                .collect(Collectors.toList())
                .toArray(new String[jsonFileSourceHelper.getValues().size()]));
    }

    @Test
    void when_readJsonFileSourceAndRunServerWithMultiplyApi_then_ok() {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSourceWithMultiplyApi
                .findServerBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSourceWithMultiplyApi.run();
        await().atMost(API_TIMEOUT_MS, TimeUnit.MILLISECONDS).until(
                testApiSinkServer.processed(customJsonFileSourceHelper.getValues().size()));
        testApiSinkServer.check(customJsonFileSourceHelper
                .getValues()
                .stream()
                .map(this::getFio)
                .collect(Collectors.toList())
                .toArray(new String[customJsonFileSourceHelper.getValues().size()]));
    }

    private String getFio(TestApiPerson.Person person) {
        return person.getFirstName()
                + SPACE_DELIMITER
                + person.getLastName()
                + SPACE_DELIMITER
                + person.getMiddleName();
    }

    private String getFio(TestApiGenderPerson.Person person) {
        return (person.getGender() == TestApiGenderPerson.Gender.MALE
                ? "Господин "
                : "Госпожа ")
                + person.getFirstName()
                + SPACE_DELIMITER
                + person.getLastName()
                + SPACE_DELIMITER
                + person.getMiddleName();
    }

    private String getShortFio(TestApiPerson.Person person) {
        return person.getFirstName()
                + SPACE_DELIMITER
                + person.getLastName();
    }
}
