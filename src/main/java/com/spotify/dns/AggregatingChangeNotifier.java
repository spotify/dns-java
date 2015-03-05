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
 * {@link ServiceResolvingChangeNotifier} and {@link StaticChangeNotifier} instances.
 */
class AggregatingChangeNotifier<T> extends AbstractChangeNotifier<T> {
  private final List<ChangeNotifier<T>> changeNotifiers;

  /**
   * Create a new aggregating endpoint provider.
   *
   * @param changeNotifiers the providers to aggregate
   */
  public AggregatingChangeNotifier(final List<ChangeNotifier<T>> changeNotifiers) {
    this.changeNotifiers = ImmutableList.copyOf(checkNotNull(changeNotifiers));

    // Set up forwarding of endpoint updates
    for (final ChangeNotifier<T> changeNotifier : this.changeNotifiers) {
      changeNotifier.setListener(new Listener<T>() {
         @Override
         public void onChange(final ChangeNotification<T> changeNotification) {
           AggregatingChangeNotifier.super.fireRecordsUpdated(changeNotification);
         }
       }, false);
    }
  }

  @Override
  public Set<T> current() {
    ImmutableSet.Builder<T> endpoints = ImmutableSet.builder();
    for (final ChangeNotifier<T> changeNotifier : changeNotifiers) {
      endpoints.addAll(changeNotifier.current());
    }
    return endpoints.build();
  }

  @Override
  protected void closeImplementation() {
    for (ChangeNotifier<T> provider : changeNotifiers) {
      provider.close();
    }
  }
}

