package me.tatarka.fuckxml.benchmark;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.tatarka.fuckxml.benchmark.PerformanceTestRunner.TweetsReaderFactory;
import me.tatarka.fuckxml.benchmark.parsers.DOMTweetsReader;
import me.tatarka.fuckxml.benchmark.parsers.FuckXmlReflectionTweetsReader;
import me.tatarka.fuckxml.benchmark.parsers.FuckXmlTweetsReader;
import me.tatarka.fuckxml.benchmark.parsers.PullParserTweetsReader;
import me.tatarka.fuckxml.benchmark.parsers.SAXTweetsReader;
import me.tatarka.fuckxml.benchmark.parsers.SimpleXmlReader;

/**
 * Created by evan on 6/20/15.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    static final int CONCURRENCY = 1;
    static final int ITERATIONS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BarGraph graphView = (BarGraph) findViewById(R.id.graph);
        final Button startBenchmarks = (Button) findViewById(R.id.startBenchmarks);
        startBenchmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, Statistics[]>() {
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
                    protected void onPostExecute(final Statistics[] stats) {
                        if (stats != null) {
                            ArrayList<Bar> bars = new ArrayList<>(stats.length);
                            for (Statistics stat : stats) {
                                Bar b = new Bar();
                                b.setName(stat.getName());
                                b.setValue(stat.getAverageThroughputPerSecond());
                                bars.add(b);
                            }
                            graphView.setBars(bars);
                        }
                    }
                }.execute();
            }
        });
    }

    public Statistics[] comparePerformance(int concurrency, int iterations) throws Exception {
        List<PerformanceTestRunner> runners = Arrays.asList(
                newFuckXmlParserRunner(),
                newFuckXmlReflectionRunner(),
                newDOMRunner(),
                newSAXRunner(),
                newPullParserRunner(),
                newSimpleXmlRunner()
        );

        // warm up and discard the first results
        performNRunsWithEachTestRunner(concurrency, iterations, runners, 3);

        return Statistics.combine(performNRunsWithEachTestRunner(concurrency, iterations, runners, 5));
    }

    private Statistics[] performNRunsWithEachTestRunner(int concurrency, int iterations, List<PerformanceTestRunner> testRunners, int numberOfRuns) throws Exception {
        Statistics[] stats = new Statistics[numberOfRuns * testRunners.size()];
        int testRunnerSize = testRunners.size();
        for (int i = 0; i < numberOfRuns; i++) {
            Log.d(TAG, "Beginning test run " + (i + 1));
            for (int j = 0; j < testRunnerSize; j++) {
                PerformanceTestRunner r = testRunners.get(j);
                stats[i * testRunnerSize + j] = r.collectStatistics(getAssets(), concurrency, iterations);
            }
        }
        return stats;
    }

    private PerformanceTestRunner newDOMRunner() {
        return new PerformanceTestRunner(
                new TweetsReaderFactory() {
                    @Override
                    public String getParserType() {
                        return "W3C DOM";
                    }

                    @Override
                    public TweetsReader newReader() throws Exception {
                        return new DOMTweetsReader();
                    }
                }
        );
    }

    private PerformanceTestRunner newSAXRunner() {
        return new PerformanceTestRunner(
                new TweetsReaderFactory() {
                    @Override
                    public String getParserType() {
                        return "SAX";
                    }

                    @Override
                    public TweetsReader newReader() throws Exception {
                        return new SAXTweetsReader();
                    }
                }
        );
    }

    private PerformanceTestRunner newPullParserRunner() {
        return new PerformanceTestRunner(
                new TweetsReaderFactory() {
                    @Override
                    public String getParserType() {
                        return "Pull";
                    }

                    @Override
                    public TweetsReader newReader() throws Exception {
                        return new PullParserTweetsReader();
                    }
                }
        );
    }

    private PerformanceTestRunner newFuckXmlParserRunner() {
        return new PerformanceTestRunner(
                new TweetsReaderFactory() {
                    @Override
                    public String getParserType() {
                        return "Fuck Xml";
                    }

                    @Override
                    public TweetsReader newReader() throws Exception {
                        return new FuckXmlTweetsReader();
                    }
                }
        );
    }

    private PerformanceTestRunner newFuckXmlReflectionRunner() {
        return new PerformanceTestRunner(
                new TweetsReaderFactory() {
                    @Override
                    public String getParserType() {
                        return "Fuck Xml Reflection";
                    }

                    @Override
                    public TweetsReader newReader() throws Exception {
                        return new FuckXmlReflectionTweetsReader();
                    }
                }
        );
    }

    private PerformanceTestRunner newSimpleXmlRunner() {
        return new PerformanceTestRunner(new TweetsReaderFactory() {
            @Override
            public String getParserType() {
                return "Simple XML";
            }

            @Override
            public TweetsReader newReader() throws Exception {
                return new SimpleXmlReader();
            }
        });
    }
}
