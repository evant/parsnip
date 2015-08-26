package me.tatarka.parsnip;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

public interface TypeConverter<T> {

    T from(String value);

    String to(T value);

    interface Factory {
        TypeConverter<?> create(Type type, Set<? extends Annotation> annotations);
    }
}
