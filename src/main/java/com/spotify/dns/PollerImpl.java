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

import com.google.common.base.Function;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

class PollerImpl<T> implements PollingDnsSrvResolver<T> {

  private final DnsSrvResolver resolver;
  final Function<LookupResult, T> resultTransformer;

  PollerImpl(DnsSrvResolver resolver, Function<LookupResult, T> resultTransformer) {
    this.resolver = checkNotNull(resolver, "resolver");
    this.resultTransformer = checkNotNull(resultTransformer, "resultTransformer");
  }

  @Override
  public EndpointProvider<T> poll(String fqdn, long refreshInterval, TimeUnit refreshIntervalUnit) {
    return new ServiceResolvingEndpointProvider<T>(
        resolver, refreshInterval, refreshIntervalUnit, fqdn, resultTransformer);
  }
}
