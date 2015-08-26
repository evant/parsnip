package me.tatarka.parsnip.benchmark.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

import me.tatarka.parsnip.annottions.Text;

@Root(strict = false)
public class Content {
    @Attribute
    public String type;
    @Text
    @org.simpleframework.xml.Text
    public String value;
}