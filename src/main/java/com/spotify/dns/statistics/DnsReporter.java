package com.spotify.dns.statistics;

/**
 * Implement to report statistics for DNS request.
 *
 * This interface exists to allow implementors to bridge the statistics
 * collected through the use of this library with their own statistics solution.
 *
 * @author udoprog
 */
public interface DnsReporter {
  /**
   * Report resolve timing.
   * @return A new timing context.
   */
  DnsTimingContext resolveTimer();

  /**
   * Report that an empty response has been received from a resolve.
   */
  void reportEmpty();

  /**
   * Report that a resolve resulting in a failure.
   * @param error The exception causing the failure.
   */
  void reportFailure(Throwable error);
}
