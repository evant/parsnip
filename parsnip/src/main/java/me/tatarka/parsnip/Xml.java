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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.tatarka.parsnip.annotations.XmlQualifier;

public class Xml {
    private static final String ERROR_FORMAT = "No %s for %s annotated %s";

    static final List<XmlAdapter.Factory> BUILT_IN_ADAPTER_FACTORIES = new ArrayList<>(2);
    static final List<TypeConverter.Factory> BUILT_IN_CONVERTER_FACTORIES = new ArrayList<>(1);

    static {
        BUILT_IN_ADAPTER_FACTORIES.add(TagXmlAdapter.FACTORY);
        BUILT_IN_ADAPTER_FACTORIES.add(ClassXmlAdapter.FACTORY);
        BUILT_IN_CONVERTER_FACTORIES.add(StandardTypeConverters.FACTORY);
    }

    private final XmlAdapters adapters;

    private Xml(Builder builder) {
        List<XmlAdapter.Factory> adapterFactories = new ArrayList<>(builder.adapterFactories.size() + BUILT_IN_ADAPTER_FACTORIES.size());
        adapterFactories.addAll(builder.adapterFactories);
        adapterFactories.addAll(BUILT_IN_ADAPTER_FACTORIES);
        List<TypeConverter.Factory> converterFactories = new ArrayList<>(builder.typeConverterFactories.size() + BUILT_IN_CONVERTER_FACTORIES.size());
        converterFactories.addAll(builder.typeConverterFactories);
        converterFactories.addAll(BUILT_IN_CONVERTER_FACTORIES);
        adapters = new XmlAdapters(adapterFactories, converterFactories);
    }

    public <T> XmlAdapter<T> adapter(Class<T> type) {
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Type type) {
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Type type, Set<? extends Annotation> annotations) {
        XmlAdapter<T> adapter = adapters.adapter(type, annotations);
        if (adapter == null) {
            throw new IllegalArgumentException(String.format(ERROR_FORMAT, "XmlAdapter", type, Util.NO_ANNOTATIONS));
        }
        return adapter;
    }

    public static final class Builder {
        private final List<me.tatarka.parsnip.XmlAdapter.Factory> adapterFactories = new ArrayList<>();
        private final List<me.tatarka.parsnip.TypeConverter.Factory> typeConverterFactories = new ArrayList<>();

        public <T> Builder add(final Type type, final XmlAdapter<T> xmlAdapter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (xmlAdapter == null) throw new IllegalArgumentException("xmlAdapter == null");

            return add(new me.tatarka.parsnip.XmlAdapter.Factory() {
                @Override
                public me.tatarka.parsnip.XmlAdapter<?> create(Type targetType, Set<? extends Annotation> annotations, XmlAdapters adapters) {
                    return annotations.isEmpty() && Util.typesMatch(type, targetType) ? xmlAdapter : null;
                }
            });
        }

        public <T> Builder add(final Type type, final Class<? extends Annotation> annotation, final XmlAdapter<T> xmlAdapter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (annotation == null) throw new IllegalArgumentException("annotation == null");
            if (xmlAdapter == null) throw new IllegalArgumentException("xmlAdapter == null");
            if (!annotation.isAnnotationPresent(XmlQualifier.class)) {
                throw new IllegalArgumentException(annotation + " does not have @XmlQualifier");
            }

            return add(new me.tatarka.parsnip.XmlAdapter.Factory() {
                @Override
                public me.tatarka.parsnip.XmlAdapter<?> create(Type targetType, Set<? extends Annotation> annotations, XmlAdapters adapters) {
                    if (!Util.typesMatch(type, targetType)) return null;
                    // TODO: check for an annotations exact match.
                    if (!Util.isAnnotationPresent(annotations, annotation))
                        return null;
                    return xmlAdapter;
                }
            });
        }

        public Builder add(me.tatarka.parsnip.XmlAdapter.Factory xmlAdapter) {
            // TODO: define precedence order. Last added wins? First added wins?
            adapterFactories.add(xmlAdapter);
            return this;
        }

        public <T> Builder add(final Type type, final me.tatarka.parsnip.TypeConverter<T> typeConverter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (typeConverter == null) throw new IllegalArgumentException("typeConverter == null");

            return add(new me.tatarka.parsnip.TypeConverter.Factory() {
                @Override
                public me.tatarka.parsnip.TypeConverter<?> create(Type targetType, Set<? extends Annotation> annotations) {
                    return annotations.isEmpty() && Util.typesMatch(type, targetType) ? typeConverter : null;
                }
            });
        }

        public <T> Builder add(final Type type, final Class<? extends Annotation> annotation, final me.tatarka.parsnip.TypeConverter<T> typeConverter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (annotation == null) throw new IllegalArgumentException("annotation == null");
            if (typeConverter == null) throw new IllegalArgumentException("typeConverter == null");
            if (!annotation.isAnnotationPresent(XmlQualifier.class)) {
                throw new IllegalArgumentException(annotation + " does not have @XmlQualifier");
            }

            return add(new me.tatarka.parsnip.TypeConverter.Factory() {
                @Override
                public me.tatarka.parsnip.TypeConverter<?> create(Type targetType, Set<? extends Annotation> annotations) {
                    if (!Util.typesMatch(type, targetType)) return null;
                    // TODO: check for an annotations exact match.
                    if (!Util.isAnnotationPresent(annotations, annotation))
                        return null;
                    return typeConverter;
                }
            });
        }

        public Builder add(final me.tatarka.parsnip.TypeConverter.Factory typeConverter) {
            typeConverterFactories.add(typeConverter);
            return this;
        }

        public Builder add(Object adapter) {
            return add(AdapterMethodsFactory.get(adapter));
        }

        public Xml build() {
            return new Xml(this);
        }
    }

}
