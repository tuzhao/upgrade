package org.tuzhao.upgrade.wiget;

import java.lang.ref.WeakReference;

/**
 * 持有虚引用的Runnable
 * <p>
 * Created by tuzhao on 2018/03/22.
 */
public abstract class WeakRunnable<T> implements Runnable {

    private final WeakReference<T> wr;

    public WeakRunnable(T t) {
        wr = new WeakReference<>(t);
    }

    @Override
    public void run() {
        T t = wr.get();
        if (t != null) {
            weakRun(t);
        }
    }

    public abstract void weakRun(T t);

    public T getObj() {
        return wr.get();
    }

    public void clear() {
        wr.clear();
    }

}
