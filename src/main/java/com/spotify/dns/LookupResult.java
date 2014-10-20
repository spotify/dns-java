package com.spotify.dns;

import com.google.auto.value.AutoValue;

/**
 * TODO: document!
 */
@AutoValue
public abstract class LookupResult {
  public abstract String host();
  public abstract int port();
  public abstract int priority();
  public abstract int weight();

  public static LookupResult create(String host, int port, int priority, int weight) {
    return new AutoValue_LookupResult(host, port, priority, weight);
  }
}
