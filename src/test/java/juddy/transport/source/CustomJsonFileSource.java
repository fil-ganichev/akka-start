package juddy.transport.source;

import juddy.transport.api.TestApiGenderPerson;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.error.ApiCallException;
import juddy.transport.impl.source.JsonFileSource;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.function.Function;

public class CustomJsonFileSource<T> extends JsonFileSource<T> {

    public CustomJsonFileSource(Path filePath, Class<T> objectClass) {
        super(filePath, objectClass);
    }

    @Override
    public Function<ArgsWrapper, ArgsWrapper> getArgsConverter() {
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
            ObjectApiCallArguments parameter = (ObjectApiCallArguments) jsonArgsWrapper.getApiCallArguments();
            TestApiGenderPerson.Person person = (TestApiGenderPerson.Person) parameter.getValue();
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
