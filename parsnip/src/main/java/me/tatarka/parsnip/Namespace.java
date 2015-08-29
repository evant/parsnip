package me.tatarka.parsnip;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an xml namespace.
 */
public final class Namespace {
    String alias;
    String namespace;
    
    public Namespace() {
    }
   
    public Namespace(@Nullable String alias, @Nullable String namespace) {
        this.alias = alias;
        this.namespace = namespace;
    }

    /**
     * The namespace alias (xmlns:foo). This may be null if there is no namespace or this represents
     * a default namespace.
     */
    @Nullable
    public String alias() {
        return alias;
    }

    /**
     * The namespace (http://www.example.com). This may be null if there is no namespace.
     */
    @Nullable
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
