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

package me.tatarka.parsnip.benchmark;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;

import me.tatarka.parsnip.Xml;
import me.tatarka.parsnip.XmlAdapter;
import okio.Buffer;
import okio.BufferedSource;
import retrofit.Converter;
import retrofit.Retrofit;

public class Api {

    public Api() {
        final Xml xml = new Xml.Builder().build();
        final Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(new Converter.Factory() {
                    @Override
                    public Converter<?> get(Type type) {
                        return new XmlConverter<>(xml, type);
                    }
                })
                .build();
    }
    
    private static class XmlConverter<T> implements Converter<T> {
        private static final MediaType mediaType = MediaType.parse("application/xml");
        private XmlAdapter<T> adapter;

        public XmlConverter(Xml xml, Type type) {
            adapter = xml.adapter(type);
        }

        @Override
        public T fromBody(ResponseBody body) throws IOException {
            BufferedSource source = body.source();
            try {
                return adapter.fromXml(source);
            } finally {
                source.close();
            }
        }

        @Override
        public RequestBody toBody(T value) {
            Buffer buffer = new Buffer();
            try {
                adapter.toXml(buffer, value);
            } catch (IOException e) {
                // Buffer shouldn't throw
                throw new AssertionError();
            }
            return RequestBody.create(mediaType, buffer.readByteString());
        }
    }
}
