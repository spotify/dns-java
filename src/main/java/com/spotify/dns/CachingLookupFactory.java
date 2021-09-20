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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.concurrent.ExecutionException;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.lookup.LookupSession;

/**
 * Caches Lookup instances using a per-thread cache; this is so that different threads will never
 * get the same instance of Lookup. Lookup instances are not thread-safe.
 */
@Deprecated
class CachingLookupFactory implements LookupFactory {
  private final LookupFactory delegate;
  private final ThreadLocal<Cache<String, Lookup>> cacheHolder;

  CachingLookupFactory(LookupFactory delegate) {
    this.delegate = requireNonNull(delegate, "delegate");
    cacheHolder =
        ThreadLocal.withInitial(() -> CacheBuilder.newBuilder().build());
  }

  @Override
  public Lookup forName(final String fqdn) {
    try {
      return cacheHolder.get().get(
          fqdn,
          () -> delegate.forName(fqdn)
      );
    } catch (ExecutionException e) {
      throw new DnsException(e);
    } catch (UncheckedExecutionException e) {
      throw new DnsException(e);
    }
  }

  @Override
  public LookupSession sessionForName(String fqdn) {
    throw new java.lang.UnsupportedOperationException("Session not supported with caching lookup");
  }
}
