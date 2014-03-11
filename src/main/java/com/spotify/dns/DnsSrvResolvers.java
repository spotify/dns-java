/*
 * Copyright (c) 2012-2014 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotify.dns;

import com.spotify.statistics.MuninGraphCategoryConfig;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricName;
import org.xbill.DNS.Lookup;

import java.util.concurrent.TimeUnit;

import static com.google.common.primitives.Ints.checkedCast;
import static com.spotify.statistics.Property.CounterProperty;
import static com.spotify.statistics.Property.TimerProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Provides utility methods for instantiating and working with DnsSrvResolvers.
 */
public final class DnsSrvResolvers {
  public static final MetricName TIMER_NAME = new MetricName(DnsSrvResolver.class, "lookups");
  public static final MetricName FAILURES_NAME = new MetricName(DnsSrvResolver.class, "failures");
  public static final MetricName EMPTY_RESULTS_NAME = new MetricName(DnsSrvResolver.class, "emptyResults");

  private static final int DEFAULT_DNS_TIMEOUT_SECONDS = 5;

  public static DnsSrvResolverBuilder newBuilder() {
    return new DnsSrvResolverBuilder(false, false, false, SECONDS.toMillis(DEFAULT_DNS_TIMEOUT_SECONDS));
  }

  public static void configureMuninGraphs(MuninGraphCategoryConfig category) {
    category.graph("DNS Lookups")
        .muninName("dns_lookups")
        .vlabel("r/s")
        .dataSource(TIMER_NAME, "total", TimerProperty.COUNT)
        .dataSource(TIMER_NAME, "5 min rate", TimerProperty.FIVE_MINUTE_RATE)
        .dataSource(FAILURES_NAME, "failure", CounterProperty.COUNT)
        .dataSource(EMPTY_RESULTS_NAME, "empty", CounterProperty.COUNT);

    category.graph("DNS lookup durations")
        .muninName("dns_lookup_durations")
        .dataSource(TIMER_NAME, "50%", TimerProperty.MEAN)
        .dataSource(TIMER_NAME, "95%", TimerProperty.PERCENTILE95)
        .dataSource(TIMER_NAME, "99%", TimerProperty.PERCENTILE99)
        .dataSource(TIMER_NAME, "stddev", TimerProperty.STD_DEV);
  }

  public static final class DnsSrvResolverBuilder {
    private final boolean metered;
    private final boolean retainData;
    private final boolean cacheLookups;
    private final long dnsLookupTimeoutMillis;

    private DnsSrvResolverBuilder(
        boolean metered,
        boolean retainData,
        boolean cacheLookups,
        long dnsLookupTimeoutMillis) {

      this.metered = metered;
      this.retainData = retainData;
      this.cacheLookups = cacheLookups;
      this.dnsLookupTimeoutMillis = dnsLookupTimeoutMillis;
    }

    public DnsSrvResolver build() {
      // NOTE: this sucks, but is the only reasonably sane way to set a timeout in dnsjava...
      // the effect of doing this is to set a global timeout for all Lookup instances - except
      // those that potentially get a new Resolver assigned via the setResolver method... Since
      // Lookup instances are mostly encapsulated in this library, we should be fine.
      int timeoutSecs = checkedCast(MILLISECONDS.toSeconds(dnsLookupTimeoutMillis));
      int millisRemainder = checkedCast(dnsLookupTimeoutMillis - SECONDS.toMillis(timeoutSecs));

      Lookup.getDefaultResolver().setTimeout(timeoutSecs, millisRemainder);

      LookupFactory lookupFactory = new SimpleLookupFactory();

      if (cacheLookups) {
        lookupFactory = new CachingLookupFactory(lookupFactory);
      }

      DnsSrvResolver result = new XBillDnsSrvResolver(lookupFactory);

      if (metered) {
        result = new MeteredDnsSrvResolver(
            result,
            Metrics.newTimer(TIMER_NAME, TimeUnit.MILLISECONDS, TimeUnit.SECONDS),
            Metrics.newCounter(FAILURES_NAME),
            Metrics.newCounter(EMPTY_RESULTS_NAME)
        );
      }

      if (retainData) {
        result = new RetainingDnsSrvResolver(result);
      }

      return result;
    }

    public DnsSrvResolverBuilder metered(boolean metered) {
      return new DnsSrvResolverBuilder(metered, retainData, cacheLookups, dnsLookupTimeoutMillis);
    }

    public DnsSrvResolverBuilder retainingDataOnFailures(boolean retainData) {
      return new DnsSrvResolverBuilder(metered, retainData, cacheLookups, dnsLookupTimeoutMillis);
    }

    public DnsSrvResolverBuilder cachingLookups(boolean cacheLookups) {
      return new DnsSrvResolverBuilder(metered, retainData, cacheLookups, dnsLookupTimeoutMillis);
    }

    public DnsSrvResolverBuilder dnsLookupTimeoutMillis(long dnsLookupTimeoutMillis) {
      return new DnsSrvResolverBuilder(metered, retainData, cacheLookups, dnsLookupTimeoutMillis);
    }
  }

  private DnsSrvResolvers() {
    // prevent instantiation
  }
}
