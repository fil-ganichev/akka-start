package juddy.transport.impl.server;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Flow;
import juddy.transport.api.args.ArgsWrapper;
import juddy.transport.api.common.ApiBean;
import juddy.transport.api.server.ApiServer;
import juddy.transport.impl.error.CallPointNotFoundException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class ApiServerImpl extends ApiServerBase implements ApiServer {

    protected ApiServerImpl(Map<Class<?>, CallPoint<?>> points) {
        super(points);
    }

    public static ApiServerImpl of(List<Class<?>> apiInterfaces) {
        return new ApiServerImpl(apiInterfaces
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz).build())));
    }

    public static ApiServerImpl of(Map<Class<?>, Object> api) {
        return new ApiServerImpl(new ArrayList<>(api.keySet())
                .stream()
                .collect(Collectors.toMap(clazz -> clazz,
                        clazz -> CallPoint.builder().api((Class<Object>) clazz)
                                .apiServerImpl(api.get(clazz)).build())));
    }

    @Override
    protected Flow<ArgsWrapper, ArgsWrapper, NotUsed> createConnector() {
        return Flow.of(ArgsWrapper.class)
                .log(logTitle("juddy.transport.api call arguments"))
                .map(this::next)
                .map(this::call)
                .log(logTitle("juddy.transport.api call result"))
                .map(this::checkError)
                .mapError(new PFBuilder<Throwable, Throwable>()
                        .match(Exception.class, this::onError)
                        .build());
    }

    public Object getBean(Class<?> clazz) {
        CallPoint<?> callPoint = getPoints().get(clazz);
        return callPoint == null
                ? null
                : callPoint.getApiServerImpl();
    }

    private <T> T findBean(Class<T> clazz, Set<String> candidates) {
        Map<String, T> beans = getApplicationContext().getBeansOfType(clazz);
        Set<String> beanNames = beans.keySet();
        beanNames.retainAll(candidates);
        if (beanNames.isEmpty()) {
            throw new CallPointNotFoundException(String.format("No beans of type %s annotated by %s found",
                    clazz.getName(), ApiBean.class.getName()));
        } else if (beanNames.size() > 1) {
            throw new CallPointNotFoundException(String.format("Too many beans of type %s annotated by %s found",
                    clazz.getName(), ApiBean.class.getName()));
        }
        return (T) getApplicationContext().getBean(String.valueOf(beanNames.toArray()[0]));
    }

    @Override
    protected <T> void initCallPoint(CallPoint<T> point) {
        if (point.getApiServerImpl() == null) {
            Set<String> beanNameCandidates = new HashSet<>(Arrays.asList(
                    getApplicationContext().getBeanNamesForAnnotation(ApiBean.class)));
            point.setApiServerImpl(findBean(point.getApi(), beanNameCandidates));
        }
        Set<Method> methods = new HashSet<>(Arrays.asList(point.getApi().getMethods()));
        point.setMethods(methods);
    }

    @Override
    protected <T> Method findCallingMethod(ArgsWrapper argsWrapper, CallPoint<T> callPoint)
            throws NoSuchMethodException {
        Method method = argsWrapper.getCallInfo().getApiMethod();
        if (Modifier.isAbstract(method.getModifiers())) {
            method = callPoint.getApiServerImpl().getClass().getMethod(method.getName(), method.getParameterTypes());
        }
        return method;
    }
}
