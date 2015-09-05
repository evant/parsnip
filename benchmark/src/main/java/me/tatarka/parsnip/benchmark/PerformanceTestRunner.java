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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class PerformanceTestRunner {
    private static final String TAG = "TestRunner";
    
    public static List<PerformanceTestRunner> of(TweetsReaderFactory...factories) {
        List<PerformanceTestRunner> runners = new ArrayList<>(factories.length);
        for (TweetsReaderFactory factory : factories) {
            runners.add(new PerformanceTestRunner(factory));
        }
        return Collections.unmodifiableList(runners);
    }

    public interface TweetsReaderFactory {
        String getParserType();

        TweetsReader newReader() throws Exception;
    }

    private TweetsReaderFactory factory;

    public PerformanceTestRunner(TweetsReaderFactory aFactory) {
        factory = aFactory;
    }

    public Statistics collectStatistics(AssetManager assetManager, int maxConcurrency, int iterations) throws Exception {
        Log.d(TAG, factory.getParserType());
        Statistics stats = new Statistics(factory.getParserType());
        for (int i = 1; i <= maxConcurrency; i++) {
            long time = testPerformanceWithConcurrency(assetManager, i, iterations);
            stats.add(i, time, i * iterations);
        }
        return stats;
    }

    private long testPerformanceWithConcurrency(AssetManager assetManager, int concurrency, int iterations) throws Exception {
        CyclicBarrier gate = new CyclicBarrier(concurrency + 1);
        List<Thread> threads = new ArrayList<Thread>();

        // prepare everything in advance, we only want to time
        // the actual parsing
        for (int i = 0; i < concurrency; i++) {
            threads.add(new Thread(new ReadingThread(assetManager, factory.newReader(), iterations, gate)));
        }

        // get the threads lined up and waiting to go
        for (Thread t : threads) {
            t.start();
        }

        // start timing _just_ before we allow the threads to go at it
        long start = System.nanoTime();
        gate.await();

        // wait for all parsing threads to finish
        for (Thread t : threads) {
            t.join();
        }
        long stop = System.nanoTime();
        
        return stop - start;
    }
}
