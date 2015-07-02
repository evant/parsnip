package me.tatarka.fuckxml;

import org.jetbrains.annotations.Nullable;

/**
 * Created by evan on 6/15/15.
 */
public final class Namespaced {
    String namespace;
    String name;

    @Nullable
    public String namespace() {
        return namespace;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        if (namespace == null) {
            return name;
        } else {
            return "{" + namespace + "}" + name;
        }
    }
}
