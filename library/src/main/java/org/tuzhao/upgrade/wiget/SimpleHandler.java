package org.tuzhao.upgrade.wiget;

import android.app.Activity;
import android.os.Looper;
import android.os.Message;

/**
 * @author tuzhao
 */
public final class SimpleHandler extends WeakHandler<Activity> {

    public SimpleHandler(Activity activity) {
        super(activity);
    }

    public SimpleHandler(Activity activity, Looper looper) {
        super(activity, looper);
    }

    @Override
    public void weakMessage(Message msg, Activity activity) {

    }
}
