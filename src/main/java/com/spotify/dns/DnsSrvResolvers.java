/*
 * Copyright (c) 2015 Spotify AB
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

import com.spotify.dns.statistics.DnsReporter;

import org.xbill.DNS.Lookup;

import static com.google.common.primitives.Ints.checkedCast;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Provides builders for configuring and instantiating {@link DnsSrvResolver}s.
 */
public final class DnsSrvResolvers {

  private static final int DEFAULT_DNS_TIMEOUT_SECONDS = 5;
  private static final int DEFAULT_RETENTION_DURATION_HOURS = 2;

  public static DnsSrvResolverBuilder newBuilder() {
    return new DnsSrvResolverBuilder();
  }

  public static final class DnsSrvResolverBuilder {

    private final DnsReporter reporter;
    private final boolean retainData;
    private final boolean cacheLookups;
    private final boolean useLookupCache;
    private final long dnsLookupTimeoutMillis;
    private final long retentionDurationMillis;

    private DnsSrvResolverBuilder() {
      this(null,
           false,
           false,
           true,
           SECONDS.toMillis(DEFAULT_DNS_TIMEOUT_SECONDS),
           HOURS.toMillis(DEFAULT_RETENTION_DURATION_HOURS));
    }

    private DnsSrvResolverBuilder(
        DnsReporter reporter,
        boolean retainData,
        boolean cacheLookups,
        boolean useLookupCache,
        long dnsLookupTimeoutMillis,
        long retentionDurationMillis) {
      this.reporter = reporter;
      this.retainData = retainData;
      this.cacheLookups = cacheLookups;
      this.useLookupCache = useLookupCache;
      this.dnsLookupTimeoutMillis = dnsLookupTimeoutMillis;
      this.retentionDurationMillis = retentionDurationMillis;
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

      if (!useLookupCache) {
          lookupFactory = new NoCachingLookupFactory(lookupFactory);
      }

      if (cacheLookups) {
        lookupFactory = new CachingLookupFactory(lookupFactory);
      }

      DnsSrvResolver result = new XBillDnsSrvResolver(lookupFactory);

      if (reporter != null) {
        result = new MeteredDnsSrvResolver(result, reporter);
      }

      if (retainData) {
        result = new RetainingDnsSrvResolver(result, retentionDurationMillis);
      }

      return result;
    }

    public DnsSrvResolverBuilder metered(DnsReporter reporter) {
      return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                                       retentionDurationMillis);
    }

    public DnsSrvResolverBuilder retainingDataOnFailures(boolean retainData) {
      return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                                       retentionDurationMillis);
    }

    public DnsSrvResolverBuilder cachingLookups(boolean cacheLookups) {
      return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                                       retentionDurationMillis);
    }

      public DnsSrvResolverBuilder useLookupCache(boolean useLookupCache) {
          return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                  retentionDurationMillis);
      }


      public DnsSrvResolverBuilder dnsLookupTimeoutMillis(long dnsLookupTimeoutMillis) {
      return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                                       retentionDurationMillis);
    }

    public DnsSrvResolverBuilder retentionDurationMillis(long retentionDurationMillis) {
      return new DnsSrvResolverBuilder(reporter, retainData, cacheLookups, useLookupCache, dnsLookupTimeoutMillis,
                                       retentionDurationMillis);
    }
  }

  private DnsSrvResolvers() {
    // prevent instantiation
  }
}
