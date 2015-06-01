package omed.TimerBF;

import com.google.inject.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 28.03.14
 * Time: 14:24
 * To change this template use File | Settings | File Templates.
 */



public class ObjectScope {

    private Map<Key<?>,Object> store = new HashMap<Key<?>,Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        return (T)store.get(key);
    }

    public <T> void set(Key<T> key, T instance) {
        store.put(key, instance);
    }

}




