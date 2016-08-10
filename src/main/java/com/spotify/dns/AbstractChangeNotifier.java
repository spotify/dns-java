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

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper for implementing the {@link ChangeNotifier} interface.
 */
abstract class AbstractChangeNotifier<T> implements ChangeNotifier<T> {

  private static final Logger log = LoggerFactory.getLogger(AbstractChangeNotifier.class);

  private final AtomicReference<Listener<T>> listenerRef = new AtomicReference<Listener<T>>();

  private final AtomicBoolean listenerNotified = new AtomicBoolean(false);

  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public void setListener(final Listener<T> listener, final boolean fire) {
    checkNotNull(listener, "listener");

    lock.lock();
    try {
          if (!listenerRef.compareAndSet(null, listener)) {
        throw new IllegalStateException("Listener already set!");
      }

      if (fire) {
        notifyListener(newChangeNotification(current(), Collections.<T>emptySet()), true);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public final void close() {
    listenerRef.set(null);
    closeImplementation();
  }

  protected abstract void closeImplementation();

  protected final void fireRecordsUpdated(ChangeNotification<T> changeNotification) {
    notifyListener(changeNotification, false);
  }

  /**
   * Notify the listener about a change. If this is due to adding a new listener rather than
   * being an update, only notify the listener if this is the first notification sent to it.
   *
   * @param changeNotification the change notification to send
   * @param newListener call is triggered by adding a listener rather than an update
   */
  private void notifyListener(ChangeNotification<T> changeNotification, boolean newListener) {
    lock.lock();
    try {
      checkNotNull(changeNotification, "changeNotification");

      final Listener<T> listener = listenerRef.get();
      if (listener != null) {
        try {
          final boolean notified = listenerNotified.getAndSet(true);
          if (!(newListener && notified)) {
            listener.onChange(changeNotification);
          }
        } catch (Throwable e) {
          log.error("Change notification listener threw exception", e);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  protected final ChangeNotification<T> newChangeNotification(Set<T> current, Set<T> previous) {
    checkNotNull(current, "current");
    checkNotNull(previous, "previous");

    return new ChangeNotificationImpl<T>(current, previous);
  }

  private static class ChangeNotificationImpl<T> implements ChangeNotification<T> {

    private final Set<T> current;
    private final Set<T> previous;

    protected ChangeNotificationImpl(Set<T> current, Set<T> previous) {
      this.current = current;
      this.previous = previous;
    }

    @Override
    public Set<T> current() {
      return unmodifiable(current);
    }

    private Set<T> unmodifiable(Set<T> set) {
      if (ChangeNotifiers.isInitialEmptyData(set)) {
        return set;
      }
      if (set instanceof ImmutableSet) {
        return set;
      }
      return Collections.unmodifiableSet(set);
    }

    @Override
    public Set<T> previous() {
      return unmodifiable(previous);
    }
  }
}
