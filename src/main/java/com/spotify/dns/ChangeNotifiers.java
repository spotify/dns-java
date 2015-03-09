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

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;

public final class ChangeNotifiers {

  private ChangeNotifiers() {
  }

  /**
   * Creates a {@link ChangeNotifier} that aggregates the records provided by a list of notifiers.
   *
   * <p>A change event on any of the input notifiers will propagate up the the returned notifier.
   * The set of previous and current records contained in the event will be the union of all
   * records in the input notifiers, before and after the change event.
   *
   * @param notifiers  A list of notifiers to aggregate
   * @param <T> The record type
   * @return A notifier with the described behaviour
   */
  public static <T> ChangeNotifier<T> aggregate(ChangeNotifier<T>... notifiers) {
    return aggregate(Arrays.asList(notifiers));
  }

  public static <T> ChangeNotifier<T> aggregate(Iterable<ChangeNotifier<T>> notifiers) {
    return new AggregatingChangeNotifier<T>(notifiers);
  }

  /**
   * Create a {@link ChangeNotifier} with a static set of records.
   *
   * <p>This notifier will never generate any change events. Thus any attached
   * {@link ChangeNotifier.Listener} will at most get one initial call to
   * {@link ChangeNotifier.Listener#onChange(ChangeNotifier.ChangeNotification)}
   * if they are attached with the {@code fire} argument set to {@code true}.
   *
   * @param records  The records that the notifier will contain
   * @param <T> The record type
   * @return A notifier with a static set of records
   */
  public static <T> ChangeNotifier<T> staticRecords(T... records) {
    return staticRecords(Sets.newHashSet(records));
  }

  public static <T> ChangeNotifier<T> staticRecords(Set<T> records) {
    return new StaticChangeNotifier<T>(records);
  }
}
