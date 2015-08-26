package me.tatarka.fuckxml.benchmark;

import java.io.InputStream;

import me.tatarka.fuckxml.benchmark.model.feed;

public interface TweetsReader {
    feed read(InputStream stream) throws Exception;
}
