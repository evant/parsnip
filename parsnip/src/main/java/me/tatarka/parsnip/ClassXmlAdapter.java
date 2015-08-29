package me.tatarka.parsnip;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import me.tatarka.parsnip.annotations.SerializedName;
import me.tatarka.parsnip.annotations.Text;

final class ClassXmlAdapter<T> extends XmlAdapter<T> {

    // Unique name for text attribute, so it can be in the map with everything else.
    private static final String TEXT = "!me.tatarka.parsnip.text";

    static final Factory FACTORY = new Factory() {
        @Override
        public XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, XmlAdapters adapters) {
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

            ClassFactory<Object> classFactory = ClassFactory.get(rawType);
            Map<String, FieldBinding> fields = new TreeMap<>();
            Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces = new LinkedHashMap<>();
            for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
                createFieldBindings(adapters, t, fields, namespaces);
            }
            return new ClassXmlAdapter<>(classFactory, fields, namespaces);
        }

        /** Creates a field binding for each of declared field of {@code type}. */
        private void createFieldBindings(XmlAdapters adapters, Type type, Map<String, FieldBinding> fields, Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces) {
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
                    String name = getCollectionFieldName(field, rawElementType);
                    XmlAdapter<?> adapter = adapters.adapter(elementType, annotations);
                    fields.put(name, new CollectionFieldBinding<>(field, name, adapter, collectionFactory));
                } else {
                    if (field.isAnnotationPresent(Text.class)) {
                        TypeConverter<?> converter = adapters.converter(fieldType, annotations);
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
                        XmlAdapter<?> adapter = adapters.adapter(fieldType, annotations);
                        FieldBinding<?> fieldBinding;
                        String name = getFieldName(field, namespaces);
                        if (adapter != null) {
                            fieldBinding = new TagFieldBinding<>(field, name, adapter);
                        } else {
                            TypeConverter<?> converter = adapters.converter(fieldType, annotations);
                            if (converter == null) {
                                throw new IllegalArgumentException("No XmlAdapter or TypeConverter for type " + fieldType + " and annotations " + annotations);
                            }
                            me.tatarka.parsnip.annotations.Namespace namespace = field.getAnnotation(me.tatarka.parsnip.annotations.Namespace.class);
                            fieldBinding = new AttributeFieldBinding<>(field, name, namespace, converter, namespaces);
                            name = appendNamespace(name, namespace);
                        }
                        FieldBinding replaced = fields.put(name, fieldBinding);
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

        /**
         * Returns the field name, taking into account the @SerializeName and @Namespace annotations.
         */
        private String getFieldName(Field field, Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces) {
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            me.tatarka.parsnip.annotations.Namespace namespace = field.getAnnotation(me.tatarka.parsnip.annotations.Namespace.class);
            if (namespace != null) {
                namespaces.put(namespace, null);
            }
            if (serializedName != null) {
                return serializedName.value();
            } else {
                return field.getName();
            }
        }

        private String appendNamespace(String name, me.tatarka.parsnip.annotations.Namespace namespace) {
            if (namespace == null) {
                return name;
            }
            return "{" + namespace.value() + "}" + name;
        }

        /**
         * Returns the collection field name, taking into account @Serialize name. Uses the name of
         * the collection type by default, as that is likely to be singular as opposed to the field
         * name which is likely to be plural.
         */
        private String getCollectionFieldName(Field field, Class<?> rawElementType) {
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName != null) {
                return serializedName.value();
            } else {
                serializedName = rawElementType.getAnnotation(SerializedName.class);
                if (serializedName != null) {
                    return serializedName.value();
                } else {
                    return rawElementType.getSimpleName();
                }
            }
        }
    };

    private final me.tatarka.parsnip.ClassFactory<T> classFactory;
    private final Map<String, FieldBinding> fields;
    private final Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces;
    // List of fields orders with attributes first.
    private final List<FieldBinding> fieldList;

    private ClassXmlAdapter(me.tatarka.parsnip.ClassFactory<T> classFactory, Map<String, FieldBinding> fields, Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces) {
        this.classFactory = classFactory;
        this.fields = fields;
        this.namespaces = namespaces;

        fieldList = new ArrayList<>(fields.size());
        for (FieldBinding fieldBinding : fields.values()) {
            if (fieldBinding instanceof AttributeFieldBinding) {
                fieldList.add(0, fieldBinding);
            } else {
                fieldList.add(fieldBinding);
            }
        }
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
                if (fieldBinding instanceof CollectionFieldBinding) {
                    ((CollectionFieldBinding) fieldBinding).init(result);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        try {
            Namespace namespace = new Namespace();
            XmlReader.Token token = reader.peek();
            while (token != XmlReader.Token.END_TAG) {
                switch (token) {
                    case ATTRIBUTE: {
                        String name = reader.nextAttribute(namespace);
                        FieldBinding fieldBinding = getFieldBinding(name, namespace);
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
            if (!namespaces.isEmpty()) {
                for (me.tatarka.parsnip.annotations.Namespace namespace : namespaces.keySet()) {
                    Namespace ns = new Namespace();
                    writer.namespace(ns, namespace.alias(), namespace.value());
                    namespaces.put(namespace, ns);
                }
            }

            for (FieldBinding fieldBinding : fieldList) {
                fieldBinding.write(writer, value);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private FieldBinding getFieldBinding(String name, Namespace namespace) {
        if (namespace.namespace == null) {
            return fields.get(name);
        } else {
            return fields.get("{" + namespace.namespace + "}" + name);
        }
    }

    private static abstract class FieldBinding<T> {
        final Field field;

        FieldBinding(Field field) {
            this.field = field;
        }

        void read(XmlReader reader, Object value) throws IOException, IllegalAccessException {
            Object fieldValue = readValue(reader);
            field.set(value, fieldValue);
        }

        @SuppressWarnings("unchecked")
            // We require that field's values are of type T.
        void write(XmlWriter writer, Object value) throws IllegalAccessException, IOException {
            T fieldValue = (T) field.get(value);
            writeValue(writer, fieldValue);
        }

        abstract T readValue(XmlReader reader) throws IOException;

        abstract void writeValue(XmlWriter writer, T value) throws IOException;
    }

    private static class TagFieldBinding<T> extends FieldBinding<T> {
        final String name;
        final XmlAdapter<T> adapter;

        TagFieldBinding(Field field, String name, XmlAdapter<T> adapter) {
            super(field);
            this.name = name;
            this.adapter = adapter;
        }

        @Override
        T readValue(XmlReader reader) throws IOException {
            T value = adapter.fromXml(reader);
            reader.endTag();
            return value;
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            writer.beginTag(name);
            adapter.toXml(writer, value);
            writer.endTag();
        }
    }

    private static class AttributeFieldBinding<T> extends FieldBinding<T> {
        final String name;
        final me.tatarka.parsnip.annotations.Namespace namespace;
        final TypeConverter<T> converter;
        final Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces;

        AttributeFieldBinding(Field field, String name, me.tatarka.parsnip.annotations.Namespace namespace, TypeConverter<T> converter, Map<me.tatarka.parsnip.annotations.Namespace, Namespace> namespaces) {
            super(field);
            this.name = name;
            this.namespace = namespace;
            this.converter = converter;
            this.namespaces = namespaces;
        }

        @Override
        T readValue(XmlReader reader) throws IOException {
            return converter.from(reader.nextValue());
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            if (namespace == null) {
                writer.name(name);
            } else {
                Namespace ns = namespaces.get(namespace);
                writer.name(ns, name);
            }
            writer.value(converter.to(value));
        }
    }

    private static class TextFieldBinding<T> extends FieldBinding<T> {
        final TypeConverter<T> converter;

        TextFieldBinding(Field field, TypeConverter<T> converter) {
            super(field);
            this.converter = converter;
        }

        @Override
        T readValue(XmlReader reader) throws IOException {
            return converter.from(reader.nextText());
        }

        @Override
        void writeValue(XmlWriter writer, T value) throws IOException {
            writer.text(converter.to(value));
        }
    }

    private static class CollectionFieldBinding<T> extends TagFieldBinding<T> {
        final CollectionFactory collectionFactory;

        CollectionFieldBinding(Field field, String name, XmlAdapter<T> adapter, CollectionFactory collectionFactory) {
            super(field, name, adapter);
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
        void read(XmlReader reader, Object value) throws IOException, IllegalAccessException {
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
