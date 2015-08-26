package me.tatarka.parsnip.benchmark;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.echo.holographlibrary.Bar;
import com.echo.holographlibrary.BarGraph;

import java.util.ArrayList;

public class MainActivity extends Activity implements StatisticsTask.StateListener {
    private static final String TAG = "Parsnip";
    private StatisticsTask task;

    private BarGraph graph;
    private Button startBenchmarks;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graph = (BarGraph) findViewById(R.id.graph);
        startBenchmarks = (Button) findViewById(R.id.startBenchmarks);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        task = (StatisticsTask) getLastNonConfigurationInstance();
        if (task != null) {
            task.setListener(this);
        }
        startBenchmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                task = new StatisticsTask(getAssets());
                task.setListener(MainActivity.this);
                task.execute();
            }
        });
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return task;
    }

    @Override
    public void onTaskStart() {
        progressBar.setProgress(0);
        startBenchmarks.setEnabled(false);
    }

    @Override
    public void onTaskProgress(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);
    }

    @Override
    public void onTaskResult(ArrayList<Bar> bars) {
        progressBar.setProgress(0);
        startBenchmarks.setEnabled(true);
        graph.setBars(bars);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        task.setListener(null);
    }
}
