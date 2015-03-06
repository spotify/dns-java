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

import java.io.Closeable;

/**
 * A watcher for DNS SRV records.
 *
 * <p>The records can be of any type. Usually something that directly reflects what your
 * application will use the records for.
 *
 * @param <T> The record type
 */
public interface DnsSrvWatcher<T> extends Closeable {

  /**
   * Starts watching a FQDN, by creating a {@link ChangeNotifier} for it.
   *
   * @param fqdn  The FQDN to watch
   * @return  A change notifier that will reflect changes to the watched fqdn
   */
  ChangeNotifier<T> watch(String fqdn);
}
