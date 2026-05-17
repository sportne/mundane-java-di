package io.github.mundanej.mjdi.generator.fixtures.valid;

import io.github.mundanej.mjdi.Inject;
import io.github.mundanej.mjdi.Named;

public final class ScalarConsumer {
  private final String text;
  private final boolean enabled;
  private final int count;
  private final long distance;
  private final double ratio;
  private final float scale;
  private final short small;
  private final byte tiny;
  private final char letter;

  @Inject
  public ScalarConsumer(
      @Named("text") String text,
      @Named("enabled") boolean enabled,
      @Named("count") int count,
      @Named("distance") long distance,
      @Named("ratio") double ratio,
      @Named("scale") float scale,
      @Named("small") short small,
      @Named("tiny") byte tiny,
      @Named("letter") char letter) {
    this.text = text;
    this.enabled = enabled;
    this.count = count;
    this.distance = distance;
    this.ratio = ratio;
    this.scale = scale;
    this.small = small;
    this.tiny = tiny;
    this.letter = letter;
  }

  public String text() {
    return text;
  }

  public boolean enabled() {
    return enabled;
  }

  public int count() {
    return count;
  }

  public long distance() {
    return distance;
  }

  public double ratio() {
    return ratio;
  }

  public float scale() {
    return scale;
  }

  public short small() {
    return small;
  }

  public byte tiny() {
    return tiny;
  }

  public char letter() {
    return letter;
  }
}
