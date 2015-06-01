package omed.TimerBF;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 28.03.14
 * Time: 14:42
 * To change this template use File | Settings | File Templates.
 */
public class GuiceObjectScope implements Scope {

    // Make this a ThreadLocal for multithreading.
    private ObjectScope current = null;

    @Override
    public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
        return new Provider<T>() {

            @Override
            public T get() {

                // Lookup instance
                T instance = current.get(key);
                if (instance==null) {

                    // Create instance
                    instance = unscoped.get();
                    current.set(key, instance);
                }
                return instance;

            }
        };
    }

    public void enter(ObjectScope scope) {
        current = scope;
    }

    public void leave() {
        current = null;
    }

}
