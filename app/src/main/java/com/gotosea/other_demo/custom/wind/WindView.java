package com.gotosea.other_demo.custom.wind;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.gotosea.other_demo.custom.rain.BaseView;

/**
 * Created by luogj on 2018/5/8.
 */

public class WindView extends BaseView {
    public WindView(Context context) {
        super(context);
    }

    public WindView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void drawSub(Canvas canvas) {

    }

    @Override
    protected void animLogic() {

    }

    @Override
    protected boolean needStopAnimThread() {
        return false;
    }

    @Override
    protected void onAnimEnd() {

    }
}
