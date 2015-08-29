package me.tatarka.parsnip

import me.tatarka.parsnip.Namespace
import me.tatarka.parsnip.XmlWriter
import okio.Buffer
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals

class XmlWriterSpecs : Spek() {
    init {
        given("an XmlWriter") {
            val xmlWriter: ((XmlWriter) -> Unit) -> String = { xml ->
                val buffer = Buffer()
                val writer = XmlWriter(buffer)
                xml(writer)
                writer.flush()
                buffer.readUtf8()
            }

            on("an empty tag") {
                val result = xmlWriter {
                    it.beginTag("test").endTag()
                }

                it("should write a self-closing tag") {
                    assertEquals("<test/>", result)
                }
            }

            on("a nested tag") {
                val result = xmlWriter {
                    it.beginTag("test1").beginTag("test2").endTag().endTag()
                }

                it("should write nested tags") {
                    assertEquals("<test1><test2/></test1>", result)
                }
            }

            on("a tag with an attribute") {
                val result = xmlWriter {
                    it.beginTag("test").name("attribute").value("value").endTag()
                }

                it("should write the attribute and value") {
                    assertEquals("<test attribute=\"value\"/>", result)
                }
            }

            on("a tag with two attributes") {
                val result = xmlWriter {
                    it.beginTag("test")
                            .name("attribute1").value("value1")
                            .name("attribute2").value("value2")
                            .endTag();
                }

                it("should write the two attributes") {
                    assertEquals("<test attribute1=\"value1\" attribute2=\"value2\"/>", result)
                }
            }

            on("a tag with an attribute and a nested tag") {
                val result = xmlWriter {
                    it.beginTag("test1")
                            .name("attribute").value("value")
                            .beginTag("test2").endTag()
                            .endTag()
                }

                it("should write the attribute and nested tag") {
                    assertEquals("<test1 attribute=\"value\"><test2/></test1>", result)
                }
            }

            on("a tag text") {
                val result = xmlWriter {
                    it.beginTag("test").text("text").endTag()
                }

                it("should write the tag with text") {
                    assertEquals("<test>text</test>", result)
                }
            }

            on("a tag with an attribute and text") {
                val result = xmlWriter {
                    it.beginTag("test")
                            .name("attribute").value("value")
                            .text("text")
                            .endTag()
                }

                it("should write the tag with attribute and text") {
                    assertEquals("<test attribute=\"value\">text</test>", result)
                }
            }

            on("a tag with a namespace declaration") {
                val namespace = Namespace("ns", "foo")
                val result = xmlWriter {
                    it.beginTag("test").namespace(namespace).endTag()
                }

                it("should write the tag with the declared namespace") {
                    assertEquals("<test xmlns:ns=\"foo\"/>", result)
                }

                it("should write the namespace and alias to the provided namespace") {
                    assertEquals("ns", namespace.alias)
                    assertEquals("foo", namespace.namespace)
                }
            }

            on("a tag with a namespace") {
                val namespace = Namespace("ns", "foo")
                val result = xmlWriter {
                    it.namespace(namespace).beginTag(namespace, "test").endTag()
                }

                it("should write the namespace declaration and prefix the tag") {
                    assertEquals("<ns:test xmlns:ns=\"foo\"/>", result)
                }

                it("should write the namespace and alias to the provided namespace") {
                    assertEquals("ns", namespace.alias)
                    assertEquals("foo", namespace.namespace)
                }
            }

            on("a tag with a namespaced attribute") {
                val namespace = Namespace("ns", "foo")
                val result = xmlWriter {
                    it.beginTag("test")
                            .namespace(namespace)
                            .name(namespace, "attribute").value("value")
                            .endTag()
                }

                it("should writhe the namespace declaration and prefix the attribute") {
                    assertEquals("<test xmlns:ns=\"foo\" ns:attribute=\"value\"/>", result)
                }

                it("should write the namespace and alias to the provided namespace") {
                    assertEquals("ns", namespace.alias)
                    assertEquals("foo", namespace.namespace)
                }
            }

            on("a an attribute value with characters that need to be replaced") {
                val result = xmlWriter {
                    it.beginTag("test").name("attribute").value("\"\'<>&").endTag()
                }

                it("should write the value with replaced entities") {
                    assertEquals("<test attribute=\"&quot;&apos;&lt;&gt;&amp;\"/>", result)
                }
            }

            on("text with characters that need to be replaced") {
                val result = xmlWriter {
                    it.beginTag("test").text("\"\'<>&").endTag()
                }
               
                it("should write the text with replaced entities") {
                    assertEquals("<test>&quot;&apos;&lt;&gt;&amp;</test>", result)
                }
            }
        }
    }
}

