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

import me.tatarka.parsnip.benchmark.PerformanceTestRunner;
import me.tatarka.parsnip.benchmark.TweetsReader;
import me.tatarka.parsnip.benchmark.model.Author;
import me.tatarka.parsnip.benchmark.model.Content;
import me.tatarka.parsnip.benchmark.model.Tweet;
import me.tatarka.parsnip.benchmark.model.Tweets;
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
    
    public static final PerformanceTestRunner.TweetsReaderFactory FACTORY = new PerformanceTestRunner.TweetsReaderFactory() {
        @Override
        public String getParserType() {
            return "W3C DOM";
        }

        @Override
        public TweetsReader newReader() throws Exception {
            return new DOMTweetsReader();
        }
    };

    private DocumentBuilder builder;
    private DateFormat dateFormat;

    public DOMTweetsReader() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    }

    public Tweets read(InputStream stream) throws Exception {
        Document document = builder.parse(stream, "utf-8");
        Tweets result = new Tweets();
        result.tweets = new ArrayList<>();
        unmarshall(document, result);
        return result;
    }

    public void unmarshall(Document doc, Tweets tweets)
            throws Exception {
        NodeList nodes = doc.getChildNodes().item(0).getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if ((n.getNodeType() == Node.ELEMENT_NODE) && ("entry".equals(n.getNodeName()))) {
                Tweet tweet = new Tweet();
                tweets.tweets.add(tweet);
                unmarshallEntry((Element) n, tweet);
            }
        }
    }

    private void unmarshallEntry(Element tweetElem, Tweet tweet)
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
