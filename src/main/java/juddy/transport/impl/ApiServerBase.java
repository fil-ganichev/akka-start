package juddy.transport.impl;

import juddy.transport.api.*;
import juddy.transport.api.dto.ArrayApiCallArguments;
import juddy.transport.api.dto.ObjectApiCallArguments;
import juddy.transport.impl.error.ApiCallException;
import juddy.transport.impl.error.CallPointNotFoundException;
import juddy.transport.impl.error.IllegalCallPointException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter(AccessLevel.PROTECTED)
public abstract class ApiServerBase extends StageBase implements ApiServer, ApplicationContextAware {

    private final Map<Class<?>, CallPoint> points;

    private ApplicationContext applicationContext;

    protected ApiServerBase(Map<Class<?>, CallPoint> points) {
        this.points = points;
        setArgsConverter(new DefaultApiServerArgsConverter());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected abstract <T> Method findCallingMethod(ArgsWrapper argsWrapper, CallPoint<T> callPoint) throws NoSuchMethodException;

    protected <T> ArgsWrapper call(ArgsWrapper argsWrapper) {
        try {
            Class<T> apiClass = argsWrapper.getCallInfo().getApiClass();
            CallPoint<T> callPoint = points.get(apiClass);
            Method method = findCallingMethod(argsWrapper, callPoint);
            Object bean = callPoint.getApiServerImpl();
            Class<?> parameterTypes[] = method.getParameterTypes();
            ApiCallArguments apiCallArguments = argsWrapper.getApiCallArguments();
            if (parameterTypes.length == 0) {
                throw new IllegalCallPointException();
            } else if (parameterTypes.length == 1) {
                if (apiCallArguments instanceof ObjectApiCallArguments) {
                    Object result = method.invoke(bean, ((ObjectApiCallArguments) apiCallArguments).getValue());
                    return ArgsWrapperImpl.of(result).withCorrelationId(argsWrapper.getCorrelationId());
                }
            }
            Object result;
            if (apiCallArguments instanceof ArrayApiCallArguments) {
                result = method.invoke(bean, ((ArrayApiCallArguments) apiCallArguments).getValues());
            } else {
                Object parameters = apiCallArguments instanceof ObjectApiCallArguments
                        ? ((ObjectApiCallArguments) apiCallArguments).getValue()
                        : apiCallArguments;
                Method apiMethod = callPoint.getApi().getMethod(method.getName(), method.getParameterTypes());
                List args = prepareArgValues(apiMethod, parameters);
                result = method.invoke(bean, args.toArray(new Object[args.size()]));
            }
            return ArgsWrapperImpl.of(result).withCorrelationId(argsWrapper.getCorrelationId());
        } catch (Exception e) {
            Exception cause = e.getCause() != null && e.getCause() instanceof Exception && e instanceof InvocationTargetException
                    ? (Exception) e.getCause()
                    : e;
            return ArgsWrapperImpl.of(cause).withCorrelationId(argsWrapper.getCorrelationId());
        }
    }

    private List prepareArgValues(Method method, Object parameters) {
        return Arrays.stream(method.getParameterAnnotations())
                .map(annotattions -> Arrays.stream(annotattions)
                        .filter(annotattion -> annotattion.annotationType().equals(ApiArg.class))
                        .findFirst()
                        .orElseThrow())
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

    protected static Map<Class<?>, CallPoint> apiToCallPoints(List<Class<?>> apiInterfaces) {
        return apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz).build()));
    }

    protected static Map<Class<?>, CallPoint> apiToCallPoints(Map<Class<?>, Object> api) {
        return api.keySet().stream().collect(Collectors.toList())
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz)
                                .apiServerImpl(api.get(clazz)).build()));
    }

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

    protected class DefaultApiServerArgsConverter implements Function<ArgsWrapper, ArgsWrapper> {

        @Override
        public ArgsWrapper apply(ArgsWrapper argsWrapper) {
            if (argsWrapper.getCallInfo() == null) {
                if (points.size() == 1) {
                    CallPoint callPoint = points.values().stream().findFirst().orElseThrow();
                    if (callPoint.getMethods().size() == 1) {
                        ((ArgsWrapperImpl) argsWrapper).setCallInfo(CallInfo.builder()
                                .apiClass(callPoint.getApi())
                                .apiMethod((Method) callPoint.getMethods().stream().findFirst().orElseThrow())
                                .build());
                    }
                }
            }
            if (argsWrapper.getCallInfo() == null) {
                throw new CallPointNotFoundException();
            }
            return argsWrapper;
        }
    }
}
