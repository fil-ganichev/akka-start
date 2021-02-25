package juddy.transport.utils;

import juddy.transport.api.TestApiGenderPerson;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.error.ApiCallException;
import juddy.transport.impl.source.JsonFileSource;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

public class GenderPersonSource extends JsonFileSource<TestApiGenderPerson.Person> {

    public GenderPersonSource(Path filePath, Class<TestApiGenderPerson.Person> objectClass) {
        super(filePath, objectClass);
    }

    @Override
    public UnaryOperator<ArgsWrapper> getArgsConverter() {
        return argsWrapper -> {
            ArgsWrapper jsonArgsWrapper = super.getArgsConverter().apply(argsWrapper);
            CallInfo<TestApiGenderPerson> callInfo = CallInfo.<TestApiGenderPerson>builder()
                    .apiClass(TestApiGenderPerson.class)
                    .apiMethod(getMethod(jsonArgsWrapper))
                    .build();
            jsonArgsWrapper.setCallInfo(callInfo);
            return jsonArgsWrapper;
        };
    }

    private Method getMethod(ArgsWrapper jsonArgsWrapper) {
        try {
            ObjectApiCallArguments<TestApiGenderPerson.Person> parameter =
                    (ObjectApiCallArguments<TestApiGenderPerson.Person>) jsonArgsWrapper.getApiCallArguments();
            TestApiGenderPerson.Person person = parameter.getValue();
            if (person.getGender() == TestApiGenderPerson.Gender.MALE) {
                return TestApiGenderPerson.class.getMethod("getMaleFio", TestApiGenderPerson.Person.class);
            } else {
                return TestApiGenderPerson.class.getMethod("getFeMaleFio", TestApiGenderPerson.Person.class);
            }
        } catch (NoSuchMethodException e) {
            throw new ApiCallException(e);
        }
    }
}
