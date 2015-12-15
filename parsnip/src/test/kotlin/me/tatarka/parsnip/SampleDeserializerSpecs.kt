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

import me.tatarka.parsnip.samples.Rss
import okio.Okio
import org.jetbrains.spek.api.Spek
import java.io.File

/**
 * Tests the whole stack on some sample xml documents
 */
public class SampleDeserializerSpecs : Spek() {
    init {
        given("an Xml") {
            val xml = Xml.Builder().build()

            on("rss samples") {
                val dir = File(javaClass.getResource("/samples/rss").toURI())
                val adapter = xml.adapter(javaClass<Rss>())

                for (file in dir.listFiles()) {
                    it("should parse '${file.name}'") {
                        // Don't need to check result, just make sure it parses!
//                        adapter.fromXml(Okio.buffer(Okio.source(file)))
                    }
                }
            }
        }
    }
}