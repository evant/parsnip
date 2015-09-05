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

/**
 * Represents an xml namespace.
 */
public final class Namespace {
    String alias;
    String namespace;
    
    public Namespace() {
    }
   
    public Namespace(String alias, String namespace) {
        this.alias = alias;
        this.namespace = namespace;
    }

    /**
     * The namespace alias (xmlns:foo). This may be null if there is no namespace or this represents
     * a default namespace.
     */
    public String alias() {
        return alias;
    }

    /**
     * The namespace (http://www.example.com). This may be null if there is no namespace.
     */
    public String namespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Namespace namespace1 = (Namespace) o;

        return !(namespace != null ? !namespace.equals(namespace1.namespace) : namespace1.namespace != null);

    }

    @Override
    public int hashCode() {
        return namespace != null ? namespace.hashCode() : 0;
    }
}
