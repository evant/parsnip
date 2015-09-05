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

package me.tatarka.parsnip;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

import okio.BufferedSink;
import okio.ByteString;
import okio.Sink;

public class XmlWriter implements Closeable, Flushable {

    /**
     * Replacements for xml entities that need to be encoded.
     */
    private static final String[] REPLACEMENT_CHARS;

    static {
        REPLACEMENT_CHARS = new String[128];
        REPLACEMENT_CHARS['"'] = "&quot;";
        REPLACEMENT_CHARS['\''] = "&apos;";
        REPLACEMENT_CHARS['<'] = "&lt;";
        REPLACEMENT_CHARS['>'] = "&gt;";
        REPLACEMENT_CHARS['&'] = "&amp;";
    }

    private static final ByteString SELF_END_TAG = ByteString.encodeUtf8("/>");
    private static final ByteString END_TAG = ByteString.encodeUtf8("</");

    private static final int STATE_BEFORE_DOCUMENT = 0;
    private static final int STATE_TAG = 1;
    private static final int STATE_ATTRIBUTE = 2;
    private static final int STATE_TEXT = 3;
    private static final int STATE_AFTER_TAG = 4;

    private final BufferedSink sink;

    private int state = STATE_BEFORE_DOCUMENT;
    private int stackSize = 1;
    private String[] pathNames = new String[32];
    /**
     * A namespace that will be written on the next declared tag. This is to allow a namespace to
     * appear on tag in which it is declared.
     */
    private Namespace pendingNamespace;

    /**
     * A string containing a full set of spaces for a single level of indentation, or null for no
     * pretty printing.
     */
    private String indent;
    private char quote = '"';
    private String deferredName;

    public XmlWriter(BufferedSink sink) {
        if (sink == null) {
            throw new NullPointerException("sink == null");
        }
        this.sink = sink;
    }

    /**
     * Sets the indentation string to be repeated for each level of indentation in the encoded
     * document. If {@code indent.isEmpty()} the encoded document will be compact. Otherwise the
     * encoded document will be more human-readable.
     *
     * @param indent a string containing only whitespace.
     */
    public final void setIndent(String indent) {
        if (indent.length() == 0) {
            this.indent = null;
        } else {
            this.indent = indent;
        }
    }

    /**
     * Sets the quote around attribute values.
     *
     * @param quote the quote char, only single (') and double (") quotes are allowed.
     */
    public final void setQuote(char quote) {
        if (quote == '"' || quote == '\'') {
            this.quote = quote;
        } else {
            throw new IllegalArgumentException("Only single or double quotes allowed");
        }
    }

    /**
     * Begins encoding a new xml tag. Each call to this method must be paired with a call to {@link
     * #endTag()}.
     *
     * @param name the name of the tag, must not be null.
     * @return this writer.
     */
    public XmlWriter beginTag(String name) throws IOException {
        return beginTag(null, name);
    }

    /**
     * Begins encoding a new xml tag. Each call to this method must be paired with a call to {@link
     * #endTag()}.
     *
     * @param namespace the namespace for the tag, created by {@link #namespace(Namespace)}, may be
     *                  null.
     * @param name      the name of the tag, must not be null.
     * @return this writer.
     */
    public XmlWriter beginTag(Namespace namespace, String name) throws IOException {
        String fullName = namespace != null ? namespace.alias + ":" + name : name;
        afterBeginTag();
        state = STATE_TAG;
        push(fullName);
        sink.writeByte('<');
        sink.writeUtf8(fullName);

        if (pendingNamespace != null) {
            declareNamespace(pendingNamespace);
            pendingNamespace = null;
        }

        return this;
    }

    /**
     * Ends the current tag.
     *
     * @return this writer.
     */
    public XmlWriter endTag() throws IOException {
        stackSize--;
        // If were are on the same tag and no text has been written, self-close. Otherwise write a closing tag.
        String pathName = pathNames[stackSize];
        pathNames[stackSize] = null;
        if (state == STATE_TAG) {
            sink.write(SELF_END_TAG);
        } else {
            sink.write(END_TAG);
            sink.writeUtf8(pathName);
            sink.writeByte('>');
        }
        state = STATE_AFTER_TAG;
        return this;
    }

    /**
     * Encodes the attribute name. Each call to this method must be paired with {@link
     * #value(String)}.
     *
     * @param name the name of the attribute, must not be null.
     * @return this writer.
     */
    public XmlWriter name(String name) throws IOException {
        return name(null, name);
    }

