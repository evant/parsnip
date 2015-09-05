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
import java.util.LinkedHashSet;
import java.util.Set;

import me.tatarka.parsnip.annotations.Tag;

/**
 * If there is a {@link Tag} annotation, this will delegate to just the text of the tag. This is
 * useful for handling xml tags without attributes.
 */
public class TagXmlAdapter<T> extends XmlAdapter<T> {

    static final Factory FACTORY = new Factory() {
        @Override
        public XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, XmlAdapters adapters) {
            if (!me.tatarka.parsnip.Util.isAnnotationPresent(annotations, Tag.class)) {
                return null;
            }
            Set<Annotation> restOfAnnotations = new LinkedHashSet<>();
            for (Annotation annotation : annotations) {
                if (!(annotation instanceof Tag)) {
                    restOfAnnotations.add(annotation);
                }
            }
            me.tatarka.parsnip.TypeConverter<?> converter = adapters.converter(type, restOfAnnotations);
            if (converter == null) {
                throw new IllegalArgumentException("No TypeConverter for type " + type + " and annotations " + restOfAnnotations);
            }
            return new TagXmlAdapter<>(converter);
        }
    };

    private final me.tatarka.parsnip.TypeConverter<T> converter;

    public TagXmlAdapter(me.tatarka.parsnip.TypeConverter<T> converter) {
        this.converter = converter;
    }

    @Override
    public T fromXml(me.tatarka.parsnip.XmlReader reader) throws IOException {
        return converter.from(reader.nextText());
    }

    @Override
    public void toXml(XmlWriter writer, T value) throws IOException {
        writer.text(converter.to(value));
    }
}
