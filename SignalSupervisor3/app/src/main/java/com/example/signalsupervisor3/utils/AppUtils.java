package com.example.signalsupervisor3.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signalsupervisor3.GlobalData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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

    public static float[] makeRandomGaussianPoints(int xRange, int yRange) {
        float mu = 3000, sigma = 2;
        float[] points = new float[xRange * yRange * 2];
        Random r = new Random();
        for (int i = 0; i < xRange * yRange * 2; i++) {
            float v = (float) (r.nextGaussian() * sigma + mu);
            points[i] = v;
        }
        return points;
    }

    public static float[] makeRandomPoints(int xRange, int yRange) {
        float[] points = new float[xRange * yRange * 2];

        Random r = new Random(System.currentTimeMillis());
        int i = 0;
        while (i < xRange * yRange * 2) {
            float x = (float) (r.nextDouble() * xRange);
            float y = (float) (r.nextDouble() * yRange);
            points[i++] = x;
            points[i++] = y;
        }
        return points;
    }

    public static float[] makeSinPoints(int xRange, int yRange) {
        float[] points = new float[xRange * yRange * 2];
        for (int i = 0; i < xRange * yRange * 2; i++) {
            float y = (float) Math.sin(3.14 * i) * 1.0f * yRange / 2 + 1.0f * yRange / 2;
            points[i] = i++;
            points[i] = y;
        }
        return points;
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
        Log.i("getFPointsFromGlobal points", res.toString());
        return smooth(res, 3, 0.5);
    }

    private static List<FPoint> smooth(List<FPoint> points, int windowSize, double threshold) {
        List<FPoint> smoothedPoints = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            float sumX = 0;
            float sumY = 0;
            int count = 0;

            for (int j = Math.max(0, i - windowSize); j <= Math.min(points.size() - 1, i + windowSize); j++) {
                sumX += points.get(j).x;
                sumY += points.get(j).y;
                count++;
            }

            float averageX = sumX / count;
            float averageY = sumY / count;

            float distance = (float) Math.sqrt(Math.pow(points.get(i).x - averageX, 2) + Math.pow(points.get(i).y - averageY, 2));
            if (distance <= threshold) {
                smoothedPoints.add(new FPoint(averageX, averageY));
            }
        }
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
