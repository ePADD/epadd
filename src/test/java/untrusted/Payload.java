package untrusted;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Stands in for a malicious class in a crafted archive: it only records that its code ran when it was read. It is in
 * its own package so that it is outside the packages SafeObjectInput allows, like a class from a library would be.
 */
public class Payload implements Serializable, InvocationHandler {

    private static final long serialVersionUID = 1L;

    public static volatile boolean executed = false;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        executed = true;
        in.defaultReadObject();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        executed = true;
        return null;
    }
}
