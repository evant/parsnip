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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import me.tatarka.parsnip.TypeConverter.Factory;
import me.tatarka.parsnip.annotations.SerializedName;

final class StandardTypeConverters {

    static final Factory FACTORY = new Factory() {
        @Override
        public me.tatarka.parsnip.TypeConverter<?> create(Type type, Set<? extends Annotation> annotations) {
            if (type == boolean.class) return BOOLEAN_TYPE_CONVERTER;
            if (type == byte.class) return BYTE_TYPE_CONVERTER;
            if (type == char.class) return CHARACTER_TYPE_CONVERTER;
            if (type == double.class) return DOUBLE_TYPE_CONVERTER;
            if (type == float.class) return FLOAT_TYPE_CONVERTER;
            if (type == int.class) return INTEGER_TYPE_CONVERTER;
            if (type == long.class) return LONG_TYPE_CONVERTER;
            if (type == short.class) return SHORT_TYPE_CONVERTER;
            if (type == Boolean.class) return BOOLEAN_TYPE_CONVERTER;
            if (type == Byte.class) return BYTE_TYPE_CONVERTER;
            if (type == Character.class) return CHARACTER_TYPE_CONVERTER;
            if (type == Double.class) return DOUBLE_TYPE_CONVERTER;
            if (type == Float.class) return FLOAT_TYPE_CONVERTER;
            if (type == Integer.class) return INTEGER_TYPE_CONVERTER;
            if (type == Long.class) return LONG_TYPE_CONVERTER;
            if (type == Short.class) return SHORT_TYPE_CONVERTER;
            if (type == String.class) return STRING_TYPE_CONVERTER;
            Class<?> rawType = Types.getRawType(type);
            if (rawType.isEnum()) {
                //noinspection unchecked
                return new EnumTypeConverter<>((Class<? extends Enum>) rawType);
            }
            return null;
        }
    };

    private static final String ERROR_FORMAT = "Expected %s but was %s";

    private static int rangeCheckInt(String strValue, String typeMessage, int min, int max)
            throws XmlDataException {
        int value = Integer.parseInt(strValue);
        if (value < min || value > max) {
            throw new XmlDataException(
                    String.format(ERROR_FORMAT, typeMessage, value));
        }
        return value;
    }

    static final TypeConverter<Boolean> BOOLEAN_TYPE_CONVERTER = new TypeConverter<Boolean>() {
        @Override
        public Boolean from(String value) {
            return Boolean.parseBoolean(value);
        }

        @Override
        public String to(Boolean value) {
            return value.toString();
        }
    };

    static final TypeConverter<Byte> BYTE_TYPE_CONVERTER = new TypeConverter<Byte>() {
        @Override
        public Byte from(String value) {
            return (byte) rangeCheckInt(value, "a byte", Byte.MIN_VALUE, 0xFF);
        }

        @Override
        public String to(Byte value) {
            return value.toString();
        }
    };

    static final TypeConverter<Character> CHARACTER_TYPE_CONVERTER = new TypeConverter<Character>() {
        @Override
        public Character from(String value) {
            if (value.length() > 1) {
                throw new XmlDataException(String.format(ERROR_FORMAT, "a char", '"' + value + '"'));
            }
            return value.charAt(0);
        }

        @Override
        public String to(Character value) {
            return value.toString();
        }
    };

    static final TypeConverter<Double> DOUBLE_TYPE_CONVERTER = new TypeConverter<Double>() {
        @Override
        public Double from(String value) {
            return Double.parseDouble(value);
        }

        @Override
        public String to(Double value) {
            return value.toString();
        }
    };

    static final TypeConverter<Float> FLOAT_TYPE_CONVERTER = new TypeConverter<Float>() {
        @Override
        public Float from(String value) {
            return Float.parseFloat(value);
        }

        @Override
        public String to(Float value) {
            return value.toString();
        }
    };

    static final TypeConverter<Integer> INTEGER_TYPE_CONVERTER = new TypeConverter<Integer>() {
        @Override
        public Integer from(String value) {
            return Integer.parseInt(value);
        }

        @Override
        public String to(Integer value) {
            return value.toString();
        }
    };

    static final TypeConverter<Long> LONG_TYPE_CONVERTER = new TypeConverter<Long>() {
        @Override
        public Long from(String value) {
            return Long.parseLong(value);
        }

        @Override
        public String to(Long value) {
            return value.toString();
        }
    };

    static final TypeConverter<Short> SHORT_TYPE_CONVERTER = new TypeConverter<Short>() {
        @Override
        public Short from(String value) {
            return (short) rangeCheckInt(value, "a short", Short.MIN_VALUE, Short.MAX_VALUE);
        }

        @Override
        public String to(Short value) {
            return value.toString();
        }
    };

    static final TypeConverter<String> STRING_TYPE_CONVERTER = new TypeConverter<String>() {
        @Override
        public String from(String value) {
            return value;
        }

        @Override
        public String to(String value) {
            return value;
        }
    };

    static final class EnumTypeConverter<T extends Enum<T>> implements TypeConverter<T> {
        private final Map<String, T> nameConstantMap;
        private final String[] nameStrings;

        EnumTypeConverter(Class<T> enumType) {
            try {
                T[] constants = enumType.getEnumConstants();
                nameConstantMap = new LinkedHashMap<>();
                nameStrings = new String[constants.length];
                for (int i = 0; i < constants.length; i++) {
                    T constant = constants[i];
                    SerializedName annotation = enumType.getField(constant.name()).getAnnotation(SerializedName.class);
                    String name = annotation != null ? annotation.value() : constant.name();
                    nameConstantMap.put(name, constant);
                    nameStrings[i] = name;
                }
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Missing field in " + enumType.getName());
            }
        }

        @Override
        public T from(String value) {
            T constant = nameConstantMap.get(value);
            if (constant != null) return constant;
            throw new XmlDataException("Expected one of " + nameConstantMap.keySet() + " but was " + value);
        }

        @Override
        public String to(T value) {
            return nameStrings[value.ordinal()];
        }
    }
}
