package me.tatarka.fuckxml.benchmark.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import me.tatarka.fuckxml.annottions.Tag;

@Root(strict = false)
public class Author {
    @Tag
    @Element
    public String name;
    @Tag
    @Element
    public String uri;
}
