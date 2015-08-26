package me.tatarka.fuckxml.benchmark.parsers;

import me.tatarka.fuckxml.benchmark.TweetsReader;
import me.tatarka.fuckxml.benchmark.model.Author;
import me.tatarka.fuckxml.benchmark.model.Content;
import me.tatarka.fuckxml.benchmark.model.entry;
import me.tatarka.fuckxml.benchmark.model.feed;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class DOMTweetsReader implements TweetsReader {

    private DocumentBuilder builder;
    private DateFormat dateFormat;

    public DOMTweetsReader() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    }

    public feed read(InputStream stream) throws Exception {
        Document document = builder.parse(stream, "utf-8");
        feed result = new feed();
        result.tweets = new ArrayList<>();
        unmarshall(document, result);
        return result;
    }

    public void unmarshall(Document doc, feed tweets)
            throws Exception {
        NodeList nodes = doc.getChildNodes().item(0).getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if ((n.getNodeType() == Node.ELEMENT_NODE) && ("entry".equals(n.getNodeName()))) {
                entry tweet = new entry();
                tweets.tweets.add(tweet);
                unmarshallEntry((Element) n, tweet);
            }
        }
    }

    private void unmarshallEntry(Element tweetElem, entry tweet)
            throws Exception {
        NodeList _nodes = tweetElem.getChildNodes();
        for (int i = 0; i < _nodes.getLength(); i++) {
            Node node = _nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("published".equals(node.getNodeName())) {
                    tweet.published = dateFormat.parse(getPCData(node));
                } else if ("title".equals(node.getNodeName())) {
                    tweet.title = getPCData(node);
                } else if ("content".equals(node.getNodeName())) {
                    Content content = new Content();
                    tweet.content = content;
                    unmarshallContent((Element) node, content);
                } else if ("twitter:lang".equals(node.getNodeName())) {
                    tweet.lang = getPCData(node);
                } else if ("author".equals(node.getNodeName())) {
                    Author author = new Author();
                    tweet.author = author;
                    unmarshallAuthor((Element) node, author);
                }
            }
        }
    }

    private void unmarshallContent(Element contentElem, Content content) {
        content.type = contentElem.getAttribute("type");
        content.value = getPCData(contentElem);
    }

    private void unmarshallAuthor(Element authorElem, Author author) {
        NodeList nodes = authorElem.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if ("name".equals(node.getNodeName())) {
                author.name = getPCData(node);
            } else if ("uri".equals(node.getNodeName())) {
                author.uri = getPCData(node);
            }
        }
    }

    private String getPCData(Node aNode) {
        StringBuilder builder = new StringBuilder();
        if (Node.ELEMENT_NODE == aNode.getNodeType()) {
            NodeList nodes = aNode.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (Node.ELEMENT_NODE == node.getNodeType()) {
                    builder.append(getPCData(node));
                } else if (Node.TEXT_NODE == node.getNodeType()) {
                    builder.append(node.getNodeValue());
                }
            }
        }
        return builder.toString();
    }
}
