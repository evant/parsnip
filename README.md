# parsnip
 A modern XML library for Android and Java

## Why parsnip?
- Fast streaming parser based on [okio](https://github.com/square/okio).
- Simple modern api similar to [gson](https://github.com/google/gson) and [moshi](https://github.com/square/moshi).

## Why not parsnip?
- UTF-8 only
- Not a validating parser
- Doesn't support custom entities

## Usage

### Parsing into objects
```java
String xml = ...;

Xml xml = new Xml.Builder().build();
XmlAdapter<BlackjackHand> xmlAdapter = xml.adapter(BlackjackHand.class);

BlackjackHand blackjackHand = xmlAdapter.fromXml(xml);
```

### Serialize objects into xml
```java
BlackjackHand blackjackHand = new BlackjackHand(
    new Card('6', SPADES),
    Arrays.asList(new Card('4', CLUBS), new Card('A', HEARTS)));

Xml xml = new Xml.Builder().build();
XmlAdapter<BlackjackHand> xmlAdapter = xml.adapter(BlackjackHand.class);

String xml = xmlAdapter.toXml(blackjackHand);
```

### Built in xml adapters
Parsnip has built-in support for reading and writing
- primative types
- arrays, collections and lists
- Strings
- enums

It supports classes by writing them out field-by-field. Primaitves will be written out as attributes by default, classes will be written out as tags.

If you have these classes:
```java
class BlackjackHand {
  public final Card hiddenCard;
  public final List<Card> visibleCards;
  ...
}

class Card {
  public final char rank;
  public final Suit suit;
  ...
}

enum Suit {
  CLUBS, DIAMONDS, HEARTS, SPADES;
}
```

Parsnip will read and write this xml:
```xml
<BlackjackHand>
  <hiddenCard rank="6" suit="SPADES"/>
  <Card rank="4" suit="CLUBS"/>
  <Card rank="A" suit="HEARTS"/>
</BlackjackHand>
```

### Custom naming
You can customzie the names of tags and attributes with `@SerializedName()`. The above example will look a little better as such:
```java
class BlackjackHand {
  @SerializedName("HiddenCard")
  public final Card hiddenCard;
  @SerializedName("VisibleCard")
  public final List<Card> visibleCards;
  ...
}

class Card {
  public final char rank;
  public final Suit suit;
  ...
}

enum Suit {
  CLUBS, DIAMONDS, HEARTS, SPADES;
}
```

```xml
<BlackjackHand>
  <HiddenCard rank="6" suit="SPADES"/>
  <VisibleCard rank="4" suit="CLUBS"/>
  <VisibleCard rank="A" suit="HEARTS"/>
</BlackjackHand>
```

### Text
You can use the `@Text` annotation to read/write the text of a tag.
```java
class Card {
  @Text
  public final char rank;
  public final Suit suit;
}
```
```xml
<Card suit="SPADES">6</Card>
```

### Tag
Often times you only care about the contents of a tag, not any of it's attributes. You can save some nesting in your hiarchy with the `@Tag` annotation.
```java
class Card {
  @Tag
  public final char rank;
  @Tag
  public final Suit suit;
}
```
```xml
<Card>
 <rank>6</rank>
 <suit>SPADES</suid>
</Card>
```

## License

    Copyright 2015 Evan Tatarka
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
