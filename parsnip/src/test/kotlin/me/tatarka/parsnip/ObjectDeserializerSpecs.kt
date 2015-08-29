package me.tatarka.parsnip

import me.tatarka.parsnip.classes.*
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ObjectDeserializerSpecs : Spek() {
    init {
        given("an Xml") {
            val xml = Xml.Builder().build()

            on("an empty object") {
                val adapter = xml.adapter(javaClass<EmptyObject>())
                val emptyObject = adapter.fromXml("<EmptyObject/>")

                it("should create the object") {
                    assertNotNull(emptyObject)
                }
            }

            on("an object with a primitive attribute fields") {
                val adapter = xml.adapter(javaClass<PrimitiveObject>())
                val primitiveObject = adapter.fromXml("<PrimitiveObject boolean=\"true\" byte=\"1\" char=\"a\" double=\"1.0\" float=\"1.0\" int=\"1\" long=\"1\" short=\"1\" />")

                it("should create the object") {
                    assertNotNull(primitiveObject)
                }

                it("should set the boolean field") {
                    assertEquals(true, primitiveObject.boolean)
                }

                it("should set the byte field") {
                    assertEquals(1.toByte(), primitiveObject.byte)
                }

                it("should set the char field") {
                    assertEquals('a', primitiveObject.char)
                }

                it("should set the double field") {
                    assertEquals(1.0, primitiveObject.double)
                }

                it("should set the float field") {
                    assertEquals(1.0F, primitiveObject.float)
                }

                it("should set the int field") {
                    assertEquals(1, primitiveObject.int)
                }

                it("should set the long field") {
                    assertEquals(1L, primitiveObject.long)
                }

                it("should set the short field") {
                    assertEquals(1.toShort(), primitiveObject.short)
                }
            }

            on("an object with a string attribute field") {
                val adapter = xml.adapter(javaClass<StringObject>())
                val stringObject = adapter.fromXml("<StringObject string1=\"test\" />")

                it("should set the string1 field") {
                    assertEquals("test", stringObject.string1)
                }

                it("should not set the string2 field") {
                    assertNull(stringObject.string2)
                }
            }

            on("an object with an enum attribute field") {
                val adapter = xml.adapter(javaClass<EnumObject>())
                val enumObject = adapter.fromXml("<EnumObject enum1=\"One\" enum2=\"Two\" />")

                it("should set the enum1 field") {
                    assertEquals(TestEnum.One, enumObject.enum1)
                }

                it("should set the enum2 field") {
                    assertEquals(TestEnum.Two, enumObject.enum2)
                }
            }

            on("an object with a text field") {
                val adapter = xml.adapter(javaClass<TextObject>())
                val textObject = adapter.fromXml("<TextObject>test</TextObject>")

                it("should set the text field") {
                    assertEquals("test", textObject.text)
                }
            }

            on("an object with a tag field") {
                val adapter = xml.adapter(javaClass<TagObject>())
                val tagObject = adapter.fromXml("<TagObject><text>test</text></TagObject>")

                it("should set the text field") {
                    assertEquals("test", tagObject.text)
                }
            }

            on("an object with a nested one") {
                val adapter = xml.adapter(javaClass<NestedObject>())
                val nestedObject = adapter.fromXml("<NestedObject><nested string1=\"test\"/></NestedObject>")

                it("should set nested field") {
                    assertNotNull(nestedObject.nested)
                }

                it("should set the nested object string1 field") {
                    assertEquals("test", nestedObject.nested.string1)
                }
            }

            on("an object with a collection of tags") {
                val adapter = xml.adapter(javaClass<CollectionObject>())
                val collectionObject = adapter.fromXml("<CollectionObject><StringObject string1=\"test1\"/><StringObject string1=\"test2\"/></CollectionObject>")

                it("should read the items into a collection") {
                    assertEquals(listOf(StringObject("test1", null), StringObject("test2", null)), collectionObject.collection)
                }
            }

            on("an object with namespaces") {
                val adapter = xml.adapter(javaClass<NamespaceObject>())
                val namespaceObject = adapter.fromXml("<NamespaceObject xmlns:ns=\"foo\" ns:attribute=\"value\" attribute=\"notValue\"><ns:tag string1=\"test\"/></NamespaceObject>")

                it("should read the namespaced attribute, not the other one") {
                    assertEquals("value", namespaceObject.attribute)
                }

                it("should read the namespaced tag") {
                    assertEquals(StringObject("test", null), namespaceObject.tag)
                }
            }
            
            on("an attribute and tag of the same name") {
                val adapter = xml.adapter(javaClass<SameNameObject>())
                val sameNameObject = adapter.fromXml("<SameNameObject name=\"value\"><name string1=\"value\"/></SameNameObject>")
                
                it("should read the attribute") {
                    assertEquals("value", sameNameObject.attribute)
                }
                
                it("should read the tag") {
                    assertEquals(StringObject("value", null), sameNameObject.tag)
                }
            }
        }
    }
}