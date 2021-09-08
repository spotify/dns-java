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
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Resolver;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;

/** Provides builders for configuring and instantiating {@link DnsSrvResolver}s. */
public final class DnsSrvResolvers {

  private static final int DEFAULT_DNS_TIMEOUT_SECONDS = 5;
  private static final int DEFAULT_RETENTION_DURATION_HOURS = 2;

  private DnsSrvResolvers() {
    // prevent instantiation
  }

  public static DnsSrvResolverBuilder newBuilder() {
    return new DnsSrvResolverBuilder();
  }

  public static final class DnsSrvResolverBuilder {

    private final DnsReporter reporter;
    private final boolean retainData;
    private final boolean cacheLookups;
    private final long dnsLookupTimeoutMillis;
    private final long retentionDurationMillis;
    private final List<String> servers;
    private final Resolver resolver;

    private DnsSrvResolverBuilder() {
      this(
          null,
          false,
          false,
          SECONDS.toMillis(DEFAULT_DNS_TIMEOUT_SECONDS),
          HOURS.toMillis(DEFAULT_RETENTION_DURATION_HOURS),
          null,
          null);
    }

    private DnsSrvResolverBuilder(
        DnsReporter reporter,
        boolean retainData,
        boolean cacheLookups,
        long dnsLookupTimeoutMillis,
        long retentionDurationMillis,
        List<String> servers,
        Resolver resolver) {
      this.reporter = reporter;
      this.retainData = retainData;
      this.cacheLookups = cacheLookups;
      this.dnsLookupTimeoutMillis = dnsLookupTimeoutMillis;
      this.retentionDurationMillis = retentionDurationMillis;
      this.servers = servers;
      this.resolver = resolver;
    }

    public DnsSrvResolver build() {
      Resolver resolver;

      if(this.resolver != null) {
        resolver = this.resolver;
      } else {
        try {
          // If the user specified DNS servers, create a new ExtendedResolver which uses them.
          // Otherwise, use the default constructor. That will use the servers in ResolverConfig,
          // or if that's empty, localhost.
          resolver =
              servers == null
                  ? new ExtendedResolver()
                  : new ExtendedResolver(servers.toArray(new String[servers.size()]));
        } catch (UnknownHostException e) {
          throw new RuntimeException(e);
        }
      }

      // Configure the Resolver to use our timeouts.
      final Duration timeoutDuration = Duration.ofMillis(dnsLookupTimeoutMillis);
      resolver.setTimeout(timeoutDuration);

      LookupFactory lookupFactory = new SimpleLookupFactory(resolver);

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
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    public DnsSrvResolverBuilder retainingDataOnFailures(boolean retainData) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    public DnsSrvResolverBuilder cachingLookups(boolean cacheLookups) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    public DnsSrvResolverBuilder dnsLookupTimeoutMillis(long dnsLookupTimeoutMillis) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    public DnsSrvResolverBuilder retentionDurationMillis(long retentionDurationMillis) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    /**
     * Allows the user to specify which DNS servers should be used to perform DNS lookups. Servers
     * can be specified using either hostname or IP address. If not specified, the underlying DNS
     * library will determine which servers to use according to the steps documented in <a
     * href="https://github.com/dnsjava/dnsjava/blob/master/org/xbill/DNS/ResolverConfig.java">
     * ResolverConfig.java</a>
     *
     * @param servers the DNS servers to use
     * @return this builder
     */
    public DnsSrvResolverBuilder servers(List<String> servers) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }

    /**
     * Allows the user to specify a custom resolver to perform the actual DNS lookup, useful for
     * things like testing & custom protocols
     *
     * @param resolver
     * @return
     */
    public DnsSrvResolverBuilder resolver(Resolver resolver) {
      return new DnsSrvResolverBuilder(
          reporter,
          retainData,
          cacheLookups,
          dnsLookupTimeoutMillis,
          retentionDurationMillis,
          servers,
          resolver);
    }
  }
}
