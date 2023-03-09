package com.example.signalsupervisor3.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signalsupervisor3.GlobalData;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppUtils {
    public static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private static Toast mToast;

    public static class FPoint {
        public float x;
        public float y;

        public FPoint(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + " ," + y + ")";
        }
    }

    public static void showToast(Context context, String text) {
        if (mToast == null) {
            mToast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
        }
        mToast.show();
    }

    public static int getByteValidLength(byte[] bytes) {
        int i = 0;
        if (null == bytes || 0 == bytes.length)
            return i;
        for (; i < bytes.length; i++) {
            if (bytes[i] == '\0')
                break;
        }
        return i + 1;
    }

    public static List<FPoint> interpolate(List<FPoint> points, double step) {
        if (points.size() <= 1)
            return points;
        // 构造插值器
        SplineInterpolator interpolator = new SplineInterpolator();
        // 提取x和y坐标
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            x[i] = points.get(i).x;
            y[i] = points.get(i).y;
        }
        List<FPoint> res = new ArrayList<>();
        // 进行多项式样条插值
        PolynomialSplineFunction splineFunction = interpolator.interpolate(x, y);
        for (double i = x[0]; i <= x[points.size() - 1]; i += step) {
            res.add(new FPoint((float) i, (float) splineFunction.value(i)));
        }
        Log.i("interpolate ", res.toString());
        return res;
    }

    @SuppressLint("LongLogTag")
    public static float[] getPointsFromGlobal() {
        float vMax1 = GlobalData.sVMaxCh1;
        float[] logFreq = Arrays.copyOf(GlobalData.sCh2FreqArray, GlobalData.sCh2FreqArray.length);
        float[] vMaxRatios = Arrays.copyOf(GlobalData.sCh2VMaxArray, GlobalData.sCh2VMaxArray.length);
        Log.i("getPointsFromGlobal vMax1", String.valueOf(vMax1));
        Log.i("getPointsFromGlobal logFreq", Arrays.toString(logFreq));
        Log.i("getPointsFromGlobal vMaxRatios", Arrays.toString(vMaxRatios));
        if (vMax1 == 0 || logFreq == null || vMaxRatios == null) {
            return new float[4];
        }
        int minLen = Math.min(logFreq.length, vMaxRatios.length);
        float[] res = new float[minLen * 2];
        int posX = 0, posY = 1;
        for (int i = 0; i < minLen; i++) {
            logFreq[i] = (float) Math.log10(logFreq[i]);
            res[posX] = logFreq[i];
            posX += 2;
        }
        for (int i = 0; i < minLen; i++) {
            if (vMax1 != 0) {
                vMaxRatios[i] = 20 * (float) Math.log10(vMaxRatios[i] / vMax1);
            } else
                vMaxRatios[i] = 0;
            res[posY] = vMaxRatios[i];
            posY += 2;
        }
        Log.i("getPointsFromGlobal", Arrays.toString(res));
        return res;
    }

    @SuppressLint("LongLogTag")
    public static List<FPoint> getFPointsFromGlobal() {
        float vMax1 = GlobalData.sVMaxCh1;
        if (GlobalData.sCh2FreqArray == null || GlobalData.sCh2VMaxArray == null) {
            return new ArrayList<>();
        }
        float[] logFreq = Arrays.copyOf(GlobalData.sCh2FreqArray, GlobalData.sCh2FreqArray.length);
        float[] vMax2Array = Arrays.copyOf(GlobalData.sCh2VMaxArray, GlobalData.sCh2VMaxArray.length);
        Log.i("getFPointsFromGlobal vMax1", String.valueOf(vMax1));
        Log.i("getFPointsFromGlobal logFreq", Arrays.toString(logFreq));
        Log.i("getFPointsFromGlobal vMax2Array", Arrays.toString(vMax2Array));
        List<FPoint> res = new ArrayList<>();
        if (vMax1 == 0 || logFreq == null || vMax2Array == null) {
            res.add(new FPoint(0, 0));
            return res;
        }
        int minLen = Math.min(logFreq.length, vMax2Array.length);
        for (int i = 0; i < minLen; i++) {
            float x = (float) Math.log10(logFreq[i]);
            float y = 0;
            if (vMax1 != 0) {
                y = 20 * (float) Math.log10(vMax2Array[i] / vMax1);
            }
            res.add(new FPoint(x, y));
        }
        Collections.sort(res, new Comparator<FPoint>() {
            @Override
            public int compare(FPoint fPoint, FPoint t1) {
                return Float.compare(fPoint.x, t1.x);
            }
        });
        res = smooth(res, 3, 0.5);
        Log.i("getFPointsFromGlobal smoothed res", res.toString());
        List<FPoint> newRes = new ArrayList<>();
        for (int i = 0, j = 0; i < res.size(); ) {
            j = i + 1;
            FPoint head = res.get(i);
            float averageY = head.y;
            while (j < res.size() && Math.abs(res.get(j).x - res.get(i).x) < 0.01) {
                averageY += res.get(j).y;
                j++;
            }
            averageY /= j - i;
            newRes.add(new FPoint(head.x, averageY));
            i = j;
        }
        Log.i("getFPointsFromGlobal newRes", newRes.toString());
        return newRes;
    }

    /**
     * 对给定的一组二维坐标点进行平滑处理。
     *
     * @param points     要处理的坐标点列表
     * @param windowSize 平滑窗口大小，即每个点将使用左右各windowSize个点来计算平均值
     * @param threshold  平滑后每个点与原始点的距离阈值，小于等于该值的点将被加入平滑后的列表
     * @return 平滑后的坐标点列表
     */
    private static List<FPoint> smooth(List<FPoint> points, int windowSize, double threshold) {
        List<FPoint> smoothedPoints = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            // 初始化变量
            float sumX = 0;
            float sumY = 0;
            int count = 0;
            // 计算平均值
            for (int j = Math.max(0, i - windowSize); j <= Math.min(points.size() - 1, i + windowSize); j++) {
                sumX += points.get(j).x;
                sumY += points.get(j).y;
                count++;
            }
            float averageX = sumX / count;
            float averageY = sumY / count;

            // 计算距离并添加平滑后的点到列表中
            float distance = (float) Math.sqrt(Math.pow(points.get(i).x - averageX, 2) + Math.pow(points.get(i).y - averageY, 2));
            if (distance <= threshold) {
                smoothedPoints.add(new FPoint(averageX, averageY));
            }
        }
        // 输出平滑后的坐标点列表
        Log.i("smooth points", smoothedPoints.toString());
        return smoothedPoints;
    }


    public static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) {
            super(context);
        }

        public WrapContentLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public WrapContentLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}
