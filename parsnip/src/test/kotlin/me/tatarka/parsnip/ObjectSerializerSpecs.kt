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

package me.tatarka.parsnip

import me.tatarka.parsnip.classes.*
import me.tatarka.parsnip.Xml
import org.jetbrains.spek.api.Spek
import kotlin.test.assertEquals

class ObjectSerializerSpecs : Spek() {
    init {
        given("an Xml") {
            val xml = Xml.Builder().build()

            on("an empty object") {
                val adapter = xml.adapter(javaClass<EmptyObject>())
                val emptyObject = EmptyObject()
                val result = adapter.toXml(emptyObject)

                it("should write the object as a root tag") {
                    assertEquals("<EmptyObject/>", result)
                }
            }

            on("an object with primitive attribute fields") {
                val adapter = xml.adapter(javaClass<PrimitiveObject>())
                val primitiveObject = PrimitiveObject(
                        boolean = true,
                        byte = 1,
                        char = 'a',
                        double = 1.0,
                        float = 1.0f,
                        int = 1,
                        long = 1L,
                        short = 1
                )
                val result = adapter.toXml(primitiveObject)

                it("should write the object with the fields as attributes") {
                    assertEquals("<PrimitiveObject boolean=\"true\" byte=\"1\" char=\"a\" double=\"1.0\" float=\"1.0\" int=\"1\" long=\"1\" short=\"1\"/>", result)
                }
            }

            on("an object with a string attribute field") {
                val adapter = xml.adapter(javaClass<StringObject>())
                val stringObject = StringObject(string1 = "test", string2 = null)
                val result = adapter.toXml(stringObject)

                it("should write the object with a string field attribute") {
                    assertEquals("<StringObject string1=\"test\"/>", result)
                }
            }

            on("an object with an enum attribute field") {
                val adapter = xml.adapter(javaClass<EnumObject>())
                val enumObject = EnumObject(enum1 = TestEnum.One, enum2 = TestEnum.Two)
                val result = adapter.toXml(enumObject)

                it("should write the enum attributes") {
                    assertEquals("<EnumObject enum1=\"One\" enum2=\"Two\"/>", result)
                }
            }

            on("an object with a text field") {
                val adapter = xml.adapter(javaClass<TextObject>())
                val textObject = TextObject(text = "test")
                val result = adapter.toXml(textObject)

                it("should write the tag with text") {
                    assertEquals("<TextObject>test</TextObject>", result)
                }
            }

            on("an object with a tag field") {
                val adapter = xml.adapter(javaClass<TagObject>())
                val tagObject = TagObject(text = "test", items = listOf("test1", "test2"))
                val result = adapter.toXml(tagObject)

                it("should write the text as a tag") {
                    assertEquals("<TagObject><text>test</text><item>test1</item><item>test2</item></TagObject>", result)
                }
            }

            on("an object with a nested one") {
                val adapter = xml.adapter(javaClass<NestedObject>())
                val nestedObject = NestedObject(StringObject(string1 = "test", string2 = null))
                val result = adapter.toXml(nestedObject)

                it("should write nested tags") {
                    assertEquals("<NestedObject><nested string1=\"test\"/></NestedObject>", result)
                }
            }

            on("an object with a namespace attribute") {
                val adapter = xml.adapter(javaClass<NamespaceObject>())
                val namespaceObject = NamespaceObject("value", StringObject("value", null), listOf(StringObject("test1", null), StringObject("test2", null)))
                val result = adapter.toXml(namespaceObject)

                it("should write a namespaced attribute") {
                    assertEquals("<NamespaceObject xmlns:ns=\"foo\" ns:attribute=\"value\"><ns:tag string1=\"value\"/><ns:StringObject string1=\"test1\"/><ns:StringObject string1=\"test2\"/></NamespaceObject>", result)
                }
            }
            
            on("an object with an attribute and tag of the same name") {
                val adapter = xml.adapter(javaClass<SameNameObject>())
                val sameNameObject = SameNameObject("value", StringObject("value", null))
                val result = adapter.toXml(sameNameObject)
                
                it("should write the attribute and tag of the same name") {
                    assertEquals("<SameNameObject name=\"value\"><name string1=\"value\"/></SameNameObject>", result)
                }
            }
        }
    }
}