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

package me.tatarka.parsnip.samples

import me.tatarka.parsnip.annotations.Namespace
import me.tatarka.parsnip.annotations.SerializedName
import me.tatarka.parsnip.annotations.Tag

const val ATOM_NS: String = "http://www.w3.org/2005/Atom";
const val ITUNES_NS: String = "http://www.itunes.com/dtds/podcast-1.0.dtd";

data
@SerializedName("rss")
public class Rss(val channel: Channel)

data
public class Channel(
        @Namespace(ATOM_NS) val link: Link,
        @Tag val title: String,
        @Namespace(ITUNES_NS) val image: Image,
        @Tag val description: String,
        @Tag val author: String,
        @Tag val copyright: String
)

data
public class Image(val href: String)

data
public class Link(val href: String)