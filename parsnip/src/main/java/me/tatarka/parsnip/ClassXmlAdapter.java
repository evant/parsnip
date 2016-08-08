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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.tatarka.parsnip.annotations.Namespace;
import me.tatarka.parsnip.annotations.SerializedName;
import me.tatarka.parsnip.annotations.Text;

final class ClassXmlAdapter<T> extends XmlAdapter<T> {

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
            SerializedName serializedName = rawType.getAnnotation(SerializedName.class);
            String name = serializedName != null ? serializedName.value() : rawType.getSimpleName();
            Namespace namespace = rawType.getAnnotation(Namespace.class);
            TagInfo tagInfo = new TagInfo(name, namespace);
            ArrayList<AttributeFieldBinding> attributes = new ArrayList<>();
            ArrayList<TagFieldBinding> tags = new ArrayList<>();
            // Only a single text, but this makes it easier to check for duplicates
            ArrayList<TextFieldBinding> text = new ArrayList<>(1);
            for (Type t = type; t != Object.class; t = Types.getGenericSuperclass(t)) {
                createFieldBindings(adapters, t, attributes, tags, text);
            }
            return new ClassXmlAdapter<>(classFactory, tagInfo, attributes, tags, text.isEmpty() ? null : text.get(0));
        }

        /** Creates a field binding for each of declared field of {@code type}. */
        private void createFieldBindings(XmlAdapters adapters, Type type, ArrayList<AttributeFieldBinding> attributes, ArrayList<TagFieldBinding> tags, ArrayList<TextFieldBinding> text) {
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
                    String name = getFieldName(field);
                    Namespace namespace = getNamespace(field);
                    XmlAdapter<?> adapter = adapters.adapter(rawElementType, annotations);
                    tags.add(new CollectionFieldBinding<>(field, name, namespace, adapter, collectionFactory));
                } else {
                    if (field.isAnnotationPresent(Text.class)) {
                        TypeConverter<?> converter = adapters.converter(fieldType, annotations);
                        if (converter == null) {
                            throw new IllegalArgumentException("No TypeConverter for type " + fieldType + " and annotations " + annotations);
                        }
                        TextFieldBinding<?> fieldBinding = new TextFieldBinding<>(field, converter);
                        if (!text.isEmpty()) {
                            FieldBinding replaced = tags.get(0);
                            throw new IllegalArgumentException("Text annotation collision: @Text is on both '"
                                    + field.getName() + "' and '" + replaced.field.getName() + "'.");
                        }
                        text.add(fieldBinding);
                    } else {
                        XmlAdapter<?> adapter = adapters.adapter(fieldType, annotations);
                        String name = getFieldName(field);
                        Namespace namespace = getNamespace(field);
                        String ns = namespace == null ? null : namespace.value();
                        if (adapter != null) {
                            TagFieldBinding<?> fieldBinding = new TagFieldBinding<>(field, name, namespace, adapter);
                            FieldBinding replaced = getFieldBindingTags(tags, name, ns);
                            // Store it using the field's name. If there was already a field with this name, fail!
                            if (replaced != null) {
                                throw new IllegalArgumentException("Field name collision: '" + field.getName() + "'"
                                        + " declared by both " + replaced.field.getDeclaringClass().getName()
                                        + " and superclass " + fieldBinding.field.getDeclaringClass().getName());
                            }
                            tags.add(fieldBinding);
                        } else {
                            TypeConverter<?> converter = adapters.converter(fieldType, annotations);
                            if (converter == null) {
                                throw new IllegalArgumentException("No XmlAdapter or TypeConverter for type " + fieldType + " and annotations " + annotations);
                            }
                            AttributeFieldBinding<?> fieldBinding = new AttributeFieldBinding<>(field, name, namespace, converter);
                            FieldBinding replaced = getFieldBindingAttributes(attributes, name, ns);
                            // Store it using the field's name. If there was already a field with this name, fail!
                            if (replaced != null) {
                                throw new IllegalArgumentException("Field name collision: '" + field.getName() + "'"
                                        + " declared by both " + replaced.field.getDeclaringClass().getName()
                                        + " and superclass " + fieldBinding.field.getDeclaringClass().getName());
                            }
                            attributes.add(fieldBinding);
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
         * Returns the field name, taking into account the @SerializeName annotation.
         */
        private String getFieldName(Field field) {
            SerializedName serializedName = field.getAnnotation(SerializedName.class);
            if (serializedName != null) {
                return serializedName.value();
            } else {
                return field.getName();
            }
        }

        /**
         * Returns the field namespace, if it exists.
         */
        private Namespace getNamespace(Field field) {
            return field.getAnnotation(Namespace.class);
        }
    };

    private final me.tatarka.parsnip.ClassFactory<T> classFactory;
    private final TagInfo tagInfo;
    private final ArrayList<AttributeFieldBinding> attributes;
    private final ArrayList<TagFieldBinding> tags;
    private final TextFieldBinding text;
    // Namespaces to declare when writing.
    private LinkedHashSet<TagInfo> declareNamespaces;

    private ClassXmlAdapter(me.tatarka.parsnip.ClassFactory<T> classFactory, TagInfo tagInfo, ArrayList<AttributeFieldBinding> attributes, ArrayList<TagFieldBinding> tags, TextFieldBinding text) {
        this.classFactory = classFactory;
        this.tagInfo = tagInfo;
        this.attributes = attributes;
        this.tags = tags;
        this.text = text;
    }

    LinkedHashSet<TagInfo> getDeclaredNamespaces() {
        if (declareNamespaces == null) {
            declareNamespaces = initDeclaredNamespaces();
        }
        return declareNamespaces;
    }

    private LinkedHashSet<TagInfo> initDeclaredNamespaces() {
        LinkedHashSet<TagInfo> declareNamespaces = new LinkedHashSet<>();
        for (int i = 0, size = attributes.size(); i < size; i++) {
            TagInfo info = attributes.get(i).tagInfo;
            if (info.namespace() != null) {
                declareNamespaces.add(info);
            }
        }
        for (int i = 0, size = tags.size(); i < size; i++) {
            TagInfo info = tags.get(i).tagInfo;
            if (info.namespace() != null) {
                declareNamespaces.add(info);
            }
        }
        return declareNamespaces;
    }

    @Override
    public T fromXml(XmlPullParser parser, TagInfo tagInfo) throws XmlPullParserException, IOException {
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

        try {
            for (int i = 0, size = tags.size(); i < size; i++) {
                TagFieldBinding fieldBinding = tags.get(i);
                if (fieldBinding instanceof CollectionFieldBinding) {
                    ((CollectionFieldBinding) fieldBinding).init(result);
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }

        if (tagInfo == TagInfo.ROOT) {
            while (parser.next() != XmlPullParser.START_TAG) {
                // Read to start tag.
            }
        }

        try {
            if (!attributes.isEmpty()) {
                for (int i = 0, size = parser.getAttributeCount(); i < size; i++) {
                    String name = parser.getAttributeName(i);
                    FieldBinding fieldBinding = getFieldBindingAttributes(attributes, name, parser.getAttributeNamespace(i));
                    if (fieldBinding != null) {
                        fieldBinding.read(parser, i, result);
                    }
                }
            }

            loop:
            while (parser.next() != XmlPullParser.END_TAG) {
                switch (parser.getEventType()) {
                    case XmlPullParser.START_TAG: {
                        String name = parser.getName();
                        FieldBinding fieldBinding = getFieldBindingTags(tags, name, parser.getNamespace());
                        if (fieldBinding != null) {
                            fieldBinding.read(parser, 0, result);
                        } else {
                            skip(parser);
                        }
                        break;
                    }
                    case XmlPullParser.TEXT: {
                        FieldBinding fieldBinding = text;
                        if (fieldBinding != null) {
                            fieldBinding.read(parser, 0, result);
                        }
                        break;
                    }
                    case XmlPullParser.END_DOCUMENT:
                        break loop;
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws IOException, XmlPullParserException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    @Override
    public void toXml(XmlSerializer serializer, TagInfo tagInfo, T value) throws IOException {
        // Write declared namespaces for attributes and tags
        LinkedHashSet<TagInfo> declareNamespaces = getDeclaredNamespaces();
        if (!declareNamespaces.isEmpty()) {
            for (TagInfo info : declareNamespaces) {
                serializer.setPrefix(info.alias(), info.namespace());
            }
        }

        if (tagInfo == TagInfo.ROOT) {
            serializer.startTag(this.tagInfo.namespace(), this.tagInfo.name());
        } else {
            serializer.startTag(tagInfo.namespace(), tagInfo.name());
        }

        try {
            for (int i = 0, size = attributes.size(); i < size; i++) {
                FieldBinding fieldBinding = attributes.get(i);
                fieldBinding.write(serializer, value);
            }
            for (int i = 0, size = tags.size(); i < size; i++) {
                FieldBinding fieldBinding = tags.get(i);
                fieldBinding.write(serializer, value);
            }
            if (text != null) {
                text.write(serializer, value);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }

        if (tagInfo == TagInfo.ROOT) {
            serializer.endTag(this.tagInfo.namespace(), this.tagInfo.name());
        } else {
            serializer.endTag(tagInfo.namespace(), tagInfo.name());
        }
    }

    private static FieldBinding getFieldBindingTags(ArrayList<? extends TagFieldBinding> fields, String name, String namespace) {
        for (int i = 0, size = fields.size(); i < size; i++) {
            TagFieldBinding fieldBinding = fields.get(i);
            if (fieldBinding.tagInfo.name().equals(name) && nsEquals(fieldBinding.tagInfo.namespace(), namespace)) {
                return fieldBinding;
            }
        }
        return null;
    }

    private static FieldBinding getFieldBindingAttributes(ArrayList<? extends AttributeFieldBinding> fields, String name, String namespace) {
        for (int i = 0, size = fields.size(); i < size; i++) {
            AttributeFieldBinding fieldBinding = fields.get(i);
            if (fieldBinding.tagInfo.name().equals(name) && nsEquals(fieldBinding.tagInfo.namespace(), namespace)) {
                return fieldBinding;
            }
        }
        return null;
    }

    private static boolean nsEquals(String expected, String actual) {
        // All namespaces match if none expected.
        return expected == null || expected.equals(actual);
    }

    private static abstract class FieldBinding<T> {
        final Field field;

        FieldBinding(Field field) {
            this.field = field;
        }

        void read(XmlPullParser parser, int index, Object value) throws XmlPullParserException, IOException, IllegalAccessException {
            Object fieldValue = readValue(parser, index);
            field.set(value, fieldValue);
        }

        @SuppressWarnings("unchecked")
            // We require that field's values are of type T.
        void write(XmlSerializer serializer, Object value) throws IllegalAccessException, IOException {
            T fieldValue = (T) field.get(value);
            writeValue(serializer, fieldValue);
        }

        abstract T readValue(XmlPullParser parser, int index) throws XmlPullParserException, IOException;

        abstract void writeValue(XmlSerializer serializer, T value) throws IOException;
    }

    private static class TagFieldBinding<T> extends FieldBinding<T> {
        private TagInfo tagInfo;
        final XmlAdapter<T> adapter;

        TagFieldBinding(Field field, String name, Namespace namespace, XmlAdapter<T> adapter) {
            super(field);
            this.tagInfo = new TagInfo(name, namespace);
            this.adapter = adapter;
        }

        @Override
        T readValue(XmlPullParser parser, int index) throws XmlPullParserException, IOException {
            return adapter.fromXml(parser, tagInfo);
        }

        @Override
        void writeValue(XmlSerializer serializer, T value) throws IOException {
            adapter.toXml(serializer, tagInfo, value);
        }
    }

    private static class AttributeFieldBinding<T> extends FieldBinding<T> {
        final TagInfo tagInfo;
        final TypeConverter<T> converter;

        AttributeFieldBinding(Field field, String name, Namespace namespace, TypeConverter<T> converter) {
            super(field);
            this.tagInfo = new TagInfo(name, namespace);
            this.converter = converter;
        }

        @Override
        T readValue(XmlPullParser parser, int index) throws IOException {
            return converter.from(parser.getAttributeValue(index));
        }

        @Override
        void writeValue(XmlSerializer serializer, T value) throws IOException {
            String strValue = converter.to(value);
            if (strValue != null) {
                serializer.attribute(tagInfo.namespace(), tagInfo.name(), strValue);
            }
        }
    }

    private static class TextFieldBinding<T> extends FieldBinding<T> {
        final TypeConverter<T> converter;

        TextFieldBinding(Field field, TypeConverter<T> converter) {
            super(field);
            this.converter = converter;
        }

        @Override
        T readValue(XmlPullParser parser, int index) throws IOException, XmlPullParserException {
            return converter.from(parser.getText());
        }

        @Override
        void writeValue(XmlSerializer serializer, T value) throws IOException {
            serializer.text(converter.to(value));
        }
    }

    private static class CollectionFieldBinding<T> extends TagFieldBinding<T> {
        final CollectionFactory collectionFactory;

        CollectionFieldBinding(Field field, String name, Namespace namespace, XmlAdapter<T> adapter, CollectionFactory collectionFactory) {
            super(field, name, namespace, adapter);
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
        void read(XmlPullParser parser, int index, Object value) throws IOException, IllegalAccessException, XmlPullParserException {
            T additionalValue = readValue(parser, index);
            Collection<T> currentValue = (Collection<T>) field.get(value);
            currentValue.add(additionalValue);
        }

        @Override
        @SuppressWarnings("unchecked")
            // We require that field's values are of type Collection<T>.
        void write(XmlSerializer serializer, Object value) throws IllegalAccessException, IOException {
            Collection<T> fieldValue = (Collection<T>) field.get(value);
            if (fieldValue != null) {
                for (T singleValue : fieldValue) {
                    writeValue(serializer, singleValue);
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
