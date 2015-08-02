package me.tatarka.fuckxml.benchmark.model;

import java.util.Date;

import me.tatarka.fuckxml.annottions.Namespace;
import me.tatarka.fuckxml.annottions.SerializedName;
import me.tatarka.fuckxml.annottions.Tag;

@SerializedName("entry")
public class Tweet {
    @Tag
    public String title;
    @Tag
    public Date published;
    public Content content;
    @Namespace("http://api.twitter.com/")
    @SerializedName("lang")
    @Tag
    public String language;
    public Author author;
}
