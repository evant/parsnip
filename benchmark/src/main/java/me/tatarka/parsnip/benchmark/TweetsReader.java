package me.tatarka.parsnip.benchmark;

import java.io.InputStream;

import me.tatarka.parsnip.benchmark.model.feed;

public interface TweetsReader {
    feed read(InputStream stream) throws Exception;
}
