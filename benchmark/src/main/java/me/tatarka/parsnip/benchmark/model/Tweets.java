package me.tatarka.parsnip.benchmark.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

import me.tatarka.parsnip.annotations.SerializedName;

@Root(strict = false, name = "feed")
@SerializedName("feed")
public class Tweets {
    @ElementList(inline = true, entry = "entry")
    public List<Tweet> tweets;
}