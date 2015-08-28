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

It supports classes by writing them out field-by-field. Primaitves will be written out as attributes by default, classes will be written out has tags.

If you have these classes:
```java
class BlackjackHand {
  public final Card hiddenCcard;
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
```java
<BlackjackHand>
  <hiddenCard rank="6" suit="SPADES"/>
  <Card rank="4" suit="CLUBS"/>
  <Card rank="A" suit="HEARTS"/>
</BlackjackHand>
```

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

```java
<BlackjackHand>
  <HiddenCard rank="6" suit="SPADES"/>
  <VisibleCard rank="4" suit="CLUBS"/>
  <VisibleCard rank="A" suit="HEARTS"/>
</BlackjackHand>
```
