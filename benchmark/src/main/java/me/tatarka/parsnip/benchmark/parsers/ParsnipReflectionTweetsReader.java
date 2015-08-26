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
import me.tatarka.parsnip.benchmark.TweetsReader;
import me.tatarka.parsnip.benchmark.model.feed;
import okio.Okio;

public class ParsnipReflectionTweetsReader implements TweetsReader {
    private Xml xml = new Xml.Builder().add(Date.class, new DateTypeConverter()).build();

    @Override
    public feed read(InputStream stream) throws Exception {
        XmlAdapter<feed> adapter = xml.adapter(feed.class);
        return adapter.fromXml(Okio.buffer(Okio.source(stream)));
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
