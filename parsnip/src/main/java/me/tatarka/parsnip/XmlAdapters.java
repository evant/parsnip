package me.tatarka.parsnip;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class XmlAdapters {
    private final List<XmlAdapter.Factory> factories;
    private final List<TypeConverter.Factory> typeConverterFactories;
    private final ThreadLocal<List<DeferredAdapter<?>>> reentrantCalls = new ThreadLocal<>();

    XmlAdapters(List<XmlAdapter.Factory> factories, List<TypeConverter.Factory> typeConverterFactories) {
        this.factories = Collections.unmodifiableList(factories);
        this.typeConverterFactories = Collections.unmodifiableList(typeConverterFactories);
    }

    public <T> XmlAdapter<T> adapter(Type type, Set<? extends Annotation> annotations) {
        return createAdapter(0, type, annotations);
    }

    public <T> XmlAdapter<T> nextAdapter(XmlAdapter.Factory skipPast, Type type, Set<? extends Annotation> annotations) {
        return createAdapter(factories.indexOf(skipPast) + 1, type, annotations);
    }

    /**
     * Promote the {@link XmlAdapter} to root by wrapping it in one that reads the root tag.
     *
     * @param name    the name of the root tag.
     * @param adapter the adapter to wrap.
     * @return a new adapter that will read the root tag.
     */
    public <T> XmlAdapter<T> root(String name, XmlAdapter<T> adapter) {
        return new RootAdapter<>(name, adapter);
    }

    public <T> TypeConverter<T> converter(Type type, Set<? extends Annotation> annotations) {
        return createConverter(0, type, annotations);
    }

    public <T> TypeConverter<T> nextConverter(TypeConverter.Factory skipPast, Type type, Set<? extends Annotation> annotations) {
        return createConverter(typeConverterFactories.indexOf(skipPast) + 1, type, annotations);
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
        return null;
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

    private static class RootAdapter<T> extends XmlAdapter<T> {
        private String name;
        private XmlAdapter<T> delegate;

        RootAdapter(String name, XmlAdapter<T> delegate) {
            this.name = name;
            this.delegate = delegate;
        }

        @Override
        public T fromXml(XmlReader reader) throws IOException {
            String name = reader.beginTag();
            if (!this.name.equals(name)) {
                throw new me.tatarka.parsnip.XmlDataException("Invalid root tag. Expected " + this.name + " but got " + name);
            }
            T result = delegate.fromXml(reader);
            reader.endTag();
            return result;
        }

        @Override
        public void toXml(XmlWriter writer, T value) throws IOException {
            writer.beginTag(name);
            delegate.toXml(writer, value);
            writer.endTag();
        }
    }
}
