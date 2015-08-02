package me.tatarka.fuckxml;

import java.io.IOException;

public class TagXmlAdapter<T> extends XmlAdapter<T> {

    private TypeConverter<T> converter;

    public TagXmlAdapter(TypeConverter<T> converter) {
        this.converter = converter;
    }

    @Override
    public T fromXml(XmlReader reader) throws IOException {
        T result = converter.from(reader.nextText());
        reader.endTag();
        return result;
    }

    @Override
    public void toXml(XmlWriter write, T value) throws IOException {
        //TODO
        converter.to(value);
    }
}
