package me.tatarka.fuckxml.benchmark.model;

import java.util.List;

import me.tatarka.fuckxml.annottions.SerializedName;

@SerializedName("feed")
public class Tweets {
    public List<Tweet> tweets;
}