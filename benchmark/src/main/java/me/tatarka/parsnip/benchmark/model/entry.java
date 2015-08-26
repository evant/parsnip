package me.tatarka.parsnip.benchmark.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.util.Date;

import me.tatarka.parsnip.annottions.Tag;

@Root(strict = false)
public class entry {
    @Tag
    @Element
    public String title;
    @Tag
    @Element
    public Date published;
    @Element
    public Content content;
    @Tag
    @Element
    public String lang;
    @Element
    public Author author;
}
