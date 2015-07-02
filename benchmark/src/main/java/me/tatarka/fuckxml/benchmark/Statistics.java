package me.tatarka.fuckxml.benchmark;

import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Statistics implements Iterable<Statistics.Entry> {
    public static Statistics[] combine(Statistics[] stats) {
        ArrayMap<String, Statistics> statsByParser = new ArrayMap<>();
        for (Statistics s : stats) {
            Statistics statistics = statsByParser.get(s.getName());
            if (statistics == null) {
                statsByParser.put(s.getName(), s);
            } else {
                statsByParser.put(s.getName(), statistics.combine(s));
            }
        }
        Statistics[] resultStats = new Statistics[statsByParser.size()];
        int i = 0;
        for (Statistics s : statsByParser.values()) {
            s.setIndex(i);
            resultStats[i++] = s;
        }
        return resultStats;
    }
    
    public float getAverageThroughputPerSecond() {
        float average = 0;
        for (Entry entry : this) {
            average += entry.getThroughputPerSecond();
        }
        return average / size();
    }

    public class Entry {
        private int threads;
        private double time;
        private double docs;

        public Entry(int threads, double time, double docs) {
            this.threads = threads;
            this.time = time;
            this.docs = docs;
        }

        public int getThreads() {
            return threads;
        }

        public double getElapsedNanos() {
            return time;
        }

        public double getDocs() {
            return docs;
        }

        public double getThroughputPerSecond() {
            return (1000000000d / time) * docs;
        }

        public String toString() {
            return getThroughputPerSecond() + " docs/sec with " + threads + " threads";
        }
    }

    private int index;
    private String name;
    private List<Entry> entries;

    public Statistics(String name) {
        this.name = name;
        entries = new ArrayList<>();
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void add(int aThreadCount, double aNanos, double aDocs) {
        entries.add(new Entry(aThreadCount, aNanos, aDocs));
    }

    public Entry getEntry(int anIndex) {
        return entries.get(anIndex);
    }

    public int size() {
        return entries.size();
    }

    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }

    public Statistics combine(Statistics statistics) {
        if (!name.equals(statistics.name))
            throw new IllegalArgumentException("can't add " + statistics.name + " to " + name);
        if (entries.size() != statistics.size())
            throw new IllegalArgumentException("mismatched stats - I have " + size() + " but other has " + statistics.size());

        Statistics result = new Statistics(name);
        for (int i = 0; i < size(); i++) {
            Entry _mine = entries.get(i);
            Entry _other = statistics.entries.get(i);

            result.add(_mine.threads, _mine.time + _other.time, _mine.docs + _other.docs);
        }
        return result;
    }

    public String toString() {
        StringBuilder _sb = new StringBuilder();
        _sb.append("statistics for ").append(name).append("\r\n");
        for (Entry _e : entries) {
            _sb.append(_e).append("\r\n");
        }
        return _sb.toString();
    }
}
