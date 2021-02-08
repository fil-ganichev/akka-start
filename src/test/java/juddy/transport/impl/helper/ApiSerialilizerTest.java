package juddy.transport.impl.helper;

import com.fasterxml.jackson.core.type.TypeReference;
import juddy.transport.api.TestApi;
import juddy.transport.api.TestApiCars;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.impl.args.ArgsWrapperImpl;
import juddy.transport.impl.args.Message;
import juddy.transport.impl.common.ApiSerialilizer;
import juddy.transport.impl.helper.dto.Car;
import juddy.transport.impl.helper.dto.Document;
import juddy.transport.impl.helper.dto.Driver;
import juddy.transport.impl.helper.dto.Model;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSerialilizerTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ApiSerialilizer apiSerialilizer = new ApiSerialilizer();

    @Test
    void when_serializeArgsWrapper_then_ok() throws NoSuchMethodException, UnsupportedEncodingException {
        ArgsWrapperImpl argsWrapper = argsWrapperSimple(new Object[]{"Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев"});
        String serialized = apiSerialilizer.serializeArgs(argsWrapper);
        ArgsWrapper fromSerialized = apiSerialilizer.parameterFromBase64String(serialized);
        assertThat(fromSerialized).isEqualTo(argsWrapper);
    }

    @Test
    void when_resultWithObjectAsArray_then_ok() throws IOException, URISyntaxException, NoSuchMethodException {
        String messageStr = getMessageJson("01");
        Message message = apiSerialilizer.messageFromString(messageStr);
        ArgsWrapper argsWrapper = apiSerialilizer.parameterFromBase64String(message.getBase64Json());
        assertThat((List) argsWrapper.getApiCallArguments().getResult())
                .containsExactly("Москва", "Минск", "Киев", "Таллин", "Рига", "Кишинев");
    }

    @Test
    void when_serializeArgsWrapperWithArrayOfObjects_then_ok() throws NoSuchMethodException, UnsupportedEncodingException {
        apiSerialilizer.registerApiTypes("juddy.transport.api.TestApiCars.registerCar",
                Arrays.asList(
                        new TypeReference<Car>() {
                        },
                        new TypeReference<List<Driver>>() {
                        }));
        ArgsWrapperImpl argsWrapper = argsWrapperComplexType(arrayOfObjects());
        String serialized = apiSerialilizer.serializeArgs(argsWrapper);
        ArgsWrapper fromSerialized = apiSerialilizer.parameterFromBase64String(serialized);
        assertThat(fromSerialized).isEqualTo(argsWrapper);
    }

    private String getMessageJson(String testNum) throws URISyntaxException, IOException {
        Path testFile = Paths.get(ClassLoader.getSystemResource(String.format("apiHelperTest/case%s.json", testNum)).toURI());
        return new String(Files.readAllBytes(testFile), Charset.forName("utf-8"));
    }

    private Object[] arrayOfObjects() {
        Car car = Car.builder()
                .regNumber("A234AA99")
                .model(Model.builder()
                        .markName("MERCEDES BENZ")
                        .modelName("E63").build())
                .build();
        Driver mainDriver = Driver.builder()
                .fio("Иванов Андрей Сергеевич")
                .documents(Arrays.asList(Document.builder().number("22225555").serial("3333").build(),
                        Document.builder().number("23637482").serial("ПР90").build()))
                .build();
        Driver additionalDriver = Driver.builder()
                .fio("Черников Петр Викторович")
                .documents(Arrays.asList(Document.builder().number("32462424").serial("1111").build(),
                        Document.builder().number("743738953").serial("СА89").build()))
                .build();
        return new Object[]{car, Arrays.asList(mainDriver, additionalDriver)};
    }

    private ArgsWrapperImpl argsWrapperSimple(Object[] arg) {
        ArgsWrapperImpl argsWrapper = ArgsWrapperImpl.of(arg)
                .withCorrelationId("1322ab78-abd6-4015-b59d-230ffabe4817");
        argsWrapper.setCallInfo(CallInfo.<TestApi>builder()
                .apiClass(TestApi.class)
                .apiMethod(TestApi.class.getMethods()[0])
                .build());
        return argsWrapper;
    }

    private ArgsWrapperImpl argsWrapperComplexType(Object[] args) {
        ArgsWrapperImpl argsWrapper = ArgsWrapperImpl.of(args)
                .withCorrelationId("1322ab78-abd6-4015-b59d-230ffabe4817");
        argsWrapper.setCallInfo(CallInfo.<TestApiCars>builder()
                .apiClass(TestApiCars.class)
                .apiMethod(TestApiCars.class.getMethods()[0])
                .build());
        return argsWrapper;
    }
}
