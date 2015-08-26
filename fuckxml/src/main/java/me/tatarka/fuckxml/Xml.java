package me.tatarka.fuckxml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.tatarka.fuckxml.annottions.XmlQualifier;

public class Xml {
    private static final String ERROR_FORMAT = "No %s for %s annotated %s";

    private XmlAdapters xmlAdapters;

    private Xml(Builder builder) {
        List<XmlAdapter.Factory> adapterFactories = new ArrayList<>();
        List<TypeConverter.Factory> typeConverterFactories = new ArrayList<>();
        adapterFactories.addAll(builder.adapterFactories);
        adapterFactories.add(TagXmlAdapter.FACTORY);
        adapterFactories.add(ClassXmlAdapter.FACTORY);
        typeConverterFactories.addAll(builder.typeConverterFactories);
        typeConverterFactories.add(StandardTypeConverters.FACTORY);
        xmlAdapters = new XmlAdapters(adapterFactories, typeConverterFactories);
    }

    public <T> XmlAdapter<T> adapter(Class<T> type) {
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Type type) {
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Type type, Set<? extends Annotation> annotations) {
        XmlAdapter<T> adapter = xmlAdapters.adapter(type, annotations);
        if (adapter == null) {
            throw new IllegalArgumentException(String.format(ERROR_FORMAT, "XmlAdapter", type, Util.NO_ANNOTATIONS));
        }
        return xmlAdapters.root(Types.getRawType(type).getSimpleName(), adapter);
    }

    public static final class Builder {
        private final List<XmlAdapter.Factory> adapterFactories = new ArrayList<>();
        private final List<TypeConverter.Factory> typeConverterFactories = new ArrayList<>();

        public <T> Builder add(final Type type, final XmlAdapter<T> xmlAdapter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (xmlAdapter == null) throw new IllegalArgumentException("xmlAdapter == null");

            return add(new XmlAdapter.Factory() {
                @Override
                public XmlAdapter<?> create(Type targetType, Set<? extends Annotation> annotations, XmlAdapters adapters) {
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

            return add(new XmlAdapter.Factory() {
                @Override
                public XmlAdapter<?> create(Type targetType, Set<? extends Annotation> annotations, XmlAdapters adapters) {
                    if (!Util.typesMatch(type, targetType)) return null;
                    // TODO: check for an annotations exact match.
                    if (!Util.isAnnotationPresent(annotations, annotation)) return null;
                    return xmlAdapter;
                }
            });
        }

        public Builder add(XmlAdapter.Factory xmlAdapter) {
            // TODO: define precedence order. Last added wins? First added wins?
            adapterFactories.add(xmlAdapter);
            return this;
        }

        public <T> Builder add(final Type type, final TypeConverter<T> typeConverter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (typeConverter == null) throw new IllegalArgumentException("typeConverter == null");

            return add(new TypeConverter.Factory() {
                @Override
                public TypeConverter<?> create(Type targetType, Set<? extends Annotation> annotations) {
                    return annotations.isEmpty() && Util.typesMatch(type, targetType) ? typeConverter : null;
                }
            });
        }

        public <T> Builder add(final Type type, final Class<? extends Annotation> annotation, final TypeConverter<T> typeConverter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (annotation == null) throw new IllegalArgumentException("annotation == null");
            if (typeConverter == null) throw new IllegalArgumentException("typeConverter == null");
            if (!annotation.isAnnotationPresent(XmlQualifier.class)) {
                throw new IllegalArgumentException(annotation + " does not have @XmlQualifier");
            }

            return add(new TypeConverter.Factory() {
                @Override
                public TypeConverter<?> create(Type targetType, Set<? extends Annotation> annotations) {
                    if (!Util.typesMatch(type, targetType)) return null;
                    // TODO: check for an annotations exact match.
                    if (!Util.isAnnotationPresent(annotations, annotation)) return null;
                    return typeConverter;
                }
            });
        }

        public Builder add(final TypeConverter.Factory typeConverter) {
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
