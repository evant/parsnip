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
import okio.Okio;

public class ParsnipReflectionTweetsReader implements TweetsReader {
    public static final PerformanceTestRunner.TweetsReaderFactory FACTORY = new PerformanceTestRunner.TweetsReaderFactory() {
        @Override
        public String getParserType() {
            return "Parsnip Reflection";
        }

        @Override
        public TweetsReader newReader() throws Exception {
            return new ParsnipReflectionTweetsReader();
        }
    };
    
    private Xml xml = new Xml.Builder().add(Date.class, new DateTypeConverter()).build();

    @Override
    public Tweets read(InputStream stream) throws Exception {
        XmlAdapter<Tweets> adapter = xml.adapter(Tweets.class);
        Tweets tweets = adapter.fromXml(Okio.buffer(Okio.source(stream)));
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
