package me.tatarka.fuckxml.benchmark;

import me.tatarka.fuckxml.benchmark.model.Tweets;

import java.io.InputStream;

public interface TweetsReader {
    String getParserName();

    Tweets read(InputStream stream) throws Exception;
}
