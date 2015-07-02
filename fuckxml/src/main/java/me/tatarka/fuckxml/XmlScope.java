package me.tatarka.fuckxml;

/**
 * Created by evan on 6/15/15.
 */
final class XmlScope {
    static final int EMPTY_TAG = 1;

    static final int NONEMPTY_TAG = 2;

    static final int DANGLING_ATTRIBUTE = 3;

    static final int INSIDE_TAG = 4;

    static final int EMPTY_DOCUMENT = 5;

    static final int NONEMPTY_DOCUMENT = 6;

    static final int CLOSED = 7;
}
