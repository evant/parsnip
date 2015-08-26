package me.tatarka.fuckxml.benchmark;

import android.content.res.AssetManager;
import android.util.Log;
import me.tatarka.fuckxml.benchmark.model.feed;

import java.io.InputStream;
import java.util.concurrent.CyclicBarrier;

public class ReadingThread implements Runnable {
    private static final String TAG = "ReadingThread";

    private AssetManager assetManager;
    private CyclicBarrier gate;
    private TweetsReader reader;
    private int iterations;

    public ReadingThread(AssetManager assetManager, TweetsReader reader, int iterations, CyclicBarrier gate) {
        this.assetManager = assetManager;
        this.reader = reader;
        this.iterations = iterations;
        this.gate = gate;
    }

    @Override
    public void run() {
        try {
            gate.await();
            for (int i = 0; i < iterations; i++) {
                InputStream in = assetManager.open("twitter-atom.xml");
                feed tweets = reader.read(in);
                if (tweets == null) {
                    throw new RuntimeException("Expected Tweets");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Log.e(TAG, "Thread did not complete its batch");
        }
    }
}
