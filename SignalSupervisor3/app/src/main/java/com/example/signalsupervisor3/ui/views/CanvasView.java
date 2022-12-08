package com.example.signalsupervisor3.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.example.signalsupervisor3.GlobalData;
import com.example.signalsupervisor3.utils.AppUtils;

import java.util.Arrays;

public class CanvasView extends View {
    private static final String TAG = CanvasView.class.getSimpleName();
    private Paint mGridPaint;//网格画笔
    private Point mWinSize;//屏幕尺寸
    private Point mCoo;//坐标系原点
    public float[] mPoints = new float[4];
    private static final int xStep = 100;
    private static final int yStep = 100;
    private static final int X_RANGE = 4;
    private static final int Y_RANGE = 15;
    private Paint mRedPaint;

    public Canvas getCanvas() {
        return mCanvas;
    }

    private Canvas mCanvas;

    public CanvasView(Context context) {
        this(context, null);
    }

    public CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mWinSize = new Point();
        loadWinSize(getContext(), mWinSize);
        mCoo = new Point(50, mWinSize.y - 100);
        mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedPaint.setColor(Color.RED);
        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }


    /**
     * 获得屏幕高度
     *
     * @param ctx     上下文
     * @param winSize 屏幕尺寸
     */
    public static void loadWinSize(Context ctx, Point winSize) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(outMetrics);
        }
