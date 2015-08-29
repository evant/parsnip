package me.tatarka.parsnip.classes

import me.tatarka.parsnip.annotations.SerializedName

data
class SameNameObject(@SerializedName("name") val attribute: String, @SerializedName("name") val tag: StringObject)
