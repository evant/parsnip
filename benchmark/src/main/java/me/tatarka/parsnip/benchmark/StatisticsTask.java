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
import android.os.AsyncTask;
import android.util.Log;

import com.echo.holographlibrary.Bar;

import java.util.ArrayList;
import java.util.List;

import me.tatarka.parsnip.benchmark.parsers.DOMTweetsReader;
import me.tatarka.parsnip.benchmark.parsers.ParsnipTweetsReader;
import me.tatarka.parsnip.benchmark.parsers.PullParserTweetsReader;
import me.tatarka.parsnip.benchmark.parsers.SAXTweetsReader;
import me.tatarka.parsnip.benchmark.parsers.SimpleXmlReader;

public class StatisticsTask extends AsyncTask<Void, Integer, Statistics[]> {
    private static final String TAG = "Parsnip";

    static final int CONCURRENCY = 1;
    static final int ITERATIONS = 20;
    static final int WARM_UP_RUNS = 3;
    static final int ITERATION_RUNS = 5;

    private AssetManager assetManager;
    private ArrayList<Bar> bars;
    private StateListener listener;
    private int progress;
    private int total;
    private boolean isStarted;

    public StatisticsTask(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    protected void onPreExecute() {
        isStarted = true;
        if (listener != null) {
            listener.onTaskStart();
        }
    }

    public void setListener(StateListener listener) {
        this.listener = listener;
        if (listener != null) {
            if (isStarted) {
                listener.onTaskStart();
                if (progress > 0 && total > 0) {
                    listener.onTaskProgress(progress, total);
                }
            }
            if (bars != null) {
                listener.onTaskResult(bars);
            }
        }
    }

    @Override
    protected Statistics[] doInBackground(Void... params) {
        try {
            return comparePerformance(CONCURRENCY, ITERATIONS);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        this.progress = values[0];
        this.total = values[1];
        if (listener != null) {
            listener.onTaskProgress(values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Statistics[] stats) {
        if (stats != null) {
            ArrayList<Bar> bars = new ArrayList<>(stats.length);
            for (Statistics stat : stats) {
                Bar b = new Bar();
                b.setName(stat.getName());
                b.setValue(stat.getAverageThroughputPerSecond());
                bars.add(b);
            }
            this.bars = bars;
            if (listener != null) {
                listener.onTaskResult(bars);
            }
        }
        isStarted = false;
    }

    public Statistics[] comparePerformance(int concurrency, int iterations) throws Exception {
        List<PerformanceTestRunner> runners = PerformanceTestRunner.of(
                ParsnipTweetsReader.FACTORY,
                SAXTweetsReader.FACTORY,
                PullParserTweetsReader.FACTORY,
                DOMTweetsReader.FACTORY,
                ParsnipTweetsReader.FACTORY,
                SimpleXmlReader.FACTORY
        );

        int size = runners.size();
        int totalCount = (WARM_UP_RUNS + ITERATION_RUNS) * size;
        int startCount = 0;
        // warm up and discard the first results
        performNRunsWithEachTestRunner(concurrency, iterations, runners, WARM_UP_RUNS, startCount, totalCount);
        startCount = WARM_UP_RUNS * size;
        return Statistics.combine(performNRunsWithEachTestRunner(concurrency, iterations, runners, ITERATION_RUNS, startCount, totalCount));
    }

    private Statistics[] performNRunsWithEachTestRunner(int concurrency, int iterations, List<PerformanceTestRunner> testRunners, int numberOfRuns, int startCount, int totalCount) throws Exception {
        Statistics[] stats = new Statistics[numberOfRuns * testRunners.size()];
        int testRunnerSize = testRunners.size();
        for (int i = 0; i < numberOfRuns; i++) {
            Log.d(TAG, "Beginning test run " + (i + 1));
            for (int j = 0; j < testRunnerSize; j++) {
                PerformanceTestRunner r = testRunners.get(j);
                stats[i * testRunnerSize + j] = r.collectStatistics(assetManager, concurrency, iterations);
                publishProgress(startCount + i * testRunnerSize + j, totalCount);
            }
        }
        return stats;
    }


    public interface StateListener {
        void onTaskStart();

        void onTaskProgress(int progress, int total);

        void onTaskResult(ArrayList<Bar> bars);
    }
}
