package org.tuzhao.upgrade.wiget;

import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * 持有虚引用的Handler
 * <p>
 * Created by tuzhao on 2018/03/22.
 */
public abstract class UpgradeWeakHandler<T> extends android.os.Handler {

    private final WeakReference<T> w;

    public UpgradeWeakHandler(T t) {
        super();
        this.w = new WeakReference<T>(t);
    }

    public UpgradeWeakHandler(T t, Looper looper) {
        super(looper);
        this.w = new WeakReference<T>(t);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        T t = w.get();
        if (t != null) {
            weakMessage(msg, t);
        }
    }

    public void clear() {
        if (null != w) {
            w.clear();
        }
    }

    public T get() {
        return w.get();
    }

    public abstract void weakMessage(Message msg, T t);

}
