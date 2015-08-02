package me.tatarka.fuckxml;

import java.io.IOException;

/**
 * Created by evan on 8/1/15.
 */
public class AttributeXmlAdapter<T> extends XmlAdapter<T> {
    
    private TypeConverter<T> converter;
    
    public AttributeXmlAdapter(TypeConverter<T> converter) {
        this.converter = converter;
    }
   
    @Override
    public T fromXml(XmlReader reader) throws IOException {
        return converter.from(reader.nextValue());
    }

    @Override
    public void toXml(XmlWriter write, T value) throws IOException {
        //TODO
        converter.to(value);
    }
}
