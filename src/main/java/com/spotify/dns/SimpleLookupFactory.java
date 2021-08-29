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

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupSession;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/** A LookupFactory that always returns new instances. */
public class SimpleLookupFactory implements LookupFactory {
  private final LookupSession session;
  private final Resolver resolver;

  /**
   * @deprecated
   * Deprecated to avoid overloading forkjoin common pool.
   * Use {@link SimpleLookupFactory#SimpleLookupFactory(Executor)} instead.
   */
  @Deprecated
  public SimpleLookupFactory() {
    this(Lookup.getDefaultResolver());
  }

  /**
   * @deprecated
   * Deprecated to avoid overloading forkjoin common pool.
   * Use {@link SimpleLookupFactory#SimpleLookupFactory(Resolver, Executor)} instead.
   */
  public SimpleLookupFactory(Resolver resolver) {
    this(resolver, ForkJoinPool.commonPool());
  }

  public SimpleLookupFactory(Executor executor) {
    this(Lookup.getDefaultResolver(), executor);
  }

  public SimpleLookupFactory(Resolver resolver, Executor executor) {
    requireNonNull(executor);
    this.resolver = resolver;
    this.session = LookupSession.builder().resolver(resolver).executor(executor).build();
  }

  @Override
  public Lookup forName(String fqdn) {
    try {
      final Lookup lookup = new Lookup(fqdn, Type.SRV, DClass.IN);
      if (resolver != null) {
        lookup.setResolver(resolver);
      }
      return lookup;
    } catch (TextParseException e) {
      throw new DnsException("unable to create lookup for name: " + fqdn, e);
    }
  }

  @Override
  public LookupSession sessionForName(String fqdn) {
    return session;
  }
}
