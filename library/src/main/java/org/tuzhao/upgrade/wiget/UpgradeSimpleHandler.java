package org.tuzhao.upgrade.wiget;

import android.app.Activity;
import android.os.Looper;
import android.os.Message;

/**
 * @author tuzhao
 */
public final class UpgradeSimpleHandler extends UpgradeWeakHandler<Activity> {

    public UpgradeSimpleHandler(Activity activity) {
        super(activity);
    }

    public UpgradeSimpleHandler(Activity activity, Looper looper) {
        super(activity, looper);
    }

    @Override
    public void weakMessage(Message msg, Activity activity) {

    }
}
