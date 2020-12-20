package org.lokrusta.prototypes.connect.impl;

import org.lokrusta.prototypes.connect.impl.common.ApiEngineException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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
            beanFactory.registerSingleton(generateBeanName(ApiServerImpl.class), apiServerImpl);
            apiServerImpl.setApplicationContext(applicationContext);
            apiServerImpl.afterPropertiesSet();
        } catch (Exception e) {
            throw new ApiEngineException(e);
        }
    }
}
