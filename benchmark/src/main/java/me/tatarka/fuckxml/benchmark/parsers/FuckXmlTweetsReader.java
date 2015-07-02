package me.tatarka.fuckxml.benchmark.parsers;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import me.tatarka.fuckxml.XmlReader;
import me.tatarka.fuckxml.benchmark.TweetsReader;
import me.tatarka.fuckxml.benchmark.model.Author;
import me.tatarka.fuckxml.benchmark.model.Content;
import me.tatarka.fuckxml.benchmark.model.Tweet;
import me.tatarka.fuckxml.benchmark.model.Tweets;
import okio.Okio;

/**
 * Created by evan on 6/20/15.
 */
public class FuckXmlTweetsReader implements TweetsReader {
    private DateFormat dateFormat;

    public FuckXmlTweetsReader() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    }

    @Override
    public String getParserName() {
        return "Fuck Xml";
    }

    @Override
    public Tweets read(InputStream stream) throws Exception {
        XmlReader reader = new XmlReader(Okio.buffer(Okio.source(stream)));
        reader.beginTag();
        return parse(reader);
    }

    private Tweets parse(XmlReader reader) throws Exception {
        Tweets tweets = new Tweets();
        tweets.tweets = new ArrayList<>();

        while (reader.peek() != XmlReader.Token.END_DOCUMENT) {
            if (reader.peek() != XmlReader.Token.BEGIN_TAG) {
                reader.skip();
                continue;
            }

            String name = reader.beginTag();
            switch (name) {
                case "entry":
                    tweets.tweets.add(readTweet(reader));
                    break;
                default:
                    reader.skipTag();
            }
        }
        return tweets;
    }

    private Tweet readTweet(XmlReader reader) throws Exception {
        Tweet tweet = new Tweet();
        while (reader.peek() != XmlReader.Token.END_TAG) {
            if (reader.peek() != XmlReader.Token.BEGIN_TAG) {
                reader.skip();
                continue;
            }

            String name = reader.beginTag();
            switch (name) {
                case "published":
                    tweet.published = dateFormat.parse(readText(reader));
                    break;
                case "title":
                    tweet.title = readText(reader);
                    break;
                case "content":
                    tweet.content = readContent(reader);
                    break;
                case "lang":
                    tweet.language = readText(reader);
                    break;
                case "author":
                    tweet.author = readAuthor(reader);
                    break;
                default:
                    reader.skipTag();
            }
        }
        return tweet;
    }

    private Content readContent(XmlReader reader) throws Exception {
        Content content = new Content();
        while (reader.peek() == XmlReader.Token.ATTRIBUTE) {
            String name = reader.nextAttribute();
            if ("type".equals(name)) {
                content.type = reader.nextValue();
            } else {
                reader.nextValue();
            }
        }
        content.value = readText(reader);
        return content;
    }

    private Author readAuthor(XmlReader reader) throws Exception {
        Author author = new Author();
        while (reader.peek() != XmlReader.Token.END_TAG) {
            if (reader.peek() != XmlReader.Token.BEGIN_TAG) {
                reader.skip();
                continue;
            }
            
            String name = reader.beginTag();
            switch (name) {
                case "name":
                    author.name = readText(reader);
                    break;
                case "uri":
                    author.uri = readText(reader);
                    break;
                default:
                    reader.skipTag();
            }
        }
        return author;
    }
    
    private String readText(XmlReader reader) throws Exception {
        String result = "";
        if (reader.peek() == XmlReader.Token.TEXT) {
            result = reader.nextText();
            reader.endTag();
        }
        return result;
    }
}
