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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper for implementing the {@link ChangeNotifier} interface.
 */
abstract class AbstractChangeNotifier<T> implements ChangeNotifier<T> {

  private final AtomicReference<Listener<T>> listenerRef = new AtomicReference<Listener<T>>();

  @Override
  public void setListener(final Listener<T> listener, final boolean fire) {
    checkNotNull(listener, "listener");

    synchronized (this) {
      if (!listenerRef.compareAndSet(null, listener)) {
        throw new IllegalStateException("Listener already set!");
      }

      if (fire) {
        fireRecordsUpdated(newChangeNotification(current(), Collections.<T>emptySet()));
      }
    }
  }

  @Override
  public final void close() {
    listenerRef.set(null);
    closeImplementation();
  }

  protected abstract void closeImplementation();

  protected final void fireRecordsUpdated(ChangeNotification<T> changeNotification) {
    checkNotNull(changeNotification, "changeNotification");

    final Listener<T> listener = listenerRef.get();
    if (listener != null) {
      listener.onChange(changeNotification);
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
      return Collections.unmodifiableSet(current);
    }

    @Override
    public Set<T> previous() {
      return Collections.unmodifiableSet(previous);
    }
  }
}
