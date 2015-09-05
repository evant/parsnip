/*
 * Copyright 2015 Evan Tatarka.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tatarka.parsnip.benchmark;

import android.content.res.AssetManager;
import android.util.Log;
import me.tatarka.parsnip.benchmark.model.Tweets;

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
                Tweets tweets = reader.read(in);
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
