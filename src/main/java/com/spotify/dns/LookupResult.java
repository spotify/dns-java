package com.spotify.dns;

import static java.util.Objects.requireNonNull;

/**
 * Immutable data object with the relevant parts of an SRV record.
 */
public class LookupResult {

  private final String host;
  private final int port;
  private final int priority;
  private final int weight;
  private final long ttl;

  private LookupResult(final String host, final int port, final int priority, final int weight,
                       final long ttl) {
    this.host = requireNonNull(host, "host");
    this.port = port;
    this.priority = priority;
    this.weight = weight;
    this.ttl = ttl;
  }

  public static LookupResult create(String host, int port, int priority, int weight, long ttl) {
    return new LookupResult(host, port, priority, weight, ttl);
  }

  public String host() {
    return host;
  }

  public int port() {
    return port;
  }

  public int priority() {
    return priority;
  }

  public int weight() {
    return weight;
  }

  public long ttl() {
    return ttl;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final LookupResult that = (LookupResult) o;

    if (port != that.port) {
      return false;
    }
    if (priority != that.priority) {
      return false;
    }
    if (weight != that.weight) {
      return false;
    }
    if (ttl != that.ttl) {
      return false;
    }
    return !(host != null ? !host.equals(that.host) : that.host != null);

  }

  @Override
  public int hashCode() {
    int result = host != null ? host.hashCode() : 0;
    result = 31 * result + port;
    result = 31 * result + priority;
    result = 31 * result + weight;
    result = 31 * result + (int) (ttl ^ (ttl >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "LookupResult{" +
           "host='" + host + '\'' +
           ", port=" + port +
           ", priority=" + priority +
           ", weight=" + weight +
           ", ttl=" + ttl +
           '}';
  }
}
