package com.gotosea.other_demo.custom.rain;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;

import java.util.ArrayList;

/**
 * Created by luogj on 2018/5/8.
 */

public class RainView extends BaseView {
    private ArrayList<RainLine> rainLines;
    private static final int RAIN_COUNT = 900; //雨点个数
    private Paint paint;


    public RainView(Context context) {
        super(context);
    }

    public RainView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init() {
        super.init();
        rainLines = new ArrayList<>();
        for (int i = 0; i < RAIN_COUNT; i++) {
            rainLines.add(new RainLine(windowWidth, windowHeight));
        }
        paint = new Paint();
        if (paint !=null) {
            paint.setColor(0xffffffff);
        }
    }

    /**
     * 画子类
     * @param canvas
     */
    @Override
    protected void drawSub(Canvas canvas) {
        for(RainLine rainLine : rainLines) {
            canvas.drawLine(rainLine.getStartX(), rainLine.getStartY(), rainLine.getStopX(), rainLine.getStopY(), paint);
        }
    }

    /**
     * 动画逻辑处理
     */
    @Override
    protected void animLogic() {
        for(RainLine rainLine : rainLines) {
            rainLine.rain();
        }
    }

    /**
     * 里面根据当前状态判断是否需要返回停止动画
     * @return
     */
    @Override
    protected boolean needStopAnimThread() {
        for(RainLine rainLine : rainLines) {
            if (rainLine.getStartY() >= getWidth()) {
                rainLine.initRandom();
            }
        }
        return false;
    }

    /**
     * 动画结束后做的操作，比如回收资源
     */
    @Override
    protected void onAnimEnd() {

    }
}
