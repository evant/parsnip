package me.tatarka.fuckxml;

/**
 * Created by evan on 8/1/15.
 */
public interface TypeConverter<T> {
   
    T from(String value);

    String to(T value);
}
