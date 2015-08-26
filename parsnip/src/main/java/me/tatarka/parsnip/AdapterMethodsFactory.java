/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.tatarka.parsnip.annotations.FromXml;
import me.tatarka.parsnip.annotations.ToXml;

final class AdapterMethodsFactory implements me.tatarka.parsnip.XmlAdapter.Factory {
    private final List<AdapterMethod> toAdapters;
    private final List<AdapterMethod> fromAdapters;

    public AdapterMethodsFactory(List<AdapterMethod> toAdapters, List<AdapterMethod> fromAdapters) {
        this.toAdapters = toAdapters;
        this.fromAdapters = fromAdapters;
    }

    @Override
    public me.tatarka.parsnip.XmlAdapter<?> create(Type type, Set<? extends Annotation> annotations, final XmlAdapters adapters) {
        final AdapterMethod toAdapter = get(toAdapters, type, annotations);
        final AdapterMethod fromAdapter = get(fromAdapters, type, annotations);
        if (toAdapter == null && fromAdapter == null) return null;

        final me.tatarka.parsnip.XmlAdapter<Object> delegate;
        if (toAdapter == null || fromAdapter == null) {
            delegate = adapters.nextAdapter(this, type, annotations);
            if (delegate == null) {
                String missingAnnotation = toAdapter == null ? "@ToXml" : "@FromXml";
                throw new IllegalArgumentException("No " + missingAnnotation + " adapter for "
                        + type + " annotated " + annotations);
            }
        } else {
            delegate = null;
        }

        return new me.tatarka.parsnip.XmlAdapter<Object>() {
            @Override
            public Object fromXml(me.tatarka.parsnip.XmlReader reader) throws IOException {
                if (fromAdapter == null) {
                    return delegate.fromXml(reader);
                } else {
                    try {
                        return fromAdapter.fromXml(adapters, reader);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError();
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
                        throw new XmlDataException(e.getCause() + " at " + reader.getPath());
                    }
                }
            }

            @Override
            public void toXml(me.tatarka.parsnip.XmlWriter writer, Object value) throws IOException {
                if (toAdapter == null) {
                    delegate.toXml(writer, value);
                } else {
                    try {
                        toAdapter.toXml(adapters, writer, value);
                    } catch (IllegalAccessException e) {
                        throw new AssertionError();
                    } catch (InvocationTargetException e) {
                        if (e.getCause() instanceof IOException) throw (IOException) e.getCause();
                        throw new XmlDataException(e.getCause() + " at " + "writer.getPath()");
                    }
                }
            }
        };
    }

