package me.tatarka.parsnip;

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

import me.tatarka.parsnip.annottions.Text;

final class ClassXmlAdapter<T> extends me.tatarka.parsnip.XmlAdapter<T> {
    // Unique name for text attribute, so it can be in the map with everything else. 
    private static final String TEXT = "!me.tatarka.parsnip.text";

    static final Factory FACTORY = new Factory() {
        @Override
        public me.tatarka.parsnip.XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, XmlAdapters adapters) {
            Class<?> rawType = Types.getRawType(type);
            if (rawType.isInterface() || rawType.isEnum() || isPlatformType(rawType) || rawType.isPrimitive())
                return null;
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

            me.tatarka.parsnip.ClassFactory<Object> classFactory = me.tatarka.parsnip.ClassFactory.get(rawType);
            Map<String, FieldBinding> fields = new TreeMap<>();
            for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
                createFieldBindings(adapters, t, fields);
            }
            return new ClassXmlAdapter<>(classFactory, fields);
        }

        /** Creates a field binding for each of declared field of {@code type}. */
        private void createFieldBindings(XmlAdapters adapters, Type type, Map<String, FieldBinding> fields) {
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
                if (rawFieldType == List.class || rawFieldType == Collection.class || rawFieldType == Set.class) {
                    // Collections are weird in xml. A collection is multiple tags of the same name.
                    // However, they may be interspersed with other items. To handle this, we will
                    // just use the collection element type's adapter, and append it to the field's
                    // collection each time one is found.
                    Type elementType = Types.collectionElementType(fieldType, Collection.class);
                    Class<?> rawElementType = Types.getRawType(elementType);
                    CollectionFactory collectionFactory = rawFieldType == List.class || rawFieldType == Collection.class
                            ? ARRAY_LIST_COLLECTION_FACTORY : LINKED_HASH_SET_COLLECTION_FACTORY;
                    me.tatarka.parsnip.XmlAdapter<?> adapter = adapters.adapter(elementType, annotations);
                    fields.put(rawElementType.getSimpleName(), new CollectionFieldBinding<>(field, adapter, collectionFactory));
                } else {
                    if (field.isAnnotationPresent(Text.class)) {
                        me.tatarka.parsnip.TypeConverter<?> converter = adapters.converter(fieldType, annotations);
                        if (converter == null) {
                            throw new IllegalArgumentException("No TypeConverter for type " + fieldType + " and annotations " + annotations);
                        }
                        TextFieldBinding<?> fieldBinding = new TextFieldBinding<>(field, converter);
                        FieldBinding replaced = fields.put(TEXT, fieldBinding);
                        if (replaced != null) {
                            throw new IllegalArgumentException("Text annotation collision: @Text is on both '"
                                    + field.getName() + "' and '" + replaced.field.getName() + "'.");
                        }
                    } else {
                        me.tatarka.parsnip.XmlAdapter<?> adapter = adapters.adapter(fieldType, annotations);
                        FieldBinding<?> fieldBinding;
                        if (adapter != null) {
                            fieldBinding = new TagFieldBinding<>(field, adapter);
                        } else {
                            me.tatarka.parsnip.TypeConverter<?> converter = adapters.converter(fieldType, annotations);
                            if (converter == null) {
                                throw new IllegalArgumentException("No XmlAdapter or TypeConverter for type " + fieldType + " and annotations " + annotations);
                            }
                            fieldBinding = new AttributeFieldBinding<>(field, converter);
                        }
                        FieldBinding replaced = fields.put(field.getName(), fieldBinding);
                        // Store it using the field's name. If there was already a field with this name, fail!
                        if (replaced != null) {
                            throw new IllegalArgumentException("Field name collision: '" + field.getName() + "'"
                                    + " declared by both " + replaced.field.getDeclaringClass().getName()
                                    + " and superclass " + fieldBinding.field.getDeclaringClass().getName());
                        }
                    }
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

    private final me.tatarka.parsnip.ClassFactory<T> classFactory;
    private final Map<String, FieldBinding> fields;

    private ClassXmlAdapter(me.tatarka.parsnip.ClassFactory<T> classFactory, Map<String, FieldBinding> fields) {
        this.classFactory = classFactory;
        this.fields = fields;
    }

    @Override
    public T fromXml(me.tatarka.parsnip.XmlReader reader) throws IOException {
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
                if (fieldBinding instanceof CollectionFieldBinding) {
                    ((CollectionFieldBinding) fieldBinding).init(result);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        try {
            me.tatarka.parsnip.XmlReader.Token token = reader.peek();
            while (token != me.tatarka.parsnip.XmlReader.Token.END_TAG) {
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
                        String name = TEXT;
                        FieldBinding fieldBinding = fields.get(name);
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
                        throw new me.tatarka.parsnip.XmlDataException("Unexpected end of document");
                    }
                }
                token = reader.peek();
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    @Override
    public void toXml(XmlWriter writer, T value) throws IOException {
        try {
            for (Map.Entry<String, FieldBinding> entry : fields.entrySet()) {
                FieldBinding fieldBinding = entry.getValue();
                fieldBinding.write(writer, value);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static abstract class FieldBinding<T> {
        final Field field;

        FieldBinding(Field field) {
            this.field = field;
        }

        void read(me.tatarka.parsnip.XmlReader reader, Object value) throws IOException, IllegalAccessException {
            Object fieldValue = readValue(reader);
            field.set(value, fieldValue);
        }

        @SuppressWarnings("unchecked")
            // We require that field's values are of type T.
        void write(XmlWriter writer, Object value) throws IllegalAccessException, IOException {
            T fieldValue = (T) field.get(value);
            writeValue(writer, fieldValue);
        }

        abstract T readValue(me.tatarka.parsnip.XmlReader reader) throws IOException;

        abstract void writeValue(XmlWriter writer, T value) throws IOException;
    }

    private static class TagFieldBinding<T> extends FieldBinding<T> {
        final me.tatarka.parsnip.XmlAdapter<T> adapter;

        TagFieldBinding(Field field, me.tatarka.parsnip.XmlAdapter<T> adapter) {
            super(field);
            this.adapter = adapter;
        }

        @Override
        T readValue(me.tatarka.parsnip.XmlReader reader) throws IOException {
            T value = adapter.fromXml(reader);
            reader.endTag();
            return value;
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            writer.beginTag(field.getName());
            adapter.toXml(writer, value);
            writer.endTag();
        }
    }

    private static class AttributeFieldBinding<T> extends FieldBinding<T> {
        final me.tatarka.parsnip.TypeConverter<T> converter;

        AttributeFieldBinding(Field field, me.tatarka.parsnip.TypeConverter<T> converter) {
            super(field);
            this.converter = converter;
        }

        @Override
        T readValue(me.tatarka.parsnip.XmlReader reader) throws IOException {
            return converter.from(reader.nextValue());
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            writer.name(field.getName()).value(converter.to(value));
        }
    }

    private static class TextFieldBinding<T> extends FieldBinding<T> {
        final me.tatarka.parsnip.TypeConverter<T> converter;

        TextFieldBinding(Field field, me.tatarka.parsnip.TypeConverter<T> converter) {
            super(field);
            this.converter = converter;
        }

        @Override
        T readValue(me.tatarka.parsnip.XmlReader reader) throws IOException {
            return converter.from(reader.nextText());
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            writer.text(converter.to(value));
        }
    }

    private static class CollectionFieldBinding<T> extends TagFieldBinding<T> {
        final CollectionFactory collectionFactory;

        CollectionFieldBinding(Field field, me.tatarka.parsnip.XmlAdapter<T> adapter, CollectionFactory collectionFactory) {
            super(field, adapter);
            this.collectionFactory = collectionFactory;
        }

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
        void read(me.tatarka.parsnip.XmlReader reader, Object value) throws IOException, IllegalAccessException {
            T additionalValue = readValue(reader);
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
                    writeValue(writer, singleValue);
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
