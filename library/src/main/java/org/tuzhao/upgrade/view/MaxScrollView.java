package org.tuzhao.upgrade.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ScrollView;

import org.tuzhao.upgrade.BuildInfo;

/**
 * 解决maxHeight不生效的问题
 *
 * @author tuzhao
 */
public class MaxScrollView extends ScrollView {

    public MaxScrollView(Context context) {
        this(context, null);
    }

    public MaxScrollView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public MaxScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int maxPx = dip2px(getContext(), 300);
        int measuredHeight = getMeasuredHeight();
        if (measuredHeight > maxPx) {
            setMeasuredDimension(getMeasuredWidth(), maxPx);
        }
        log("px: " + maxPx + " measuredHeight: " + measuredHeight);
    }

    private static int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private static void log(String msg) {
        if (BuildInfo.DEBUG_MODE) {
            Log.d("MaxScrollView", msg);
        }
    }

}
