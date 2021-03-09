package juddy.transport.impl.server;

import juddy.transport.api.args.ApiArg;
import juddy.transport.api.args.ApiCallArguments;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.args.CallInfo;
import juddy.transport.api.dto.ArrayApiCallArguments;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.api.server.ApiServer;
import juddy.transport.impl.common.StageBase;
import juddy.transport.impl.error.ApiCallException;
import juddy.transport.impl.error.ApiException;
import juddy.transport.impl.error.CallPointNotFoundException;
import juddy.transport.impl.error.IllegalCallPointException;
import lombok.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Getter(AccessLevel.PROTECTED)
public abstract class ApiServerBase extends StageBase implements ApiServer, ApplicationContextAware {

    private final Map<Class<?>, CallPoint<?>> points;

    private ApplicationContext applicationContext;

    protected ApiServerBase(Map<Class<?>, CallPoint<?>> points) {
        this.points = points;
        setArgsConverter(new DefaultApiServerArgsConverter());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected abstract <T> Method findCallingMethod(ArgsWrapper argsWrapper, CallPoint<T> callPoint)
            throws NoSuchMethodException;

    protected <T> ArgsWrapper call(ArgsWrapper argsWrapper) {
        try {
            Class<T> apiClass = ((CallInfo<T>) argsWrapper.getCallInfo()).getApiClass();
            CallPoint<T> callPoint = (CallPoint<T>) points.get(apiClass);
            Method method = findCallingMethod(argsWrapper, callPoint);
            Object bean = callPoint.getApiServerImpl();
            Class<?>[] parameterTypes = method.getParameterTypes();
            ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
            if (parameterTypes.length == 0) {
                throw new IllegalCallPointException();
            } else if (parameterTypes.length == 1 && apiCallArguments instanceof ObjectApiCallArguments) {
                Object result = method.invoke(bean, ((ObjectApiCallArguments) apiCallArguments).getValue());
                return ArgsWrapper.of(result).copyDataFrom(argsWrapper);
            }
            Object result;
            if (apiCallArguments instanceof ArrayApiCallArguments) {
                result = method.invoke(bean, ((ArrayApiCallArguments) apiCallArguments).getValues());
            } else {
                Object parameters = apiCallArguments instanceof ObjectApiCallArguments
                        ? ((ObjectApiCallArguments) apiCallArguments).getValue()
                        : apiCallArguments;
                Method apiMethod = callPoint.getApi().getMethod(method.getName(), method.getParameterTypes());
                List<?> args = prepareArgValues(apiMethod, parameters);
                result = method.invoke(bean, args.toArray(new Object[0]));
            }
            return ArgsWrapper.of(result).copyDataFrom(argsWrapper);
        } catch (InvocationTargetException e) {
            Exception cause = e.getCause() instanceof Exception
                    ? (Exception) e.getCause()
                    : e;
            return ArgsWrapper.of(cause).copyDataFrom(argsWrapper);
        } catch (Exception e) {
            return ArgsWrapper.of(e).copyDataFrom(argsWrapper);
        }
    }

    private List<Object> prepareArgValues(Method method, Object parameters) {
        return Arrays.stream(method.getParameterAnnotations())
                .map(annotations -> Arrays.stream(annotations)
                        .filter(annotation -> annotation.annotationType().equals(ApiArg.class))
                        .findFirst()
                        .orElseThrow(() -> new ApiCallException(
                                String.format("Annotation @ApiArg not found on parameters of method %s",
                                        method.getName()))))
                .map(annotation -> findValue(((ApiArg) annotation).value(), parameters))
                .collect(Collectors.toList());
    }

    private Object findValue(String parameterName, Object parameters) {
        try {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(parameterName, parameters.getClass());
            return propertyDescriptor.getReadMethod().invoke(parameters);
        } catch (Exception e) {
            throw new ApiCallException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        points.forEach((key, point) -> initCallPoint(point));
    }

    protected abstract <T> void initCallPoint(CallPoint<T> point);

    @Data
    @Builder
    protected static class CallPoint<T> {
        //Интерфейс
        private Class<T> api;
        //Генерируемая имплементация интерфейса
        private T apiImpl;
        //Экземпляр класса бина-обработчика
        private Object apiServerImpl;
        private Set<Method> methods;
    }

    protected class DefaultApiServerArgsConverter implements UnaryOperator<ArgsWrapper> {

        @SneakyThrows
        @Override
        public ArgsWrapper apply(ArgsWrapper argsWrapper) {
            if (argsWrapper.getCallInfo() == null && points.size() == 1) {
                CallPoint<?> callPoint = points.values().stream().findFirst().orElseThrow(
                        () -> new ApiException("Call points list is empty"));
                if (callPoint.getMethods().size() == 1) {
                    argsWrapper.setCallInfo(CallInfo.builder()
                            .apiClass((Class<Object>) callPoint.getApi())
                            .apiMethod(callPoint.getMethods().stream()
                                    .findFirst()
                                    .orElseThrow(() -> new ApiException("Method list is empty in call point")))
                            .build());
                }
            }
            if (argsWrapper.getCallInfo() == null) {
                throw new CallPointNotFoundException();
            }
            return argsWrapper;
        }
    }
}
