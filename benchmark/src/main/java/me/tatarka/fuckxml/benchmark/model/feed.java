package me.tatarka.fuckxml.benchmark.model;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(strict = false)
public class feed {
    @ElementList(inline = true, entry = "entry")
    public List<entry> tweets;
}