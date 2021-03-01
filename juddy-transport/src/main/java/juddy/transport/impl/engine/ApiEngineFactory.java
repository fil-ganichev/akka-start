package juddy.transport.impl.engine;

import juddy.transport.impl.client.ApiClientImpl;
import juddy.transport.impl.common.StageBase;
import juddy.transport.impl.common.TransportMode;
import juddy.transport.impl.error.ApiEngineException;
import juddy.transport.impl.net.TcpClientTransportImpl;
import juddy.transport.impl.net.TcpServerTransportImpl;
import juddy.transport.impl.server.ApiProxiedServerImpl;
import juddy.transport.impl.server.ApiServerBase;
import juddy.transport.impl.server.ApiServerImpl;
import juddy.transport.impl.source.ApiSourceImpl;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApiEngineFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private DefaultListableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

    public ApiServerImpl apiServer(Map<Class<?>, Class<?>> api) {
        ApiServerImpl apiServerImpl = ApiServerImpl.of(api
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> registerBean(entry.getValue()))));
        api.values().forEach(this::registerBean);
        registerApiServer(apiServerImpl);
        return apiServerImpl;
    }

    public ApiProxiedServerImpl apiProxiedServer(Map<Class<?>, Class<?>> api) {
        ApiProxiedServerImpl apiProxiedServerImpl = ApiProxiedServerImpl.of(api
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> registerBean(entry.getValue()))));
        api.values().forEach(this::registerBean);
        registerApiServer(apiProxiedServerImpl);
        api.keySet().forEach(clazz -> {
            Object proxyInstance = apiProxiedServerImpl.getProxy(clazz);
            beanFactory.registerSingleton(generateBeanName(proxyInstance.getClass()), proxyInstance);
        });
        return apiProxiedServerImpl;
    }

    public <T> ApiSourceImpl<T> apiSource(ApiSourceImpl<T> apiSource) {
        registerStageBean(apiSource);
        return apiSource;
    }

    public TcpServerTransportImpl tcpServerTransport(String host, int port) {
        TcpServerTransportImpl tcpServerTransport = TcpServerTransportImpl.of(host, port);
        registerStageBean(tcpServerTransport);
        return tcpServerTransport;
    }

    public TcpClientTransportImpl tcpClientTransport(String host, int port) {
        TcpClientTransportImpl tcpClientTransport = TcpClientTransportImpl.of(host, port);
        registerStageBean(tcpClientTransport);
        return tcpClientTransport;
    }

    public TcpClientTransportImpl tcpClientTransport(String host, int port, TransportMode transportMode) {
        TcpClientTransportImpl tcpClientTransport = TcpClientTransportImpl.of(host, port, transportMode);
        registerStageBean(tcpClientTransport);
        return tcpClientTransport;
    }

    public ApiClientImpl apiClient(List<Class<?>> api) {
        ApiClientImpl apiClientImpl = ApiClientImpl.of(api);
        registerStageBean(apiClientImpl);
        api.forEach(clazz -> {
            Object proxyInstance = apiClientImpl.getProxy(clazz);
            beanFactory.registerSingleton(generateBeanName(proxyInstance.getClass()), proxyInstance);
        });
        return apiClientImpl;
    }

    private <T> T registerBean(Class<T> clazz) {
        GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
        genericBeanDefinition.setBeanClass(clazz);
        String beanName = generateBeanName(clazz);
        beanFactory.registerBeanDefinition(beanName, genericBeanDefinition);
        return (T) applicationContext.getBean(beanName);
    }

    private String generateBeanName(Class<?> clazz) {
        String className = clazz.isAnonymousClass()
                ? clazz.getName()
                : clazz.getSimpleName();
        StringBuilder baseName = new StringBuilder(className);
        for (int i = 1; true; i++) {
            baseName.append(i);
            if (!applicationContext.containsBean(baseName.toString())) {
                break;
            }
            baseName.delete(className.length(), baseName.length());
        }
        return baseName.toString();
    }

    private void registerApiServer(ApiServerBase apiServerBase) {
        try {
            registerStageBean(apiServerBase);
            apiServerBase.setApplicationContext(applicationContext);
        } catch (Exception e) {
            throw new ApiEngineException(e);
        }
    }

    private void registerStageBean(StageBase bean) {
        try {
            beanFactory.registerSingleton(generateBeanName(bean.getClass()), bean);
            beanFactory.autowireBean(bean);
            bean.afterPropertiesSet();
        } catch (Exception e) {
            throw new ApiEngineException(e);
        }
    }
}
