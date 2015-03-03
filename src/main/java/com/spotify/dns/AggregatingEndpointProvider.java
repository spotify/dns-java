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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An endpoint provider that aggregates the endpoints provided by a list of other providers.
 * The intention is that the endpoint resolution will be done by
 * {@link ServiceResolvingEndpointProvider} and {@link StaticEndpointProvider} instances.
 */
class AggregatingEndpointProvider<T> extends AbstractEndpointProvider<T> {
  private final List<EndpointProvider<T>> endpointProviders;

  /**
   * Create a new aggregating endpoint provider.
   *
   * @param endpointProviders the providers to aggregate
   */
  public AggregatingEndpointProvider(final List<EndpointProvider<T>> endpointProviders) {
    this.endpointProviders = ImmutableList.copyOf(checkNotNull(endpointProviders));

    // Set up forwarding of endpoint updates
    for (final EndpointProvider<T> endpointProvider : this.endpointProviders) {
      endpointProvider.setListener(new Listener<T>() {
         @Override
         public void endpointsChanged(final EndpointProvider<T> endpointProvider) {
           AggregatingEndpointProvider.super.fireEndpointsUpdated();
         }
       }, false);
    }
  }

  @Override
  public Set<T> getEndpoints() {
    ImmutableSet.Builder<T> endpoints = ImmutableSet.builder();
    for (final EndpointProvider<T> endpointProvider : endpointProviders) {
      endpoints.addAll(endpointProvider.getEndpoints());
    }
    return endpoints.build();
  }

  @Override
  protected void closeImplementation() {
    for (EndpointProvider<T> provider : endpointProviders) {
      provider.close();
    }
  }
}

