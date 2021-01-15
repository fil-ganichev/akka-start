package org.lokrusta.prototypes.connect.impl;

import org.lokrusta.prototypes.connect.impl.common.ApiEngineException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
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
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }

    public ApiServerImpl apiServer(Map<Class<?>, Class<?>> api, boolean isProxyCall) {
        ApiServerImpl apiServerImpl = ApiServerImpl.of(api
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> registerBean(entry.getValue()))), isProxyCall);
        api.values().forEach(this::registerBean);
        registerApiServer(apiServerImpl);
        if (isProxyCall) {
            api.keySet().forEach(clazz -> {
                Object proxyInstance = apiServerImpl.getProxy(clazz);
                beanFactory.registerSingleton(generateBeanName(proxyInstance.getClass()), proxyInstance);
            });
        }
        return apiServerImpl;
    }

    public ApiServerImpl apiServer(Map<Class<?>, Class<?>> api) {
        return apiServer(api, false);
    }

    public ApiSourceImpl apiSource(ApiSourceImpl apiSource) {
        beanFactory.registerSingleton(generateBeanName(apiSource.getClass()), apiSource);
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
        String baseName = clazz.getSimpleName();
        for (int i = 1; true; i++) {
            baseName = baseName + i;
            if (!applicationContext.containsBean(baseName)) {
                break;
            }
        }
        return baseName;
    }

    private void registerApiServer(ApiServerImpl apiServerImpl) {
        try {
            registerStageBean(apiServerImpl);
            apiServerImpl.setApplicationContext(applicationContext);
        } catch (Exception e) {
            throw new ApiEngineException(e);
        }
    }

    private void registerStageBean(InitializingBean bean) {
        try {
            beanFactory.registerSingleton(generateBeanName(bean.getClass()), bean);
            bean.afterPropertiesSet();
        } catch (Exception e) {
            throw new ApiEngineException(e);
        }
    }
}
