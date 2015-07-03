package me.tatarka.fuckxml

import okio.Buffer
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fails

/**
 * Created by evan on 6/19/15.
 */
class XmlReaderSpecs : Spek() {
    init {
        given("an XmlReader") {
            val xmlReader = { xml: String -> XmlReader(Buffer().writeUtf8(xml)) }

            // Tag

            on("an empty self-closing tag") {
                val reader = xmlReader("<test/>")
                val tag = reader.beginTag()
                reader.endTag()

                it("should read the tag name") {
                    assertEquals("test", tag)
                }
            }

            on("an empty self-closing tag with spaces") {
                val reader = xmlReader("< test />")
                val tag = reader.beginTag()
                reader.endTag()

                it("should read the tag name") {
                    assertEquals("test", tag)
                }
            }

            on("an empty tag") {
                val reader = xmlReader("<test></test>")
                val tag = reader.beginTag()
                reader.endTag()

                it("should read the tag name") {
                    assertEquals("test", tag)
                }
            }

            on("a nested tag") {
                val reader = xmlReader("<test1><test2/></test1>")
                val tag1 = reader.beginTag()
                val tag2 = reader.beginTag()
                reader.endTag()
                reader.endTag()

                it("should read the outer tag name") {
                    assertEquals("test1", tag1)
                }

                it ("should read the inner tag name") {
                    assertEquals("test2", tag2)
                }
            }

            on("a nested tag with space") {
                val reader = xmlReader("<test1> <test2/> </test1>")
                val tag1 = reader.beginTag()
                val tag2 = reader.beginTag()
                reader.endTag()
                reader.endTag()

                it("should read the outer tag name") {
                    assertEquals("test1", tag1)
                }

                it ("should read the inner tag name") {
                    assertEquals("test2", tag2)
                }
            }

            on("two nested tags") {
                val reader = xmlReader("<test1><test2/><test3/></test1>")
                val tag1 = reader.beginTag()
                val tag2 = reader.beginTag()
                reader.endTag()
                val tag3 = reader.beginTag()
                reader.endTag()
                reader.endTag()

                it("should read the outer tag name") {
                    assertEquals("test1", tag1)
                }

                it ("should read the first inner tag name") {
                    assertEquals("test2", tag2)
                }

                it ("should read the second inner tag name") {
                    assertEquals("test3", tag3)
                }
            }

            on("declaration before document") {
                val reader = xmlReader("<?xml version=\"1.1\"?><test/>")
                val tag = reader.beginTag()

                it ("should skip the declaration and read the tag") {
                    assertEquals("test", tag)
                }
            }

            // Comment

            on("a comment between tags") {
                val reader = xmlReader("<test><!-- this should be ignored --></test>")
                val tag = reader.beginTag()
                reader.endTag()

                it("should read the tag name") {
                    assertEquals("test", tag)
                }
            }

            // Text

            on("a tag with text") {
                val reader = xmlReader("<test>value</test>")
                reader.beginTag()
                val text = reader.nextText()

                it ("should read the text") {
                    assertEquals("value", text)
                }
            }

            // Attribute

            on("a tag with a single-quoted attribute") {
                val reader = xmlReader("<test attribute='value'/>")
                reader.beginTag()
                val name = reader.nextAttribute()
                val value = reader.nextValue()

                it ("should read the attribute") {
                    assertEquals("attribute", name)
                }

                it ("should read the value") {
                    assertEquals("value", value)
                }
            }

            on("a tag with a double-quoted attribute") {
                val reader = xmlReader("<test attribute=\"value\"/>")
                reader.beginTag()
                val name = reader.nextAttribute()
                val value = reader.nextValue()

                it ("should read the attribute") {
                    assertEquals("attribute", name)
                }

                it ("should read the value") {
                    assertEquals("value", value)
                }
            }

            on("a tag with two attributes") {
                val reader = xmlReader("<test attribute1='value1' attribute2='value2'/>")
                reader.beginTag()
                val name1 = reader.nextAttribute()
                val value1 = reader.nextValue()
                val name2 = reader.nextAttribute()
                val value2 = reader.nextValue()

                it ("should read the first attribute") {
                    assertEquals("attribute1", name1)
                }

                it ("should read the first value") {
                    assertEquals("value1", value1)
                }

                it ("should read the second attribute") {
                    assertEquals("attribute2", name2)
                }

                it ("should read the second value") {
                    assertEquals("value2", value2)
                }
            }

            // Namespace
            
            on("a tag with a namespaced name") {
                val reader = xmlReader("<test1 xmlns:ns='foo'><ns:test2/></test1>")
                reader.beginTag()
                val tag = Namespaced()
                reader.beginTag(tag)
                
                it ("should read the tag name") {
                    assertEquals("test2", tag.name)
                }
                
                it ("should read the tag namespace") {
                    assertEquals("foo", tag.namespace)
                }
            }

            on("a tag with a namespaced attribute") {
                val reader = xmlReader("<test xmlns:ns='foo' ns:attribute='value'/>")
                reader.beginTag()
                val attribute = Namespaced()
                reader.nextAttribute(attribute)
                val value = reader.nextValue()

                it ("should read the attribute") {
                    assertEquals("attribute", attribute.name)
                }

                it ("should read the attribute namespace") {
                    assertEquals("foo", attribute.namespace)
                }

                it ("should read the value") {
                    assertEquals("value", value)
                }
            }

            on("a tag with a attribute and a nested duplicate namespace declaration") {
                val reader = xmlReader("<test1 xmlns:ns='foo'><test2 xmlns:ns='bar' ns:attribute='value'/></test1>")
                reader.beginTag()
                reader.beginTag()
                val attribute = Namespaced()
                reader.nextAttribute(attribute)
                val value = reader.nextValue()

                it ("should read the attribute") {
                    assertEquals("attribute", attribute.name)
                }

                it ("should read the attribute namespace") {
                    assertEquals("bar", attribute.namespace)
                }

                it ("should read the value") {
                    assertEquals("value", value)
                }
            }

            on("a tag with a namespace declaration and an attribute that incorrectly uses the namespace on an outer tag") {
                val reader = xmlReader("<test1><test2 xmlns:ns='foo'/><test3 ns:attribute='value'/></test1>")
                reader.beginTag()
                reader.beginTag()
                reader.endTag()
                reader.beginTag()
                val attribute = Namespaced()
                reader.nextAttribute(attribute)
                val value = reader.nextValue()

                it ("should read the attribute") {
                    assertEquals("attribute", attribute.name)
                }

                it ("should not read the attribute namespace of the other tag") {
                    assertNotEquals("foo", attribute.namespace)
                }

                it ("should read the value") {
                    assertEquals("value", value)
                }
            }
            
            on("a tag with a default namespace declaration") {
                val reader = xmlReader("<test1 xmlns='foo'><test2/></test1>")
                reader.beginTag()
                val tag = Namespaced()
                reader.beginTag(tag)

                it ("should read the tag name") {
                    assertEquals("test2", tag.name)
                }

                it ("should have the default namespace") {
                    assertEquals("foo", tag.namespace)
                }
            }

            on("an attribute on a tag with a default namespace declaration") {
                val reader = xmlReader("<test xmlns='foo' attribute='value'/>")
                reader.beginTag()
                val attribute = Namespaced()
                reader.nextAttribute(attribute)

                it ("should read the attribute") {
                    assertEquals("attribute", attribute.name)
                }

                it ("should have the default namespace") {
                    assertEquals("foo", attribute.namespace)
                }
            }

            // Entity

            on("built-in entity in text") {
                val reader = xmlReader("<test>&quot;&apos;&lt;&gt;&amp;</test>")
                reader.beginTag()
                val text = reader.nextText()

                it ("should expand the entities in text") {
                    assertEquals("\"'<>&", text)
                }
            }

            on("built-in entity in value") {
                val reader = xmlReader("<test attribute='&quot;&apos;&lt;&gt;&amp;'/>")
                reader.beginTag()
                reader.nextAttribute(Namespaced())
                val value = reader.nextValue()

                it ("should expand the entities in value") {
                    assertEquals("\"'<>&", value)
                }
            }

            on("a decimal character reference in text") {
                val reader = xmlReader("<test>&#9731;</test>")
                reader.beginTag()
                val text = reader.nextText()

                it ("should expand the decimal character reference in text") {
                    assertEquals("\u2603", text)
                }
            }

            on("a hex character reference in text") {
                val reader = xmlReader("<test>&#x2603;</test>")
                reader.beginTag()
                val text = reader.nextText()

                it ("should expand the hex character reference in text") {
                    assertEquals("\u2603", text)
                }
            }

            // Peek

            on("peek") {
                on("empty input") {
                    val reader = xmlReader("")
                    val token = reader.peek()

                    it ("should peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token)
                    }
                }

                on("an empty self-closing tag") {
                    val reader = xmlReader("<test/>")
                    val token1 = reader.peek()
                    reader.beginTag()
                    val token2 = reader.peek()
                    reader.endTag()
                    val token3 = reader.peek()

                    it ("should first peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token1)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token2)
                    }

                    it ("should finally peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token3)
                    }
                }

                on("an empty tag") {
                    val reader = xmlReader("<test></test>")
                    val token1 = reader.peek()
                    reader.beginTag()
                    val token2 = reader.peek()
                    reader.endTag()
                    val token3 = reader.peek()

                    it ("should first peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token1)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token2)
                    }

                    it ("should finally peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token3)
                    }
                }

                on("a nested tag") {
                    val reader = xmlReader("<test1><test2/></test1>")
                    val token1 = reader.peek()
                    reader.beginTag()
                    val token2 = reader.peek()
                    reader.beginTag()
                    val token3 = reader.peek()
                    reader.endTag()
                    val token4 = reader.peek()
                    reader.endTag()
                    val token5 = reader.peek()

                    it ("should first peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token1)
                    }

                    it ("should next peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token2)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token3)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token4)
                    }

                    it ("should finally peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token5)
                    }
                }

                on("a tag with text") {
                    val reader = xmlReader("<test>text</test>")
                    val token1 = reader.peek()
                    reader.beginTag()
                    val token2 = reader.peek()
                    reader.nextText()
                    val token3 = reader.peek()
                    reader.endTag()
                    val token4 = reader.peek()

                    it ("should first peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token1)
                    }

                    it ("should next peek a TEXT") {
                        assertEquals(XmlReader.Token.TEXT, token2)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token3)
                    }

                    it ("should finally peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token4)
                    }
                }

                on("a tag with an attribute") {
                    val reader = xmlReader("<test attribute='value'/>")
                    val token1 = reader.peek()
                    reader.beginTag()
                    val token2 = reader.peek()
                    reader.nextAttribute(Namespaced())
                    val token3 = reader.peek()
                    reader.nextValue()
                    val token4 = reader.peek()
                    reader.endTag()
                    val token5 = reader.peek()

                    it ("should first peek a BEGIN_TAG") {
                        assertEquals(XmlReader.Token.BEGIN_TAG, token1)
                    }

                    it ("should next peek an ATTRIBUTE") {
                        assertEquals(XmlReader.Token.ATTRIBUTE, token2)
                    }

                    it ("should next peek a VALUE") {
                        assertEquals(XmlReader.Token.VALUE, token3)
                    }

                    it ("should next peek an END_TAG") {
                        assertEquals(XmlReader.Token.END_TAG, token4)
                    }

                    it ("should finally peek an END_DOCUMENT") {
                        assertEquals(XmlReader.Token.END_DOCUMENT, token5)
                    }
                }
            }
            
            // Skip
            
            on("skip begin tag") {
                val reader = xmlReader("<test/>")
                reader.skip()
                val token = reader.peek() 
                
                it ("should peek an END_TAG") {
                    assertEquals(XmlReader.Token.END_TAG, token) 
                }
            }
            
            on("skip attribute") {
                val reader = xmlReader("<text attribute=\"value\"/>")
                reader.beginTag()
                reader.skip()
                val token = reader.peek()
                
                it ("should peek a VALUE") {
                    assertEquals(XmlReader.Token.VALUE, token) 
                }
            }
            
            on("skip value") {
                val reader = xmlReader("<test attribute=\"value\"/>")
                reader.beginTag()
                reader.nextAttribute()
                reader.skip()
                val token = reader.peek()

                it ("should peek an END_TAG") {
                    assertEquals(XmlReader.Token.END_TAG, token)
                }
            }
            
            on("skip text") {
                val reader = xmlReader("<test>text</test>")
                reader.beginTag()
                reader.skip()
                val token = reader.peek()

                it ("should peek an END_TAG") {
                    assertEquals(XmlReader.Token.END_TAG, token)
                }
            }
            
            on("skip end tag") {
                val reader = xmlReader("<test/>")
                reader.beginTag()
                reader.skip()
                val token = reader.peek()
                
                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }
            
            // Skip tag
            
            on("skip self closing tag") {
                val reader = xmlReader("<test/>")
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }
            
            on("skip an empty tag") {
                val reader = xmlReader("<test></test>")
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }
            
            on("skip a tag with text") {
                val reader = xmlReader("<test>text</test>")
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }

            on("skip a tag with an attribute") {
                val reader = xmlReader("<test attribute=\"value\"/>")
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }

            on("skip a tag with a nested tag") {
                val reader = xmlReader("<test1><test2/></test1>")
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_DOCUMENT") {
                    assertEquals(XmlReader.Token.END_DOCUMENT, token)
                }
            }

