package me.tatarka.fuckxml;

import java.io.IOException;

/**
 * Created by evan on 8/1/15.
 */
public class TextXmlAdapter<T> extends XmlAdapter<T> {
    
    private TypeConverter<T> converter;
    
    public TextXmlAdapter(TypeConverter<T> converter) {
        this.converter = converter;
    }
   
    @Override
    public T fromXml(XmlReader reader) throws IOException {
        return converter.from(reader.nextText());
    }

    @Override
    public void toXml(XmlWriter write, T value) throws IOException {
        //TODO
        converter.to(value);
    }
}
