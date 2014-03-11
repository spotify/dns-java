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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.xbill.DNS.Lookup;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Caches Lookup instances using a per-thread cache; this is so that different threads will never
 * get the same instance of Lookup. Lookup instances are not thread-safe.
 */
class CachingLookupFactory implements LookupFactory {
  private final LookupFactory delegate;
  private final ThreadLocal<Cache<String, Lookup>> cacheHolder;

  CachingLookupFactory(LookupFactory delegate) {
    this.delegate = Preconditions.checkNotNull(delegate, "delegate");
    cacheHolder =
        new ThreadLocal<Cache<String, Lookup>>() {
          @Override
          protected Cache<String, Lookup> initialValue() {
            return CacheBuilder.newBuilder().build();
          }
        };
  }

  @Override
  public Lookup forName(final String fqdn) {
    try {
      return cacheHolder.get().get(
          fqdn,
          new Callable<Lookup>() {
            @Override
            public Lookup call() {
              return delegate.forName(fqdn);
            }
          }
      );
    } catch (ExecutionException e) {
      throw new DnsException(e);
    }
  }
}
