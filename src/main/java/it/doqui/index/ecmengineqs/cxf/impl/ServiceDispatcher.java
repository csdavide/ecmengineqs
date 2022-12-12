package it.doqui.index.ecmengineqs.cxf.impl;

import io.quarkus.arc.All;
import it.doqui.index.ecmengine.mtom.exception.EcmEngineException;
import it.doqui.index.ecmengineqs.cxf.ServiceProxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@Slf4j
public class ServiceDispatcher implements InvocationHandler {

    public ServiceProxy getProxy() {
        return (ServiceProxy) Proxy.newProxyInstance(ServiceDispatcher.class.getClassLoader(), new Class<?>[]{ ServiceProxy.class }, this);
    }

    @Inject
    @All
    List<AbstractServiceBridge> bridges;

    final Map<String, Pair<Object,Method>> wrappedMethods = new HashMap<>();

    @PostConstruct
    public void init() {
        if (bridges != null) {
            bridges.forEach(bridge -> {
                for (Method m : bridge.getClass().getMethods()) {
                    wrappedMethods.put(m.getName(), new ImmutablePair<>(bridge, m));
                }
            });
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Pair<Object,Method> p = wrappedMethods.get(method.getName());
        if (p != null) {
            try {
                log.info("[ServiceImpl::{}] BEGIN", method.getName());
                return p.getRight().invoke(p.getLeft(), args);
            } finally {
                log.info("[ServiceImpl::{}] END", method.getName());
            }
        } else {
            throw new EcmEngineException("method not yet implemented");
        }
    }
}
