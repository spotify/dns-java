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

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * An endpoint provider that provides a static set of endpoints.
 */
class StaticEndpointProvider<T> extends AbstractEndpointProvider<T> {

  private final Set<T> endpoints;

  /**
   * Create a static endpoint provider.
   *
   * @param endpoints The endpoints to provide.
   */
  public StaticEndpointProvider(final Set<T> endpoints) {
    this.endpoints = ImmutableSet.copyOf(endpoints);
  }

  @Override
  public Set<T> getEndpoints() {
    return endpoints;
  }

  @Override
  public void closeImplementation() {
    // empty implementation
  }
}

