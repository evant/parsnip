/*
 * Copyright 2015 Evan Tatarka.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.tatarka.parsnip;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public abstract class XmlAdapter<T> {
    public abstract T fromXml(XmlPullParser parser, TagInfo tagInfo) throws XmlPullParserException, IOException;

    public final T fromXml(InputStream stream) throws IOException {
        return fromXml(stream, null);
    }

    public final T fromXml(InputStream stream, String encoding) throws IOException {
        XmlPullParser parser = newPullParser();
        try {
            parser.setInput(stream, encoding);
            return fromXml(parser, TagInfo.ROOT);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    public final T fromXml(Reader reader) throws IOException {
        XmlPullParser parser = newPullParser();
        try {
            parser.setInput(reader);
            return fromXml(parser, TagInfo.ROOT);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
    }

    public final T fromXml(String string) throws IOException {
        return fromXml(new StringReader(string));
    }

    public abstract void toXml(XmlSerializer serializer, TagInfo tagInfo, T value) throws IOException;

    public final void toXml(OutputStream stream, T value) throws IOException {
        toXml(stream, null, value);
    }

    public final void toXml(OutputStream stream, String encoding, T value) throws IOException {
        XmlSerializer serializer = newSerializer();
        serializer.setOutput(stream, encoding);
        toXml(serializer, TagInfo.ROOT, value);
    }

    public final void toXml(Writer writer, T value) throws IOException {
        XmlSerializer serializer = newSerializer();
        serializer.setOutput(writer);
        toXml(serializer, TagInfo.ROOT, value);
    }

    public final String toXml(T value) throws IOException {
        StringWriter writer = new StringWriter();
        try {
            toXml(writer, value);
        } catch (IOException e) {
            throw new AssertionError(e); // No I/O writing to a Buffer.
        }
        return writer.toString();
    }

    public interface Factory {
        XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, XmlAdapters adapters);
    }

    // Taken from android's Xml util class.

    /**
     * Returns a new pull parser with namespace support.
     */
    private static XmlPullParser newPullParser() {
        try {
            KXmlParser parser = new KXmlParser();
//            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, true);
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            return parser;
        } catch (XmlPullParserException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Creates a new xml serializer.
     */
    private static XmlSerializer newSerializer() {
        try {
            return XmlSerializerFactory.instance.newSerializer();
        } catch (XmlPullParserException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Factory for xml serializers. Initialized on demand.
     */
    private static class XmlSerializerFactory {
        static final String TYPE
                = "org.kxml2.io.KXmlParser,org.kxml2.io.KXmlSerializer";
        static final XmlPullParserFactory instance;

        static {
            try {
                instance = XmlPullParserFactory.newInstance(TYPE, null);
            } catch (XmlPullParserException e) {
                throw new AssertionError(e);
            }
        }
    }
}
