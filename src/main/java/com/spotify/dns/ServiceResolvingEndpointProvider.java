/*
 * Copyright (c) 2012-2015 Spotify AB
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

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An endpoint provider that resolves and provides tcp:// endpoints for a service using DNS. The
 * endpoints are refreshed at a configurable interval.
 */
class ServiceResolvingEndpointProvider<T> extends AbstractEndpointProvider<T> {

  private static final Logger log = LoggerFactory.getLogger(ServiceResolvingEndpointProvider.class);
  private static final ScheduledExecutorService executor =
      MoreExecutors.getExitingScheduledExecutorService(
          new ScheduledThreadPoolExecutor(
              1, new ThreadFactoryBuilder().setNameFormat("dns-lookup-%d").build()),
              0, TimeUnit.SECONDS);


  private final DnsSrvResolver resolver;
  private final ScheduledFuture<?> updaterFuture;
  private final Function<LookupResult, T> resultTransformer;

  private final String fqdn;

  private volatile Set<T> endpoints = Collections.emptySet();

  /**
   * Create an endpoint provider that provides endpoints using a srv resolver.
   *
   * @param resolver            The resolver to use.
   * @param refreshInterval     The interval to refresh the DNS resolution.
   * @param refreshIntervalUnit The timeunit of the refresh interval.
   * @param fqdn                The name to lookup SRV records for
   * @param resultTransformer   TODO
   */
  public ServiceResolvingEndpointProvider(final DnsSrvResolver resolver,
                                          final long refreshInterval,
                                          final TimeUnit refreshIntervalUnit,
                                          final String fqdn,
                                          final Function<LookupResult, T> resultTransformer) {
    checkArgument(refreshInterval > 0);
    this.resolver = checkNotNull(resolver);
    this.resultTransformer = checkNotNull(resultTransformer, "resultTransformer");
    this.fqdn = checkNotNull(fqdn, "fqdn");

    updaterFuture =
        executor.scheduleWithFixedDelay(new Updater(), 0, refreshInterval, refreshIntervalUnit);
  }

  @Override
  public void closeImplementation() {
    updaterFuture.cancel(false);
  }

  @Override
  public Set<T> getEndpoints() {
    return Collections.unmodifiableSet(endpoints);
  }

  private class Updater implements Runnable {

    @Override
    public void run() {
      try {
        List<LookupResult> nodes = resolver.resolve(fqdn);
        Set<T> currentEndpoints = Sets.newHashSet();
        for (LookupResult node : nodes) {
          currentEndpoints.add(resultTransformer.apply(node));
        }
        if (!currentEndpoints.equals(endpoints)) {
          endpoints = currentEndpoints;
          fireEndpointsUpdated();
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}

