package org.mockito.internal.creation;

import java.io.Serializable;

import org.mockito.cglib.proxy.MethodProxy;
import org.mockito.internal.util.reflection.Whitebox;

public class SerializableMockitoMethodProxy extends AbstractMockitoMethodProxy implements Serializable {

    private static final long serialVersionUID = -5337859962876770632L;
    private Class<?> c1;
    private Class<?> c2;
    private String desc;
    private String name;
    private String superName;
    private transient MethodProxy methodProxy;

    public SerializableMockitoMethodProxy(MethodProxy methodProxy) {
        Object info = Whitebox.getInternalState(methodProxy, "createInfo");
        c1 = (Class<?>) Whitebox.getInternalState(info, "c1");
        c2 = (Class<?>) Whitebox.getInternalState(info, "c2");
        desc = methodProxy.getSignature().getDescriptor();
        name = methodProxy.getSignature().getName();
        superName = methodProxy.getSuperName();
        this.methodProxy = methodProxy;
    }

    protected MethodProxy getMethodProxy() {
        if (methodProxy == null)
            methodProxy = MethodProxy.create(c1, c2, desc, name, superName);
        return methodProxy;
    }
}
