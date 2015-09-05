package me.tatarka.parsnip.classes

import me.tatarka.parsnip.annotations.Namespace

data
class NamespaceObject(
        @Namespace("foo", alias = "ns") val attribute: String,
        @Namespace("foo", alias = "ns") val tag: StringObject,
        @Namespace("foo", alias = "ns") val items: List<StringObject>
)
