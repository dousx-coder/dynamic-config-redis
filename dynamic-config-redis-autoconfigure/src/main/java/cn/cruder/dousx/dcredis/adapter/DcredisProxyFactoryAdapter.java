package cn.cruder.dousx.dcredis.adapter;

import cn.cruder.dousx.dcredis.factory.DcredisProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ConfigProxyFactory适配器
 */
public class DcredisProxyFactoryAdapter implements FactoryBean<Object> {

    private final Class<?> interfaceType;

    @Autowired
    private DcredisProxyFactory dcredisProxyFactory;

    public DcredisProxyFactoryAdapter(Class<?> interfaceType) {
        this.interfaceType = interfaceType;
    }

    @Override
    public Object getObject() {
        return dcredisProxyFactory.createProxy(interfaceType);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }

}