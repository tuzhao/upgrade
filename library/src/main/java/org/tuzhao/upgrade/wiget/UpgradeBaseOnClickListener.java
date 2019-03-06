package org.tuzhao.upgrade.wiget;

import android.view.View;

/**
 * 解决普通快速的点击 出现两次弹框的问题
 */
public abstract class UpgradeBaseOnClickListener implements View.OnClickListener {
    private long lastTimeMillis;
    private long MIN_CLICK_INTERVAL = 1000;

    public UpgradeBaseOnClickListener() {

    }

    public UpgradeBaseOnClickListener(long interval) {
        MIN_CLICK_INTERVAL = interval;
    }

    private boolean isTimeEnabled() {
        long currentTimeMillis = System.currentTimeMillis();
        if ((currentTimeMillis - lastTimeMillis) > MIN_CLICK_INTERVAL) {
            lastTimeMillis = currentTimeMillis;
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (isTimeEnabled()) {
            click(v);
        }
    }

    public abstract void click(View v);
}
