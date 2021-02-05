package juddy.transport.impl.server;

import juddy.transport.api.*;
import juddy.transport.config.server.ApiServerTestAutoConfiguration;
import juddy.transport.impl.engine.ApiEngineImpl;
import juddy.transport.utils.CustomJsonFileSourceHelper;
import juddy.transport.utils.FileSourceHelper;
import juddy.transport.utils.JsonFileSourceHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
    private CustomJsonFileSourceHelper<TestApiGenderPerson.Person> customJsonFileSourceHelper;

    // Вызываем сервер явно, получаем результат
    @Test
    void when_callApiServer_then_ok() throws ExecutionException, InterruptedException, TimeoutException {
        TestApi testApi = apiEngine.findProxy(TestApi.class);
        List<String> cities = testApi.split("Москва, Минск, Киев, Таллин, Рига, Кишинев").get(500, TimeUnit.MILLISECONDS);
        assertThat(cities).containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }

    // Получаем строки из TestSource, преобразуем из в вызов единственного метода API, проверяем результат
    @Test
    void when_readFileSourceAndRunServerApi_then_ok() throws InterruptedException {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSource.getBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSource.run();
        Thread.sleep(1000);
        testApiSinkServer.check(fileSourceHelper.getValues().toArray(new String[fileSourceHelper.getValues().size()]));
    }

    // Получаем строки, преобразуем из в вызов единственного метода API, далее еще один вызов API, проверяем результат
    @Test
    void when_readFileSourceAndRunServerApiAndNextOne_then_ok() throws InterruptedException {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSourceTwoPhases.getBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSourceTwoPhases.run();
        Thread.sleep(1000);
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

    // Получаем строки, преобразуем из в вызов единственного метода API, с параметром объектом json из строки, далее еще один вызов API, проверяем результат
    @Test
    void when_readJsonFileSourceAndRunServerApiAndNextOne_then_ok() throws InterruptedException {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromJsonSource.getBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromJsonSource.run();
        Thread.sleep(1000);
        testApiSinkServer.check(jsonFileSourceHelper
                .getValues()
                .stream()
                .map(this::getFio)
                .collect(Collectors.toList())
                .toArray(new String[jsonFileSourceHelper.getValues().size()]));
    }

    // Получаем строки, преобразуем в json-объект, далее  вызов единственного метода API, с параметрами=полям объекта json, далее еще один вызов API, проверяем результат
    @Test
    void when_readJsonFileSourceAndRunServerApiWithSeveralArgumentsAndNextOne_then_ok() throws InterruptedException {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromJsonSourceWithMultiplyArguments.getBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromJsonSourceWithMultiplyArguments.run();
        Thread.sleep(1000);
        testApiSinkServer.check(jsonFileSourceHelper
                .getValues()
                .stream()
                .map(this::getShortFio)
                .collect(Collectors.toList())
                .toArray(new String[jsonFileSourceHelper.getValues().size()]));
    }

    @Test
    void when_readJsonFileSourceAndRunServerWithMultiplyApi_then_ok() throws InterruptedException {
        TestApiSinkServer testApiSinkServer = (TestApiSinkServer) apiEngineFromSourceWithMultiplyApi.getBean(TestApiSink.class);
        testApiSinkServer.reset();
        apiEngineFromSourceWithMultiplyApi.run();
        Thread.sleep(1000);
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

    private String getShortFio(TestApiPerson.Person person) {
        return person.getFirstName()
                + SPACE_DELIMITER
                + person.getLastName();
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
}