            on("skip an inner tag") {
                val reader = xmlReader("<test1><test2/></test1>")
                reader.beginTag()
                reader.beginTag()
                reader.skipTag()
                val token = reader.peek()

                it ("should peek an END_TAG") {
                    assertEquals(XmlReader.Token.END_TAG, token)
                }
            }

            // Error

            on("an incorrectly started tag") {
                val reader = xmlReader("test")
                var error = fails {
                    reader.beginTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Expected BEGIN_TAG but was TEXT at path /", error?.getMessage())
                }
            }

            on("an incorrectly closed self-closing tag because of end of input") {
                val reader = xmlReader("<test/")
                reader.beginTag()
                var error = fails {
                    reader.endTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("End of input", error?.getMessage())
                }
            }

            on("an incorrectly closed self-closing tag because of wrong closing char") {
                val reader = xmlReader("<test/!")
                reader.beginTag()
                var error = fails {
                    reader.endTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Expected '>' but was '!' at path /test", error?.getMessage())
                }
            }

            on("an incorrectly closed end tag because of end of input") {
                val reader = xmlReader("<test></test")
                reader.beginTag()
                val error = fails {
                    reader.endTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Expected '>' at path /test", error?.getMessage())
                }
            }

            on("an incorrectly closed end tag because of wrong name") {
                val reader = xmlReader("<test></nope>")
                reader.beginTag()
                val error = fails {
                    reader.endTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Mismatched tags: Expected 'test' but was 'nope' at path /test", error?.getMessage())
                }
            }

            on("an incorrectly closed end tag because of wrong closing char") {
                val reader = xmlReader("<test></test!")
                reader.beginTag()
                val error = fails {
                    reader.endTag()
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Expected '>' but was '!' at path /test", error?.getMessage())
                }
            }

            on("a incorrectly duplicated attribute") {
                val reader = xmlReader("<test attribute='value1' attribute='value2/>")
                val attribute = Namespaced()
                reader.beginTag()
                reader.nextAttribute(attribute)
                reader.nextValue()
                val error = fails {
                    reader.nextAttribute(attribute)
                }

                it ("should throw a syntax Exception") {
                    assertEquals("Duplicate attribute 'attribute' at path /test", error?.getMessage())
                }
            }
        }
    }
}