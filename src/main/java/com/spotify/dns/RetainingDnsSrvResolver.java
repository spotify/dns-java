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

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Objects.firstNonNull;

/**
 * A caching DnsSrvResolver that keeps track of the previous results of a particular query. If
 * available, the previous result is returned in case of a failure, or if a query that used to
 * return valid data starts returning empty results. The purpose is to provide protection against
 * transient failures in the DNS infrastructure.
 */
class RetainingDnsSrvResolver implements DnsSrvResolver {
  private final DnsSrvResolver delegate;
  private final Map<String, List<HostAndPort>> cache;

  RetainingDnsSrvResolver(DnsSrvResolver delegate) {
    this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    cache = new ConcurrentHashMap<String, List<HostAndPort>>();
  }

  @Override
  public List<HostAndPort> resolve(final String fqdn) {
    Preconditions.checkNotNull(fqdn, "fqdn");

    try {
      List<HostAndPort> nodes = delegate.resolve(fqdn);

      if (nodes.isEmpty()) {
        nodes = firstNonNull(cache.get(fqdn), nodes);
      } else {
        cache.put(fqdn, nodes);
      }

      return nodes;
    } catch (Exception e) {
      if (cache.containsKey(fqdn)) {
        return cache.get(fqdn);
      }

      throw Throwables.propagate(e);
    }
  }
}