    public static AdapterMethodsFactory get(Object adapter) {
        List<AdapterMethod> toAdapters = new ArrayList<>();
        List<AdapterMethod> fromAdapters = new ArrayList<>();

        for (Class<?> c = adapter.getClass(); c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(ToXml.class)) {
                    AdapterMethod toAdapter = toAdapter(adapter, m);
                    AdapterMethod conflicting = get(toAdapters, toAdapter.type, toAdapter.annotations);
                    if (conflicting != null) {
                        throw new IllegalArgumentException("Conflicting @ToXml methods:\n"
                                + "    " + conflicting.method + "\n"
                                + "    " + toAdapter.method);
                    }
                    toAdapters.add(toAdapter);
                }

                if (m.isAnnotationPresent(FromXml.class)) {
                    AdapterMethod fromAdapter = fromAdapter(adapter, m);
                    AdapterMethod conflicting = get(fromAdapters, fromAdapter.type, fromAdapter.annotations);
                    if (conflicting != null) {
                        throw new IllegalArgumentException("Conflicting @FromXml methods:\n"
                                + "    " + conflicting.method + "\n"
                                + "    " + fromAdapter.method);
                    }
                    fromAdapters.add(fromAdapter);
                }
            }
        }

        if (toAdapters.isEmpty() && fromAdapters.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one @ToJson or @FromJson method on "
                    + adapter.getClass().getName());
        }

        return new AdapterMethodsFactory(toAdapters, fromAdapters);
    }

    /**
     * Returns an object that calls a {@code method} method on {@code adapter} in service of
     * converting an object to JSON.
     */
    static AdapterMethod toAdapter(Object adapter, Method method) {
        method.setAccessible(true);
        Type[] parameterTypes = method.getGenericParameterTypes();
        final Type returnType = method.getGenericReturnType();

        if (parameterTypes.length == 2
                && parameterTypes[0] == me.tatarka.parsnip.XmlWriter.class
                && returnType == void.class) {
            // public void pointToXml(XmlWriter writer, Point point) throws Exception {
            Set<? extends Annotation> parameterAnnotations
                    = me.tatarka.parsnip.Util.xmlAnnotations(method.getParameterAnnotations()[1]);
            return new AdapterMethod(parameterTypes[1], parameterAnnotations, adapter, method, false) {
                @Override
                public void toXml(XmlAdapters adapters, me.tatarka.parsnip.XmlWriter writer, Object value)
                        throws IOException, InvocationTargetException, IllegalAccessException {
                    method.invoke(adapter, writer, value);
                }
            };

        } else if (parameterTypes.length == 1 && returnType != void.class) {
            // public List<Integer> pointToXml(Point point) throws Exception {
            final Set<? extends Annotation> returnTypeAnnotations = me.tatarka.parsnip.Util.xmlAnnotations(method);
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Set<? extends Annotation> qualifierAnnotations =
                    me.tatarka.parsnip.Util.xmlAnnotations(parameterAnnotations[0]);
            boolean nullable = me.tatarka.parsnip.Util.hasNullable(parameterAnnotations[0]);
            return new AdapterMethod(parameterTypes[0], qualifierAnnotations, adapter, method, nullable) {
                @Override
                public void toXml(XmlAdapters adapters, me.tatarka.parsnip.XmlWriter writer, Object value)
                        throws IOException, InvocationTargetException, IllegalAccessException {
                    me.tatarka.parsnip.XmlAdapter<Object> delegate = adapters.adapter(returnType, returnTypeAnnotations);
                    if (delegate == null) {
                        throw new IllegalArgumentException("No XmlAdapter for type " + returnType + " and annotations " + returnTypeAnnotations);
                    }
                    Object intermediate = method.invoke(adapter, value);
                    delegate.toXml(writer, intermediate);
                }
            };

        } else {
            throw new IllegalArgumentException("Unexpected signature for " + method + ".\n"
                    + "@ToXml method signatures may have one of the following structures:\n"
                    + "    <any access modifier> void toXml(XmlWriter writer, T value) throws <any>;\n"
                    + "    <any access modifier> R toXml(T value) throws <any>;\n");
        }
    }

    /**
     * Returns an object that calls a {@code method} method on {@code adapter} in service of
     * converting an object from JSON.
     */
    static AdapterMethod fromAdapter(Object adapter, Method method) {
        method.setAccessible(true);
        final Type[] parameterTypes = method.getGenericParameterTypes();
        final Type returnType = method.getGenericReturnType();

        if (parameterTypes.length == 1
                && parameterTypes[0] == me.tatarka.parsnip.XmlReader.class
                && returnType != void.class) {
            // public Point pointFromXml(XmlReader xmlReader) throws Exception {
            Set<? extends Annotation> returnTypeAnnotations = me.tatarka.parsnip.Util.xmlAnnotations(method);
            return new AdapterMethod(returnType, returnTypeAnnotations, adapter, method, false) {
                @Override
                public Object fromXml(XmlAdapters adapters, me.tatarka.parsnip.XmlReader reader)
                        throws IOException, IllegalAccessException, InvocationTargetException {
                    return method.invoke(adapter, reader);
                }
            };

        } else if (parameterTypes.length == 1 && returnType != void.class) {
            // public Point pointFromXml(List<Integer> o) throws Exception {
            Set<? extends Annotation> returnTypeAnnotations = me.tatarka.parsnip.Util.xmlAnnotations(method);
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            final Set<? extends Annotation> qualifierAnnotations
                    = me.tatarka.parsnip.Util.xmlAnnotations(parameterAnnotations[0]);
            boolean nullable = me.tatarka.parsnip.Util.hasNullable(parameterAnnotations[0]);
            return new AdapterMethod(returnType, returnTypeAnnotations, adapter, method, nullable) {
                @Override
                public Object fromXml(XmlAdapters adapters, me.tatarka.parsnip.XmlReader reader)
                        throws IOException, IllegalAccessException, InvocationTargetException {
                    me.tatarka.parsnip.XmlAdapter<Object> delegate = adapters.adapter(parameterTypes[0], qualifierAnnotations);
                    if (delegate == null) {
                        throw new IllegalArgumentException("No XmlAdapter for type " + parameterTypes[0] + " and annotations " + qualifierAnnotations);
                    }
                    Object intermediate = delegate.fromXml(reader);
                    return method.invoke(adapter, intermediate);
                }
            };

        } else {
            throw new IllegalArgumentException("Unexpected signature for " + method + ".\n"
                    + "@ToXml method signatures may have one of the following structures:\n"
                    + "    <any access modifier> void toXml(XmlWriter writer, T value) throws <any>;\n"
                    + "    <any access modifier> R toXml(T value) throws <any>;\n");
        }
    }

    /**
     * Returns the matching adapter method from the list.
     */
    private static AdapterMethod get(
            List<AdapterMethod> adapterMethods, Type type, Set<? extends Annotation> annotations) {
        for (int i = 0, size = adapterMethods.size(); i < size; i++) {
            AdapterMethod adapterMethod = adapterMethods.get(i);
            if (adapterMethod.type.equals(type) && adapterMethod.annotations.equals(annotations)) {
                return adapterMethod;
            }
        }
        return null;
    }

    static abstract class AdapterMethod {
        final Type type;
        final Set<? extends Annotation> annotations;
        final Object adapter;
        final Method method;
        final boolean nullable;

        public AdapterMethod(Type type, Set<? extends Annotation> annotations, Object adapter, Method method, boolean nullable) {
            this.type = type;
            this.annotations = annotations;
            this.adapter = adapter;
            this.method = method;
            this.nullable = nullable;
        }

        public void toXml(XmlAdapters adapters, me.tatarka.parsnip.XmlWriter writer, Object value)
                throws IOException, IllegalAccessException, InvocationTargetException {
            throw new AssertionError();
        }

        public Object fromXml(XmlAdapters adapters, me.tatarka.parsnip.XmlReader reader)
                throws IOException, IllegalAccessException, InvocationTargetException {
            throw new AssertionError();
        }
    }
}
