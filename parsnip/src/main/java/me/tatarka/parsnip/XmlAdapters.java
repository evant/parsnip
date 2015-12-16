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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XmlAdapters {
    private final List<XmlAdapter.Factory> factories;
    private final List<TypeConverter.Factory> typeConverterFactories;
    private final ThreadLocal<List<DeferredAdapter<?>>> reentrantCalls = new ThreadLocal<>();
    private final Map<Object, XmlAdapter<?>> adapterCache = new LinkedHashMap<>();

    XmlAdapters(List<XmlAdapter.Factory> factories, List<TypeConverter.Factory> typeConverterFactories) {
        this.factories = Collections.unmodifiableList(factories);
        this.typeConverterFactories = Collections.unmodifiableList(typeConverterFactories);
    }

    @SuppressWarnings("unchecked") // Factories are required to return only matching JsonAdapters.
    public <T> XmlAdapter<T> adapter(Type type, Set<? extends Annotation> annotations) {
        // If there's an equivalent adapter in the cache, we're done!
        Object cacheKey = cacheKey(type, annotations);
        synchronized (adapterCache) {
            XmlAdapter<?> result = adapterCache.get(cacheKey);
            if (result != null) return (XmlAdapter<T>) result;
        }

        // Short-circuit if this is a reentrant call.
        List<DeferredAdapter<?>> deferredAdapters = reentrantCalls.get();
        if (deferredAdapters != null) {
            for (int i = 0, size = deferredAdapters.size(); i < size; i++) {
                DeferredAdapter<?> deferredAdapter = deferredAdapters.get(i);
                if (deferredAdapter.cacheKey.equals(cacheKey)) {
                    return (XmlAdapter<T>) deferredAdapter;
                }
            }
        } else {
            deferredAdapters = new ArrayList<>();
            reentrantCalls.set(deferredAdapters);
        }

        // Prepare for re-entrant calls, then ask each factory to create a type adapter.
        DeferredAdapter<T> deferredAdapter = new DeferredAdapter<>(cacheKey);
        deferredAdapters.add(deferredAdapter);
        try {
            for (int i = 0, size = factories.size(); i < size; i++) {
                XmlAdapter<T> result = (XmlAdapter<T>) factories.get(i).create(type, annotations, this);
                if (result != null) {
                    deferredAdapter.ready(result);
                    synchronized (adapterCache) {
                        adapterCache.put(cacheKey, result);
                    }
                    return result;
                }
            }
        } finally {
            deferredAdapters.remove(deferredAdapters.size() - 1);
            if (deferredAdapters.isEmpty()) {
                reentrantCalls.remove();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") // Factories are required to return only matching JsonAdapters.
    public <T> XmlAdapter<T> nextAdapter(XmlAdapter.Factory skipPast, Type type, Set<? extends Annotation> annotations) {
        int skipPastIndex = factories.indexOf(skipPast);
        if (skipPastIndex == -1) {
            throw new IllegalArgumentException("Unable to skip past unknown factory " + skipPast);
        }
        for (int i = skipPastIndex + 1, size = factories.size(); i < size; i++) {
            XmlAdapter<T> result = (XmlAdapter<T>) factories.get(i).create(type, annotations, this);
            if (result != null) return result;
        }
        return null;
    }

    public <T> TypeConverter<T> converter(Type type, Set<? extends Annotation> annotations) {
        return createConverter(0, type, annotations);
    }

    public <T> TypeConverter<T> nextConverter(TypeConverter.Factory skipPast, Type type, Set<? extends Annotation> annotations) {
        return createConverter(typeConverterFactories.indexOf(skipPast) + 1, type, annotations);
    }

    /**
     * Returns an opaque object that's equal if the type and annotations are equal.
     */
    private Object cacheKey(Type type, Set<? extends Annotation> annotations) {
        if (annotations.isEmpty()) return type;
        return Arrays.asList(type, annotations);
    }

    @SuppressWarnings("unchecked") // Factories are required to return only matching TypeConverters.
    private <T> TypeConverter<T> createConverter(int firstIndex, Type type, Set<? extends Annotation> annotations) {
        for (int i = firstIndex, size = typeConverterFactories.size(); i < size; i++) {
            TypeConverter<T> result = (TypeConverter<T>) typeConverterFactories.get(i).create(type, annotations);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Sometimes a type adapter factory depends on its own product; either directly or indirectly.
     * To make this work, we offer this type adapter stub while the final adapter is being computed.
     * When it is ready, we wire this to delegate to that finished adapter.
     * <p/>
     * <p>Typically this is necessary in self-referential object models, such as an {@code Employee}
     * class that has a {@code List<Employee>} field for an organization's management hierarchy.
     */
    private static class DeferredAdapter<T> extends XmlAdapter<T> {
        private Object cacheKey;
        private XmlAdapter<T> delegate;

        public DeferredAdapter(Object cacheKey) {
            this.cacheKey = cacheKey;
        }

        public void ready(XmlAdapter<T> delegate) {
            this.delegate = delegate;
            this.cacheKey = null;
        }

        @Override
        public T fromXml(XmlPullParser parser, TagInfo tagInfo) throws IOException, XmlPullParserException {
            if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
            return delegate.fromXml(parser, tagInfo);
        }

        @Override
        public void toXml(XmlSerializer serializer, TagInfo tagInfo, T value) throws IOException {
            if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
            delegate.toXml(serializer, tagInfo, value);
        }
    }
}