//        winSize.x = outMetrics.widthPixels;
//        winSize.y = outMetrics.heightPixels;
        winSize.x = 1050;
        winSize.y = 1100;
    }

    /**
     * 绘制网格
     *
     * @param canvas  画布
     * @param winSize 屏幕尺寸
     * @param paint   画笔
     */
    private static void drawGrid(Canvas canvas, Point winSize, Paint paint) {
        //初始化网格画笔
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        //设置虚线效果new float[]{可见长度, 不可见长度},偏移值
        paint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));
        canvas.drawPath(gridPath(50, winSize), paint);
    }

    /**
     * 绘制网格:注意只有用path才能绘制虚线
     *
     * @param step    小正方形边长
     * @param winSize 屏幕尺寸
     */
    public static Path gridPath(int step, Point winSize) {
        Path path = new Path();
        for (int i = 0; i < winSize.y / step + 1; i++) {
            path.moveTo(0, step * i);
            path.lineTo(winSize.x, step * i);
        }
        for (int i = 0; i < winSize.x / step + 1; i++) {
            path.moveTo(step * i, 0);
            path.lineTo(step * i, winSize.y);
        }
        return path;
    }

    /**
     * 坐标系路径
     *
     * @param coo     坐标点
     * @param winSize 屏幕尺寸
     * @return 坐标系路径
     */
    public static Path cooPath(Point coo, Point winSize) {
        Path path = new Path();
        //x正半轴线
        path.moveTo(coo.x, coo.y);
        path.lineTo(winSize.x, coo.y);
        //x负半轴线
        path.moveTo(coo.x, coo.y);
        path.lineTo(coo.x - winSize.x, coo.y);
        //y负半轴线
        path.moveTo(coo.x, coo.y);
        path.lineTo(coo.x, coo.y - winSize.y);
        //y负半轴线
        path.moveTo(coo.x, coo.y);
        path.lineTo(coo.x, winSize.y);
        return path;
    }

    /**
     * 绘制坐标系
     *
     * @param canvas  画布
     * @param coo     坐标系原点
     * @param winSize 屏幕尺寸
     * @param paint   画笔
     */
    public static void drawCoo(Canvas canvas, Point coo, Point winSize, Paint paint) {
        //初始化网格画笔
        paint.setStrokeWidth(4);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        //设置虚线效果new float[]{可见长度, 不可见长度},偏移值
        paint.setPathEffect(null);
        //绘制直线
        canvas.drawPath(cooPath(coo, winSize), paint);
        //左箭头
        canvas.drawLine(winSize.x, coo.y, winSize.x - 40, coo.y - 20, paint);
        canvas.drawLine(winSize.x, coo.y, winSize.x - 40, coo.y + 20, paint);
//        //下箭头
//        canvas.drawLine(coo.x, winSize.y, coo.x - 20, winSize.y - 40, paint);
//        canvas.drawLine(coo.x, winSize.y, coo.x + 20, winSize.y - 40, paint);
        //上箭头
        canvas.drawLine(coo.x, 0, coo.x - 20, 40, paint);
        canvas.drawLine(coo.x, 0, coo.x + 20, 40, paint);
        //为坐标系绘制文字
        drawText4Coo(canvas, coo, winSize, paint);
    }

    /**
     * 为坐标系绘制文字
     *
     * @param canvas  画布
     * @param coo     坐标系原点
     * @param winSize 屏幕尺寸
     * @param paint   画笔
     */
    private static void drawText4Coo(Canvas canvas, Point coo, Point winSize, Paint paint) {
        //绘制文字
        paint.setTextSize(50);
        canvas.drawText("x", winSize.x - 60, coo.y - 40, paint);
        canvas.drawText("y", coo.x - 40, 60, paint);
        paint.setTextSize(25);
        //X正轴文字
        for (int i = 1; i <= (winSize.x - coo.x) / xStep; i++) {
            paint.setStrokeWidth(2);
            float val = 1.0f * xStep * X_RANGE / (winSize.x - coo.x) * i;
            String text = String.format("%.1f", val).toString();
            canvas.drawText(text, coo.x - 20 + xStep * i, coo.y + 30, paint);
            paint.setStrokeWidth(5);
            canvas.drawLine(coo.x + xStep * i, coo.y, coo.x + xStep * i, coo.y - 10, paint);
        }
//        //X负轴文字
//        for (int i = 1; i <= coo.x / xStep; i++) {
//            paint.setStrokeWidth(2);
//            canvas.drawText(-1.0 * xStep * X_RANGE / (winSize.x - coo.x) * i + "", coo.x - 20 - xStep * i, coo.y + 30, paint);
//            paint.setStrokeWidth(5);
//            canvas.drawLine(coo.x - xStep * i, coo.y, coo.x - xStep * i, coo.y - 10, paint);
//        }
//        //y负轴文字
//        for (int i = 1; i <= (winSize.y - coo.y) / yStep; i++) {
//            paint.setStrokeWidth(2);
//            canvas.drawText(-1.0 * yStep * Y_RANGE / coo.y * i + "", coo.x + 20, coo.y + 10 + yStep * i, paint);
//            paint.setStrokeWidth(5);
//            canvas.drawLine(coo.x, coo.y + yStep * i, coo.x + 10, coo.y + yStep * i, paint);
//        }
        //y正轴文字
        for (int i = 1; i <= coo.y / yStep; i++) {
            paint.setStrokeWidth(2);
            float val = 1.0f * yStep * Y_RANGE / coo.y * i;
            String text = String.format("%.1f", val).toString();
            canvas.drawText(text, coo.x + 20, coo.y + 30 - yStep * i, paint);
            paint.setStrokeWidth(5);
            canvas.drawLine(coo.x, coo.y - yStep * i, coo.x + 10, coo.y - yStep * i, paint);
        }
    }

    /**
     * 绘制颜色(注意在画坐标系前绘制，否则后者覆盖)
     *
     * @param canvas
     */
    public void drawColor(Canvas canvas) {
//        canvas.drawColor(Color.parseColor("#E0F7F5"));
//        canvas.drawARGB(255, 224, 247, 245);
//        三者等价
        canvas.drawRGB(224, 247, 245);
    }

    /**
     * 绘制点
     *
     * @param canvas
     */
    public void drawPoint(Canvas canvas, float[] points) {
        normalizePoints(mCoo, points);
        mRedPaint.setStrokeWidth(4);
        canvas.drawPoints(points, mRedPaint);
    }

    /**
     * 绘制线
     *
     * @param canvas
     */
    public void drawLine(Canvas canvas) {
        float[] points = new float[]{
                2400f, 2f, 3000f, 2.5f,
                3000f, 2.5f, 3600f, 2.0f,
                3600f, 2.0f, 4200f, 1.75f,
                4200f, 1.75f, 4800f, 1.5f,
                4800f, 1.5f, 5400f, 1.5f
        };
        normalizePoints(mCoo, points);
        mRedPaint.setStrokeWidth(4);
        //canvas.drawLine(500, 200, 900, 400, mRedPaint);
        //绘制一组点，坐标位置由float数组指定(必须是4的倍数个)
        canvas.drawLines(points, mRedPaint);
    }

    public void normalizePoints(Point origin, float[] points) {
        for (int i = 0; i < points.length; i++) {
            // 奇数
            if ((i & 0b1) == 1) {
                points[i] = origin.y - points[i] / Y_RANGE * origin.y;
            }
            //偶数
            else {
                points[i] = origin.x + points[i] / X_RANGE * (mWinSize.x - origin.x);
            }
        }
        Log.d("normalizePoints", Arrays.toString(points));
    }

//    public void normalizePoints(Point origin, float[] points) {
//        for (int i = 0; i < points.length; i++) {
//            // 奇数
//            if ((i & 0b1) == 1) {
//                points[i] = origin.y - points[i];
//            }
//            //偶数
//            else {
//                points[i] += origin.x + points[i];
//            }
//        }
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mCanvas = canvas;
        //TODO drawGrid 绘制网格：release：
        drawGrid(canvas, mWinSize, mGridPaint);
        //TODO drawCoo 绘制坐标系:release：
        drawCoo(canvas, mCoo, mWinSize, mGridPaint);
        //画点
        drawPoint(canvas, mPoints);
//        //画线
        //drawLine(canvas);
    }
}
