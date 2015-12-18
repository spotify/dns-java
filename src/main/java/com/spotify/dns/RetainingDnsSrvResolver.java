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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A caching DnsSrvResolver that keeps track of the previous results of a particular query. If
 * available, the previous result is returned in case of a failure, or if a query that used to
 * return valid data starts returning empty results. The purpose is to provide protection against
 * transient failures in the DNS infrastructure. The cached result is persisted for two hours.
 */
class RetainingDnsSrvResolver implements DnsSrvResolver {
  public final static long VALID_CACHE_LIFETIME = 1000 * 60 * 60 * 2;  // 2 hrs

  private final DnsSrvResolver delegate;
  private final Map<String, List<LookupResult>> cache;

  private Date lastValidResultDate;

  RetainingDnsSrvResolver(DnsSrvResolver delegate) {
    this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    cache = new ConcurrentHashMap<String, List<LookupResult>>();
  }

  @Override
  public List<LookupResult> resolve(final String fqdn) {
    Preconditions.checkNotNull(fqdn, "fqdn");

    try {
      final List<LookupResult> nodes = delegate.resolve(fqdn);

      // No nodes resolved? Return stale data if the cache is still valid.
      if (nodes.isEmpty()) {
        final Date now = new Date();
        final List<LookupResult> cached = cache.get(fqdn);
        return (cached != null && now.getTime() - lastValidResultDate.getTime() < VALID_CACHE_LIFETIME)
            ? cached : nodes;
      } else {
        lastValidResultDate = new Date();
        cache.put(fqdn, nodes);
        return nodes;
      }
    } catch (Exception e) {
      if (cache.containsKey(fqdn)) {
        return cache.get(fqdn);
      }

      throw Throwables.propagate(e);
    }
  }

  @VisibleForTesting
  void setLastValidResultDate(final Date date) {
    lastValidResultDate = date;
  }
}
