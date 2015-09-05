package me.tatarka.parsnip.classes

import me.tatarka.parsnip.annotations.SerializedName
import me.tatarka.parsnip.annotations.Tag

data
public class TagObject(@Tag val text: String, @SerializedName("item") @Tag val items: List<String>)
