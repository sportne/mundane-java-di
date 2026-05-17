package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KeyTest {
  @Test
  void equalKeysHaveEqualHashCodes() {
    Key<Widget> first = Key.named(Widget.class, "primary");
    Key<Widget> second = Key.named(Widget.class, "primary");

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
  }

  @Test
  void keysWithDifferentTypesAreNotEqual() {
    Key<Widget> widget = Key.of(Widget.class);
    Key<Service> service = Key.of(Service.class);

    assertNotEquals(widget, service);
  }

  @Test
  void keysWithDifferentNamesAreNotEqual() {
    Key<Widget> primary = Key.named(Widget.class, "primary");
    Key<Widget> secondary = Key.named(Widget.class, "secondary");

    assertNotEquals(primary, secondary);
  }

  @Test
  void namedAndUnnamedKeysAreNotEqual() {
    Key<Widget> unnamed = Key.of(Widget.class);
    Key<Widget> named = Key.named(Widget.class, "primary");

    assertNotEquals(unnamed, named);
  }

  @Test
  void keyDoesNotEqualOtherObjectTypes() {
    assertNotEquals(Key.of(Widget.class), "io.github.mundanej.mjdi.KeyTest$Widget");
  }

  @Test
  void unnamedKeyHasEmptyNameAndTypeNameString() {
    Key<Widget> key = Key.of(Widget.class);

    assertFalse(key.name().isPresent());
    assertEquals(Widget.class.getName(), key.toString());
  }

  @Test
  void namedKeyStringIncludesName() {
    Key<Widget> key = Key.named(Widget.class, "primary");

    assertEquals(Widget.class.getName() + "[primary]", key.toString());
  }

  @Test
  void rejectsNullType() {
    assertThrows(NullPointerException.class, () -> Key.of(null));
    assertThrows(NullPointerException.class, () -> Key.named(null, "primary"));
  }

  @Test
  void rejectsNullName() {
    assertThrows(NullPointerException.class, () -> Key.named(Widget.class, null));
  }

  @Test
  void rejectsEmptyName() {
    assertThrows(IllegalArgumentException.class, () -> Key.named(Widget.class, ""));
  }

  @Test
  void rejectsBlankName() {
    assertThrows(IllegalArgumentException.class, () -> Key.named(Widget.class, " \t"));
  }

  @Test
  void preservesNonblankNameWithoutTrimming() {
    Key<Widget> key = Key.named(Widget.class, " primary ");

    assertEquals(" primary ", key.name().orElseThrow());
    assertEquals(Widget.class.getName() + "[ primary ]", key.toString());
  }

  private static final class Widget {}

  private static final class Service {}
}