    /**
     * Encodes the attribute name. Each call to this method must be paired with {@link
     * #value(String)}.
     *
     * @param namespace the namespace for the tag, created by {@link #namespace(Namespace)}, may be
     *                  null.
     * @param name      the name of the attribute, must not be null.
     * @return this writer.
     */
    public XmlWriter name(Namespace namespace, String name) throws IOException {
        if (state != STATE_TAG) {
            throw new IllegalStateException();
        }
        state = STATE_ATTRIBUTE;
        deferredName = namespace != null ? namespace.alias + ":" + name : name;
        return this;
    }

    private void writeDeferredName() throws IOException {
        if (deferredName != null) {
            sink.writeByte(' ');
            sink.writeUtf8(deferredName);
            sink.writeByte('=');
            deferredName = null;
        }
    }

    /**
     * Encodes the attribute value.
     *
     * @param value the value of the attribute, if this is null the attribute will be skipped.
     * @return this writer.
     */
    public XmlWriter value(String value) throws IOException {
        if (state != STATE_ATTRIBUTE) {
            throw new IllegalStateException();
        }
        state = STATE_TAG;
        if (value == null) {
            // skip this name and value
            deferredName = null;
            return this;
        }
        writeDeferredName();
        sink.writeByte(quote);
        string(value);
        sink.writeByte(quote);
        return this;
    }

    /**
     * Encodes the tag text.
     *
     * @param text the tag text.
     * @return this writer.
     */
    public XmlWriter text(String text) throws IOException {
        if (state == STATE_BEFORE_DOCUMENT || state == STATE_ATTRIBUTE) {
            throw new IllegalStateException();
        }
        afterBeginTag();
        state = STATE_TEXT;
        string(text);
        return this;
    }

    /**
     * Declare a namespace with the given name. It will either be declared on the current tag or the
     * next one if there isn't a current one.
     *
     * @param namespace The namespace object, pass to {@link #beginTag(Namespace, String)} or {@link
     *                  #name(Namespace, String)} to reference the namespace. You should take care
     *                  to not pass a namespace that is not defined in the current scope. Feel free
     *                  to use the same one multiple times to reduce allocations.
     * @return this writer.
     */
    public XmlWriter namespace(Namespace namespace) throws IOException {
        if (state == STATE_TAG) {
            declareNamespace(namespace);
        } else if (state == STATE_BEFORE_DOCUMENT || state == STATE_AFTER_TAG) {
            pendingNamespace = namespace;
        } else {
            throw new IllegalStateException();
        }
        return this;
    }

    private void afterBeginTag() throws IOException {
        if (state == STATE_TAG) {
            sink.writeByte('>');
        }
    }

    private void declareNamespace(Namespace namespace) throws IOException {
        name("xmlns:" + namespace.alias).value(namespace.namespace);
    }

    /**
     * Ensures all buffered data is written to the underlying {@link Sink} and flushes that writer.
     */
    @Override
    public void flush() throws IOException {
        if (stackSize == 0) {
            throw new IllegalStateException("XmlWriter is closed.");
        }
        sink.flush();
    }

    /**
     * Flushes and closes the writer and the underlying {@link Sink}.
     *
     * @throws me.tatarka.parsnip.XmlDataException if the XML document is incomplete.
     */
    @Override
    public void close() throws IOException {
        sink.close();

        int size = stackSize;
        if (size > 1) {
            throw new me.tatarka.parsnip.XmlDataException("Incomplete document");
        }
        stackSize = 0;
    }

    public String getPath() {
        StringBuilder path = new StringBuilder("/");
        for (int i = 1; i < stackSize; i++) {
            path.append(pathNames[i]);
            if (i != stackSize - 1) {
                path.append("/");
            }
        }
        return path.toString();
    }

    private void push(String name) {
        int stackSize = this.stackSize;
        if (stackSize == pathNames.length) {
            String[] newPathNames = new String[stackSize * 2];
            System.arraycopy(pathNames, 0, newPathNames, 0, stackSize);
            pathNames = newPathNames;

        }
        pathNames[stackSize] = name;
        this.stackSize++;
    }

    private void string(String value) throws IOException {
        String[] replacements = REPLACEMENT_CHARS;
        int last = 0;
        int length = value.length();
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            String replacement;
            if (c < 128) {
                replacement = replacements[c];
                if (replacement == null) {
                    continue;
                }
            } else {
                continue;
            }
            if (last < i) {
                sink.writeUtf8(value, last, i);
            }
            sink.writeUtf8(replacement);
            last = i + 1;
        }
        if (last < length) {
            sink.writeUtf8(value, last, length);
        }
    }
}
