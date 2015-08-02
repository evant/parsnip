package me.tatarka.fuckxml;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class ClassXmlAdapter<T> extends XmlAdapter<T> {
    /**
     * We need a special name for the text field, since we don't get that information from the xml
     **/
    static final String NAME_TEXT = "!com.fuckxml.TEXT";

    static final XmlAdapter.Factory FACTORY = new Factory() {
        @Override
        public XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, Xml xml) {
            Class<?> rawType = Types.getRawType(type);
            if (rawType.isInterface() || rawType.isEnum() || isPlatformType(rawType)) return null;
            if (!annotations.isEmpty()) return null;

            if (rawType.getEnclosingClass() != null && !Modifier.isStatic(rawType.getModifiers())) {
                if (rawType.getSimpleName().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Cannot serialize anonymous class " + rawType.getName());
                } else {
                    throw new IllegalArgumentException(
                            "Cannot serialize non-static nested class " + rawType.getName());
                }
            }
            if (Modifier.isAbstract(rawType.getModifiers())) {
                throw new IllegalArgumentException("Cannot serialize abstract class " + rawType.getName());
            }

            ClassFactory<Object> classFactory = ClassFactory.get(rawType);
            Map<String, FieldBinding> fields = new TreeMap<>();
            for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
                createFieldBindings(xml, t, fields);
            }
            return new ClassXmlAdapter<>(classFactory, fields);
        }

        /** Creates a field binding for each of declared field of {@code type}. */
        private void createFieldBindings(Xml xml, Type type, Map<String, FieldBinding> fieldBindings) {
            Class<?> rawType = Types.getRawType(type);
            boolean platformType = isPlatformType(rawType);
            for (Field field : rawType.getDeclaredFields()) {
                if (!includeField(platformType, field.getModifiers())) continue;

                field.setAccessible(true);
                // Look up a type adapter for this type.
                Type fieldType = Types.resolve(type, rawType, field.getGenericType());
                Set<? extends Annotation> annotations = Util.xmlAnnotations(field);

                // Create the binding between field and Xml.
                Class<?> rawFieldType = Types.getRawType(fieldType);
                XmlAdapter<Object> adapter;
                FieldBinding<?> fieldBinding;
                if (rawFieldType == List.class || rawFieldType == Collection.class || rawFieldType == Set.class) {
                    // Collections are weird in xml. A collection is multiple tags of the same name.
                    // However, they may be interspersed with other items. To handle this, we will
                    // just use the collection element type's adapter, and append it to the field's
                    // collection each time one is found.
                    Type elementType = Types.collectionElementType(fieldType, Collection.class);
                    CollectionFactory collectionFactory = rawFieldType == List.class || rawFieldType == Collection.class
                            ? ARRAY_LIST_COLLECTION_FACTORY : LINKED_HASH_SET_COLLECTION_FACTORY;
                    adapter = xml.adapter(elementType, annotations);
                    fieldBinding = new CollectionFieldBinding<>(field, adapter, collectionFactory);
                } else {
                    adapter = xml.adapter(fieldType, annotations);
                    fieldBinding = new SimpleFieldBinding<>(field, adapter);
                }

                String name = adapter instanceof TextXmlAdapter ? NAME_TEXT : field.getName();
                // Store it using the field's name. If there was already a field with this name, fail!
                FieldBinding replaced = fieldBindings.put(name, fieldBinding);
                if (replaced != null) {
                    throw new IllegalArgumentException("Field name collision: '" + field.getName() + "'"
                            + " declared by both " + replaced.field.getDeclaringClass().getName()
                            + " and superclass " + fieldBinding.field.getDeclaringClass().getName());
                }
            }
        }

        /**
         * Returns true if {@code rawType} is built in. We don't reflect on private fields of platform
         * types because they're unspecified and likely to be different on Java vs. Android.
         */
        private boolean isPlatformType(Class<?> rawType) {
            return rawType.getName().startsWith("java.")
                    || rawType.getName().startsWith("javax.")
                    || rawType.getName().startsWith("android.");
        }

        /** Returns true if fields with {@code modifiers} are included in the emitted JSON. */
        private boolean includeField(boolean platformType, int modifiers) {
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) return false;
            return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers) || !platformType;
        }
    };

    private final ClassFactory<T> classFactory;
    private final Map<String, FieldBinding> fields;

    private ClassXmlAdapter(ClassFactory<T> classFactory, Map<String, FieldBinding> fields) {
        this.classFactory = classFactory;
        this.fields = fields;
    }

    @Override
    public T fromXml(XmlReader reader) throws IOException {
        T result;
        try {
            result = classFactory.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException)
                throw (RuntimeException) targetException;
            if (targetException instanceof Error) throw (Error) targetException;
            throw new RuntimeException(targetException);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        
        for (FieldBinding fieldBinding : fields.values()) {
            try {
                fieldBinding.init(result);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        try {
            XmlReader.Token token = reader.peek();
            // Read the begin tag if it hasn't already (won't be on root)
            // TODO: validate root tag name?
            if (token == XmlReader.Token.BEGIN_TAG) {
                reader.beginTag();
                token = reader.peek();
            }
            while (token != XmlReader.Token.END_TAG) {
                switch (token) {
                    case ATTRIBUTE: {
                        String name = reader.nextAttribute();
                        FieldBinding fieldBinding = fields.get(name);
                        if (fieldBinding != null) {
                            fieldBinding.read(reader, result);
                        } else {
                            reader.skip();
                        }
                        break;
                    }
                    case TEXT: {
                        FieldBinding fieldBinding = fields.get(NAME_TEXT);
                        if (fieldBinding != null) {
                            fieldBinding.read(reader, result);
                        } else {
                            reader.skip();
                        }
                        break;
                    }
                    case BEGIN_TAG: {
                        String name = reader.beginTag();
                        FieldBinding fieldBinding = fields.get(name);
                        if (fieldBinding != null) {
                            fieldBinding.read(reader, result);
                        } else {
                            reader.skipTag();
                        }
                        break;
                    }
                    case END_DOCUMENT: {
                        throw new XmlDataException("Unexpected end of document");
                    }
                }
                token = reader.peek();
            }
            reader.endTag();
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    @Override
    public void toXml(XmlWriter write, T value) throws IOException {
        //TODO
    }

    private static abstract class FieldBinding<T> {
        final Field field;
        final XmlAdapter<T> adapter;

        protected FieldBinding(Field field, XmlAdapter<T> adapter) {
            this.field = field;
            this.adapter = adapter;
        }

        abstract void init(Object value) throws IllegalAccessException;

        abstract void read(XmlReader reader, Object value) throws IOException, IllegalAccessException;

        abstract void write(XmlWriter writer, Object value) throws IllegalAccessException, IOException;
    }

    private static class SimpleFieldBinding<T> extends FieldBinding<T> {
        SimpleFieldBinding(Field field, XmlAdapter<T> adapter) {
            super(field, adapter);
        }

        @Override
        void init(Object value) {
            // Nothing to do
        }

        @Override
        void read(XmlReader reader, Object value) throws IOException, IllegalAccessException {
            T fieldValue = adapter.fromXml(reader);
            field.set(value, fieldValue);
        }

        @Override
        @SuppressWarnings("unchecked")
            // We require that field's values are of type T.
        void write(XmlWriter writer, Object value) throws IllegalAccessException, IOException {
            T fieldValue = (T) field.get(value);
            adapter.toXml(writer, fieldValue);
        }
    }

    private static class CollectionFieldBinding<T> extends FieldBinding<T> {
        final CollectionFactory collectionFactory;

        CollectionFieldBinding(Field field, XmlAdapter<T> adapter, CollectionFactory collectionFactory) {
            super(field, adapter);
            this.collectionFactory = collectionFactory;
        }

        @Override
        @SuppressWarnings("unchecked")
        void init(Object value) throws IllegalAccessException {
            // Ensure field holds a collection.
            Collection<T> currentValue = (Collection<T>) field.get(value);
            if (currentValue == null) {
                field.set(value, collectionFactory.newCollection());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
            // We require that field's values are of type Collection<T>.
        void read(XmlReader reader, Object value) throws IOException, IllegalAccessException {
            T additionalValue = adapter.fromXml(reader);
            Collection<T> currentValue = (Collection<T>) field.get(value);
            currentValue.add(additionalValue);
        }

        @Override
        @SuppressWarnings("unchecked")
            // We require that field's values are of type Collection<T>.
        void write(XmlWriter writer, Object value) throws IllegalAccessException, IOException {
            Collection<T> fieldValue = (Collection<T>) field.get(value);
            if (fieldValue != null) {
                for (T singleValue : fieldValue) {
                    adapter.toXml(writer, singleValue);
                }
            }
        }
    }

    private static abstract class CollectionFactory {
        abstract <T> Collection<T> newCollection();
    }

    private static final CollectionFactory ARRAY_LIST_COLLECTION_FACTORY = new CollectionFactory() {
        @Override
        <C> ArrayList<C> newCollection() {
            return new ArrayList<>();
        }
    };

    private static final CollectionFactory LINKED_HASH_SET_COLLECTION_FACTORY = new CollectionFactory() {
        @Override
        <C> LinkedHashSet<C> newCollection() {
            return new LinkedHashSet<>();
        }
    };
}
