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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

public abstract class XmlAdapter<T> {
    public abstract T fromXml(XmlReader reader) throws IOException;

    public final T fromXml(BufferedSource source) throws IOException {
        return fromXml(new XmlReader(source));
    }

    public final T fromXml(String string) throws IOException {
        return fromXml(new Buffer().writeUtf8(string));
    }

    public abstract void toXml(XmlWriter writer, T value) throws IOException;

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
        XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, XmlAdapters adapters);
    }
}
