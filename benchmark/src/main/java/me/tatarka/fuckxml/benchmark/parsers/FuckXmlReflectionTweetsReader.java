package me.tatarka.fuckxml.benchmark.parsers;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.tatarka.fuckxml.TypeConverter;
import me.tatarka.fuckxml.Xml;
import me.tatarka.fuckxml.XmlAdapter;
import me.tatarka.fuckxml.XmlDataException;
import me.tatarka.fuckxml.benchmark.TweetsReader;
import me.tatarka.fuckxml.benchmark.model.feed;
import okio.Okio;

public class FuckXmlReflectionTweetsReader implements TweetsReader {
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
