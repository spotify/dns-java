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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * A {@link ChangeNotifier} that aggregates the records provided by a list of notifiers.
 */
class AggregatingChangeNotifier<T> extends AbstractChangeNotifier<T> {

  private final List<ChangeNotifier<T>> changeNotifiers;

  private volatile Set<T> records;

  /**
   * Create a new aggregating {@link ChangeNotifier}.
   *
   * @param changeNotifiers the notifiers to aggregate
   */
  AggregatingChangeNotifier(final Iterable<ChangeNotifier<T>> changeNotifiers) {
    this.changeNotifiers = ImmutableList.copyOf(changeNotifiers);

    // Set up forwarding of listeners
    for (final ChangeNotifier<T> changeNotifier : this.changeNotifiers) {
      changeNotifier.setListener(ignored -> checkChange(), false);
    }

    records = aggregateSet();
  }

  @Override
  public Set<T> current() {
    return records;
  }

  @Override
  protected void closeImplementation() {
    for (ChangeNotifier<T> provider : changeNotifiers) {
      provider.close();
    }
  }

  private synchronized void checkChange() {
    Set<T> currentRecords = aggregateSet();

    if (ChangeNotifiers.isNoLongerInitial(currentRecords, records) || !currentRecords.equals(records)) {
      final ChangeNotification<T> changeNotification =
          newChangeNotification(currentRecords, records);
      records = currentRecords;

      fireRecordsUpdated(changeNotification);
    }
  }

  private Set<T> aggregateSet() {
    if (areAllInitial(changeNotifiers)) {
      return ChangeNotifiers.initialEmptyDataInstance();
    }

    ImmutableSet.Builder<T> records = ImmutableSet.builder();
    for (final ChangeNotifier<T> changeNotifier : changeNotifiers) {
      records.addAll(changeNotifier.current());
    }
    return records.build();
  }

  private boolean areAllInitial(List<ChangeNotifier<T>> changeNotifiers) {
    for (final ChangeNotifier<T> changeNotifier : changeNotifiers) {
      if (!ChangeNotifiers.isInitialEmptyData(changeNotifier.current())) {
        return false;
      }
    }
    return true;
  }
}

