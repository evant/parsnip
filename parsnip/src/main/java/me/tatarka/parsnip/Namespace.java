package me.tatarka.parsnip;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an xml namespace.
 */
public final class Namespace {
    String alias;
    String namespace;

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
}
