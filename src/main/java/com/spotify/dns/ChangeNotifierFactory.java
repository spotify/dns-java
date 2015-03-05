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

/**
 * Creates a {@link RunnableChangeNotifier} from a FQDN.
 *
 * Intended to be used from {@link DnsSrvWatcherFactory} when implementing custom triggering
 * schemes for {@link DnsSrvWatcher}s.
 */
public interface ChangeNotifierFactory<T> {

  /**
   * Creates a {@link ChangeNotifier} that is a {@link Runnable}. When a check for a change should
   * be executed is up to the caller of this method.
   *
   * @param fqdn The FQDN for the change notifier
   * @return A runnable change notifier
   */
  RunnableChangeNotifier<T> create(String fqdn);

  /**
   * A {@link ChangeNotifier} that that can be executed. A call to {@link Runnable#run()} will
   * trigger a check for record changes.
   */
  interface RunnableChangeNotifier<T> extends ChangeNotifier<T>, Runnable {
  }
}
