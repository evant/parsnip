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

import java.lang.reflect.Type;

import retrofit.Converter;

public class ParsnipConverterFactory implements Converter.Factory {

    public static ParsnipConverterFactory create() {
        return create(new Xml.Builder().build());
    }

    public static ParsnipConverterFactory create(Xml xml) {
        return new ParsnipConverterFactory(xml);
    }

    private final Xml xml;

    private ParsnipConverterFactory(Xml xml) {
        if (xml == null) throw new NullPointerException("xml == null");
        this.xml = xml;
    }

    @Override
    public Converter<?> get(Type type) {
        XmlAdapter<?> adapter = xml.adapter(type);
        return new ParsnipConverter<>(adapter);
    }
}
