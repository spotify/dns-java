package com.spotify.dns.statistics;

/**
 * Implement to handle timings when performing dns requests.
 *
 * @author udoprog
 */
public interface DnsTimingContext {
  void stop();
}
