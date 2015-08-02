package me.tatarka.fuckxml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import me.tatarka.fuckxml.annottions.XmlQualifier;

/**
 * Created by evan on 7/3/15.
 */
public class Xml {
    private final List<XmlAdapter.Factory> factories;
    private final ThreadLocal<List<DeferredAdapter<?>>> reentrantCalls = new ThreadLocal<>();

    private Xml(Builder builder) {
        List<XmlAdapter.Factory> factories = new ArrayList<>();
        factories.addAll(builder.factories);
        factories.add(StandardXmlAdapters.FACTORY);   
        factories.add(ClassXmlAdapter.FACTORY);
        this.factories = Collections.unmodifiableList(factories);
    }

    /**
     * Returns a JSON adapter for {@code type}, creating it if necessary.
     */
    public <T> XmlAdapter<T> adapter(Type type) {
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Class<T> type) {
        // TODO: cache created JSON adapters.
        return adapter(type, Util.NO_ANNOTATIONS);
    }

    public <T> XmlAdapter<T> adapter(Type type, Set<? extends Annotation> annotations) {
        return createAdapter(0, type, annotations);
    }

    public <T> XmlAdapter<T> nextAdapter(XmlAdapter.Factory skipPast, Type type, Set<? extends Annotation> annotations) {
        return createAdapter(factories.indexOf(skipPast) + 1, type, annotations);
    }

    @SuppressWarnings("unchecked") // Factories are required to return only matching XmlAdapters.
    private <T> XmlAdapter<T> createAdapter(int firstIndex, Type type, Set<? extends Annotation> annotations) {
        List<DeferredAdapter<?>> deferredAdapters = reentrantCalls.get();
        if (deferredAdapters == null) {
            deferredAdapters = new ArrayList<>();
            reentrantCalls.set(deferredAdapters);
        } else if (firstIndex == 0) {
            // If this is a regular adapter lookup, check that this isn't a reentrant call.
            for (DeferredAdapter<?> deferredAdapter : deferredAdapters) {
                if (deferredAdapter.type.equals(type) && deferredAdapter.annotations.equals(annotations)) {
                    return (XmlAdapter<T>) deferredAdapter;
                }
            }
        }

        DeferredAdapter<T> deferredAdapter = new DeferredAdapter<>(type, annotations);
        deferredAdapters.add(deferredAdapter);
        try {
            for (int i = firstIndex, size = factories.size(); i < size; i++) {
                XmlAdapter<T> result = (XmlAdapter<T>) factories.get(i).create(type, annotations, this);
                if (result != null) {
                    deferredAdapter.ready(result);
                    return result;
                }
            }
        } finally {
            deferredAdapters.remove(deferredAdapters.size() - 1);
        }

        throw new IllegalArgumentException("No XmlAdapter for " + type + " annotated " + annotations);
    }

    public static final class Builder {
        private final List<XmlAdapter.Factory> factories = new ArrayList<>();

        public <T> Builder add(final Type type, final XmlAdapter<T> xmlAdapter) {
            if (type == null) throw new IllegalArgumentException("type == null");
            if (xmlAdapter == null) throw new IllegalArgumentException("xmlAdapter == null");

            return add(new XmlAdapter.Factory() {
                @Override
                public XmlAdapter<?> create(Type targetType, Set<? extends Annotation> annotations, Xml xml) {
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
                public XmlAdapter<?> create(
                        Type targetType, Set<? extends Annotation> annotations, Xml xml) {
                    if (!Util.typesMatch(type, targetType)) return null;

                    // TODO: check for an annotations exact match.
                    if (!Util.isAnnotationPresent(annotations, annotation)) return null;

                    return xmlAdapter;
                }
            });
        }

        public Builder add(XmlAdapter.Factory xmlAdapter) {
            // TODO: define precedence order. Last added wins? First added wins?
            factories.add(xmlAdapter);
            return this;
        }

        public Builder add(Object adapter) {
            return add(AdapterMethodsFactory.get(adapter));
        }

        public Xml build() {
            return new Xml(this);
        }
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
        private Type type;
        private Set<? extends Annotation> annotations;
        private XmlAdapter<T> delegate;

        public DeferredAdapter(Type type, Set<? extends Annotation> annotations) {
            this.type = type;
            this.annotations = annotations;
        }

        public void ready(XmlAdapter<T> delegate) {
            this.delegate = delegate;

            // Null out the type and annotations so they can be garbage collected.
            this.type = null;
            this.annotations = null;
        }

        @Override
        public T fromXml(XmlReader reader) throws IOException {
            if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
            return delegate.fromXml(reader);
        }

        @Override
        public void toXml(XmlWriter writer, T value) throws IOException {
            if (delegate == null) throw new IllegalStateException("Type adapter isn't ready");
            delegate.toXml(writer, value);
        }
    }
}
