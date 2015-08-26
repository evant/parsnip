package me.tatarka.parsnip;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;

final class StandardTypeConverters {
    
    static final me.tatarka.parsnip.TypeConverter.Factory FACTORY = new me.tatarka.parsnip.TypeConverter.Factory() {
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
                return enumConverter((Class<? extends Enum>) rawType);
            }
            return null;
        }
    };
    
    private static final String ERROR_FORMAT = "Expected %s but was %s";

    private static int rangeCheckInt(String strValue, String typeMessage, int min, int max)
            throws me.tatarka.parsnip.XmlDataException {
        int value = Integer.parseInt(strValue);
        if (value < min || value > max) {
            throw new me.tatarka.parsnip.XmlDataException(
                    String.format(ERROR_FORMAT, typeMessage, value));
        }
        return value;
    }

    static final me.tatarka.parsnip.TypeConverter<Boolean> BOOLEAN_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Boolean>() {
        @Override
        public Boolean from(String value) {
            return Boolean.parseBoolean(value);
        }

        @Override
        public String to(Boolean value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Byte> BYTE_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Byte>() {
        @Override
        public Byte from(String value) {
            return (byte) rangeCheckInt(value, "a byte", Byte.MIN_VALUE, 0xFF);
        }

        @Override
        public String to(Byte value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Character> CHARACTER_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Character>() {
        @Override
        public Character from(String value) {
            if (value.length() > 1) {
                throw new me.tatarka.parsnip.XmlDataException(String.format(ERROR_FORMAT, "a char", '"' + value + '"'));
            }
            return value.charAt(0);
        }

        @Override
        public String to(Character value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Double> DOUBLE_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Double>() {
        @Override
        public Double from(String value) {
            return Double.parseDouble(value);
        }

        @Override
        public String to(Double value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Float> FLOAT_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Float>() {
        @Override
        public Float from(String value) {
            return Float.parseFloat(value);
        }

        @Override
        public String to(Float value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Integer> INTEGER_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Integer>() {
        @Override
        public Integer from(String value) {
            return Integer.parseInt(value);
        }

        @Override
        public String to(Integer value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Long> LONG_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Long>() {
        @Override
        public Long from(String value) {
            return Long.parseLong(value);
        }

        @Override
        public String to(Long value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<Short> SHORT_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<Short>() {
        @Override
        public Short from(String value) {
            return (short) rangeCheckInt(value, "a short", Short.MIN_VALUE, Short.MAX_VALUE);
        }

        @Override
        public String to(Short value) {
            return value.toString();
        }
    };

    static final me.tatarka.parsnip.TypeConverter<String> STRING_TYPE_CONVERTER = new me.tatarka.parsnip.TypeConverter<String>() {
        @Override
        public String from(String value) {
            return value;
        }

        @Override
        public String to(String value) {
            return value;
        }
    };

    static <T extends Enum<T>> me.tatarka.parsnip.TypeConverter<T> enumConverter(final Class<T> enumType) {
        return new me.tatarka.parsnip.TypeConverter<T>() {
            @Override
            public T from(String value) {
                try {
                    return Enum.valueOf(enumType, value);
                } catch (IllegalArgumentException e) {
                    throw new me.tatarka.parsnip.XmlDataException("Expected one of " + Arrays.toString(enumType.getEnumConstants()) + " but was " + value);
                }
            }

            @Override
            public String to(T value) {
                return value.name();
            }
        };
    }
}
