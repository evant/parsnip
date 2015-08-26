package me.tatarka.parsnip.benchmark;

import java.io.InputStream;

import me.tatarka.parsnip.benchmark.model.Tweets;

public interface TweetsReader {
    Tweets read(InputStream stream) throws Exception;
}
