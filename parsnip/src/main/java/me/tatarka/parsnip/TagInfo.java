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

import me.tatarka.parsnip.annotations.Namespace;

public class TagInfo {
    public static final TagInfo ROOT = new TagInfo(null, null, null);

    private String name;
    private String namespace;
    private String alias;
    
    public TagInfo(String name, Namespace namespace) {
        this.name = name;
        this.namespace = namespace != null ? namespace.value() : null;
        this.alias = namespace != null ? namespace.alias() : null;
    }

    public TagInfo(String name, String namespace, String alias) {
        this.name = name;
        this.namespace = namespace;
    }

    public String name() {
        return name;
    }

    public String namespace() {
        return namespace;
    }

    public String alias() {
        return alias;
    }
}
