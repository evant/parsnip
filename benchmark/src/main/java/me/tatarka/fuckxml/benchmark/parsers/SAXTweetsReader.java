package me.tatarka.fuckxml.benchmark.parsers;

import me.tatarka.fuckxml.benchmark.TweetsReader;
import me.tatarka.fuckxml.benchmark.model.Author;
import me.tatarka.fuckxml.benchmark.model.Content;
import me.tatarka.fuckxml.benchmark.model.Tweet;
import me.tatarka.fuckxml.benchmark.model.Tweets;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class SAXTweetsReader implements TweetsReader {

    private XMLReader reader;
    private TweetsHandler handler;

    public SAXTweetsReader() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        reader = parser.getXMLReader();
        handler = new TweetsHandler();
        reader.setContentHandler(handler);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
    }

    @Override
    public String getParserName() {
        return "SAX";
    }

    @Override
    public Tweets read(InputStream stream) throws Exception {
        reader.parse(new InputSource(stream));
        return handler.getResult();
    }

    private static class TweetsHandler extends DefaultLexicalHandler {
        private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        private Tweets tweets;
        private Tweet tweet;
        private Content content;
        private Author author;
        private String currentElement;
        private StringBuilder chars;

        public Tweets getResult() {
            return tweets;
        }

        @Override
        public void startDocument() throws SAXException {
            chars = new StringBuilder();
            tweets = new Tweets();
            tweets.tweets = new ArrayList<>();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            currentElement = qName;
            chars.setLength(0);

            if ("entry".equals(qName)) {
                tweets.tweets.add(tweet = new Tweet());
            } else if ("content".equals(qName)) {
                tweet.content = (content = new Content());
                content.type = attributes.getValue("type");
            } else if ("author".equals(qName)) {
                tweet.author = (author = new Author());
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (chars.length() > 0) {
                setCharacterValue(chars);
            }
            currentElement = null;
        }

        @Override
        public void startEntity(String name) throws SAXException {
        }

        @Override
        public void endEntity(String name) throws SAXException {
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            chars.append(ch, start, length);
        }

        private void setCharacterValue(StringBuilder characters) throws SAXException {
            if ("published".equals(currentElement)) {
                try {
                    tweet.published = dateFormat.parse(characters.toString());
                } catch (ParseException anExc) {
                    throw new SAXException(anExc);
                }
            } else if (("title".equals(currentElement)) && (tweet != null)) {
                tweet.title = characters.toString();
            } else if ("content".equals(currentElement)) {
                content.value = characters.toString();
            } else if ("twitter:lang".equals(currentElement)) {
                tweet.language = characters.toString();
            } else if ("name".equals(currentElement)) {
                author.name = characters.toString();
            } else if ("uri".equals(currentElement)) {
                author.uri = characters.toString();
            }
        }
    }

    static class DefaultLexicalHandler extends DefaultHandler implements LexicalHandler {
        @Override
        public void comment(char[] aArg0, int aArg1, int aArg2) throws SAXException {
        }

        @Override
        public void endCDATA() throws SAXException {
        }

        @Override
        public void endDTD() throws SAXException {
        }

        @Override
        public void endEntity(String aName) throws SAXException {
        }

        @Override
        public void startCDATA() throws SAXException {
        }

        @Override
        public void startDTD(String aArg0, String aArg1, String aArg2) throws SAXException {
        }

        @Override
        public void startEntity(String aName) throws SAXException {
        }
    }
}
