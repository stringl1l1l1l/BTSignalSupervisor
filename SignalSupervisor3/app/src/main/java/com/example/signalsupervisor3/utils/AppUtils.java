package com.example.signalsupervisor3.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.signalsupervisor3.GlobalData;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppUtils {
    public static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private static Toast mToast;

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
        float[] logFreq = GlobalData.sCh2FreqArray;
        float[] vMaxRatios = GlobalData.sCh2VMaxArray;
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
