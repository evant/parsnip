package me.tatarka.fuckxml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

/**
 * Created by evan on 7/3/15.
 */
public abstract class XmlAdapter<T> {
    public abstract T fromXml(XmlReader reader) throws IOException;

    public final T fromXml(BufferedSource source) throws IOException {
        return fromXml(new XmlReader(source));
    }

    public final T fromXml(String string) throws IOException {
        return fromXml(new Buffer().writeUtf8(string));
    }

    public abstract void toXml(XmlWriter write, T value) throws IOException;

    public final void toXml(BufferedSink sink, T value) throws IOException {
        toXml(new XmlWriter(sink), value);
    }

    public final String toXml(T value) throws IOException {
        Buffer buffer = new Buffer();
        try {
            toXml(buffer, value);
        } catch (IOException e) {
            throw new AssertionError(e); // No I/O writing to a Buffer.
        }
        return buffer.readUtf8();
    }

    public interface Factory {
        XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, Xml xml);
    }
}
