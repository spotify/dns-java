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
 * A factory for creating {@link DnsSrvWatcher} implementations.
 *
 * A {@link ChangeNotifierFactory} is supplied for creating
 * {@link ChangeNotifierFactory.RunnableChangeNotifier}s. It is up to the implementation of the
 * {@link DnsSrvWatcher} to decide how to schdule runing of the created {@link ChangeNotifier}s.
 *
 * @param <T> The record type
 */
public interface DnsSrvWatcherFactory<T> {

  /**
   * Creates a {@link DnsSrvWatcher} that should create {@link ChangeNotifier} instances using
   * the given factory.
   *
   * @param changeNotifierFactory The factory to use for creating change notifier instances
   * @return A {@link DnsSrvWatcher}
   */
  DnsSrvWatcher<T> create(ChangeNotifierFactory<T> changeNotifierFactory);
}
