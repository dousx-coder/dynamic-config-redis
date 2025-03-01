package cn.cruder.dousx.dcredis.adapter;

import cn.cruder.dousx.dcredis.factory.ConfigProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ConfigProxyFactory适配器
 */
public class ConfigProxyFactoryAdapter implements FactoryBean<Object> {

    private final Class<?> interfaceType;

    @Autowired
    private ConfigProxyFactory configProxyFactory;

    public ConfigProxyFactoryAdapter(Class<?> interfaceType) {
        this.interfaceType = interfaceType;
    }

    @Override
    public Object getObject() {
        return configProxyFactory.createProxy(interfaceType);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }

}