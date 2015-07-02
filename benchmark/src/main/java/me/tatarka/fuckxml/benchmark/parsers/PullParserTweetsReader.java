package me.tatarka.fuckxml.benchmark.parsers;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import me.tatarka.fuckxml.benchmark.TweetsReader;
import me.tatarka.fuckxml.benchmark.model.Author;
import me.tatarka.fuckxml.benchmark.model.Content;
import me.tatarka.fuckxml.benchmark.model.Tweet;
import me.tatarka.fuckxml.benchmark.model.Tweets;

public class PullParserTweetsReader implements TweetsReader {

    private DateFormat dateFormat;
    private XmlPullParserFactory f;

    public PullParserTweetsReader() throws Exception {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f = XmlPullParserFactory.newInstance();
        f.setNamespaceAware(true);
    }

    @Override
    public String getParserName() {
        return "Pull-Parser";
    }

    @Override
    public Tweets read(InputStream stream) throws Exception {
        XmlPullParser parser = f.newPullParser();
        parser.setInput(stream, "utf-8");
        parser.nextTag();
        return parse(parser);
    }

    private Tweets parse(XmlPullParser parser) throws Exception {
        Tweets tweets = new Tweets();
        tweets.tweets = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "entry":
                    tweets.tweets.add(readTweet(parser));
                    break;
                default:
                    skip(parser);
            }
        }

        return tweets;
    }

    private Tweet readTweet(XmlPullParser parser) throws Exception {
        Tweet tweet = new Tweet();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "published":
                    tweet.published = dateFormat.parse(readText(parser));
                    break;
                case "title":
                    tweet.title = readText(parser);
                    break;
                case "content":
                    tweet.content = readContent(parser);
                    break;
                case "lang":
                    tweet.language = readText(parser);
                    break;
                case "author":
                    tweet.author = readAuthor(parser);
                    break;
                default:
                    skip(parser);
            }
        }
        return tweet;
    }

    private Content readContent(XmlPullParser parser) throws Exception {
        Content content = new Content();
        content.type = parser.getAttributeValue(null, "type");
        content.value = readText(parser);
        return content;
    }

    private Author readAuthor(XmlPullParser parser) throws Exception {
        Author author = new Author();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "name":
                    author.name = readText(parser);
                    break;
                case "uri":
                    author.uri = readText(parser);
                    break;
                default:
                    skip(parser);
            }
        }
        return author;
    }


    private String readText(XmlPullParser parser) throws Exception {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }


    private void skip(XmlPullParser parser) throws Exception {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
