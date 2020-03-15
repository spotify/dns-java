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

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A DnsSrvResolver that keeps track of the previous results of a particular query. If
 * available, the previous result is returned in case of a failure, or if a query that used to
 * return valid data starts returning empty results. The purpose is to provide protection against
 * transient failures in the DNS infrastructure. Data is retained for a configurable period of time.
 */
class RetainingDnsSrvResolver implements DnsSrvResolver {
  private final DnsSrvResolver delegate;
  private final Cache<String, List<LookupResult>> cache;

  RetainingDnsSrvResolver(DnsSrvResolver delegate, long retentionTimeMillis) {
    Preconditions.checkArgument(retentionTimeMillis > 0L,
                                "retention time must be positive, was %d", retentionTimeMillis);

    this.delegate = requireNonNull(delegate, "delegate");
    cache = CacheBuilder.newBuilder()
        .expireAfterWrite(retentionTimeMillis, TimeUnit.MILLISECONDS)
        .build();
  }

  @Override
  public List<LookupResult> resolve(final String fqdn) {
    requireNonNull(fqdn, "fqdn");

    try {
      final List<LookupResult> nodes = delegate.resolve(fqdn);

      // No nodes resolved? Return stale data.
      if (nodes.isEmpty()) {
        List<LookupResult> cached = cache.getIfPresent(fqdn);
        return (cached != null) ? cached : nodes;
      }

      cache.put(fqdn, nodes);

      return nodes;
    } catch (Exception e) {
      if (cache.getIfPresent(fqdn) != null) {
        return cache.getIfPresent(fqdn);
      }

      throw Throwables.propagate(e);
    }
  }
}
