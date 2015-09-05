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

package me.tatarka.parsnip.benchmark.parsers;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.transform.RegistryMatcher;
import org.simpleframework.xml.transform.Transform;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.tatarka.parsnip.benchmark.PerformanceTestRunner;
import me.tatarka.parsnip.benchmark.TweetsReader;
import me.tatarka.parsnip.benchmark.model.Tweets;

public class SimpleXmlReader implements TweetsReader {
    public static final PerformanceTestRunner.TweetsReaderFactory FACTORY = new PerformanceTestRunner.TweetsReaderFactory() {
        @Override
        public String getParserType() {
            return "Simple";
        }

        @Override
        public TweetsReader newReader() throws Exception {
            return new SimpleXmlReader();
        }
    };

    private Serializer serializer;

    public SimpleXmlReader() {
        RegistryMatcher m = new RegistryMatcher();
        m.bind(Date.class, new DateFormatTransformer());
        serializer = new Persister(m);
    }

    @Override
    public Tweets read(InputStream stream) throws Exception {
        return serializer.read(Tweets.class, stream);
    }

    static class DateFormatTransformer implements Transform<Date> {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        @Override
        public Date read(String value) throws Exception {
            return dateFormat.parse(value);
        }

        @Override
        public String write(Date value) throws Exception {
            return dateFormat.format(value);
        }
    }
}
