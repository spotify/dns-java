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

import static com.spotify.dns.ChangeNotifierFactory.RunnableChangeNotifier;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class ChangeNotifiers {

  /**
   * Ensure that we get a unique empty set that you can't create any other way
   * (i.e. ImmutableSet.of() always returns the same instance).
   *
   * This is needed to distinguishing the initial state of change notifiers from
   * when they have gotten proper data.
   */
  private static final Set INITIAL_EMPTY_DATA = Collections.unmodifiableSet(new HashSet<>());

  private ChangeNotifiers() {
  }

  /**
   * Use this to determine if the data you get back from a notifier is the initial result of the result of a proper
   * DNS lookup. This is useful for distinguishing a proper but empty DNS result from the case
   * where a lookup has not completed yet.
   * @param set
   * @return true if the input is an initially empty set.
   */
  public static <T> boolean isInitialEmptyData(Set<T> set) {
    return set == INITIAL_EMPTY_DATA;
  }

  static <T> Set<T> initialEmptyDataInstance() {
    return INITIAL_EMPTY_DATA;
  }

  static <T> boolean isNoLongerInitial(Set<T> current, Set<T> previous) {
    return isInitialEmptyData(previous) && !isInitialEmptyData(current);
  }

  /**
   * Creates a {@link ChangeNotifier} that aggregates the records provided by a list of notifiers.
   *
   * <p>A change event on any of the input notifiers will propagate up the the returned notifier.
   * The set of previous and current records contained in the event will be the union of all
   * records in the input notifiers, before and after the change event.
   *
   * @param notifiers  A list of notifiers to aggregate
   * @param <T>        The record type
   * @return A notifier with the described behaviour
   */
  public static <T> ChangeNotifier<T> aggregate(ChangeNotifier<T>... notifiers) {
    return aggregate(Arrays.asList(notifiers));
  }

  public static <T> ChangeNotifier<T> aggregate(Iterable<ChangeNotifier<T>> notifiers) {
    return new AggregatingChangeNotifier<>(notifiers);
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
   * @param <T>      The record type
   * @return A notifier with a static set of records
   */
  public static <T> ChangeNotifier<T> staticRecords(T... records) {
    return staticRecords(Sets.newHashSet(records));
  }

  public static <T> ChangeNotifier<T> staticRecords(Set<T> records) {
    return new StaticChangeNotifier<>(records);
  }

  /**
   * Create a {@link RunnableChangeNotifier} that directly wraps a set of records given by a
   * {@link Supplier}.
   *
   * <p>Each call to {@link Runnable#run()} will cause the supplier to be polled and regular
   * change notifications to be triggered.
   *
   * <p>This implementation is useful for testing components that depend on a
   * {@link ChangeNotifier}.
   *
   * @param recordsSupplier  The supplier of records
   * @param <T>              The record type
   * @return A runnable notifier
   */
  public static <T> RunnableChangeNotifier<T> direct(Supplier<Set<T>> recordsSupplier) {
    return new DirectChangeNotifier<>(recordsSupplier);
  }

  /**
   * @deprecated Use {@link #direct(java.util.function.Supplier)}
   * deprecated since version 3.2.0
   */
  public static <T> RunnableChangeNotifier<T> direct(com.google.common.base.Supplier<Set<T>> recordsSupplier) {
    return new DirectChangeNotifier<>(recordsSupplier);
  }

  public static <T> RunnableChangeNotifier<T> direct(AtomicReference<Set<T>> recordsHolder) {
    return new DirectChangeNotifier<>(supplierFromRef(recordsHolder));
  }

  private static <T> Supplier<Set<T>> supplierFromRef(final AtomicReference<Set<T>> ref) {
    requireNonNull(ref, "ref");
    return ref::get;
  }
}
