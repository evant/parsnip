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

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.tatarka.parsnip.TypeConverter;
import me.tatarka.parsnip.Xml;
import me.tatarka.parsnip.XmlAdapter;
import me.tatarka.parsnip.XmlDataException;
import me.tatarka.parsnip.benchmark.PerformanceTestRunner;
import me.tatarka.parsnip.benchmark.TweetsReader;
import me.tatarka.parsnip.benchmark.model.Tweets;

public class ParsnipTweetsReader implements TweetsReader {
    public static final PerformanceTestRunner.TweetsReaderFactory FACTORY = new PerformanceTestRunner.TweetsReaderFactory() {
        @Override
        public String getParserType() {
            return "Parsnip";
        }

        @Override
        public TweetsReader newReader() throws Exception {
            return new ParsnipTweetsReader();
        }
    };

    private Xml xml = new Xml.Builder().add(Date.class, new DateTypeConverter()).build();

    @Override
    public Tweets read(InputStream stream) throws Exception {
        XmlAdapter<Tweets> adapter = xml.adapter(Tweets.class);
        Tweets tweets = adapter.fromXml(stream);
        return tweets;
    }

    static class DateTypeConverter implements TypeConverter<Date> {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        @Override
        public Date from(String value) {
            try {
                return dateFormat.parse(value);
            } catch (ParseException e) {
                throw new XmlDataException(e);
            }
        }

        @Override
        public String to(Date value) {
            return dateFormat.format(value);
        }
    }
}
