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

import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A helper for implementing the {@link com.spotify.dns.EndpointProvider} interface.
 */
abstract class AbstractEndpointProvider<T> implements EndpointProvider<T> {

  private final AtomicReference<Listener<T>> listenerRef = new AtomicReference<Listener<T>>();

  @Override
  public void setListener(final Listener<T> listener, final boolean fire) {
    checkNotNull(listener, "listener");

    if (!listenerRef.compareAndSet(null, listener)) {
      throw new IllegalStateException("Listener already set!");
    }

    if (fire) {
      fireEndpointsUpdated();
    }
  }

  @Override
  public final void close() {
    listenerRef.set(null);

    closeImplementation();
  }

  protected abstract void closeImplementation();

  protected void fireEndpointsUpdated() {
    listenerRef.get().endpointsChanged(this);
  }
}
