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

import java.util.Set;

/**
 * A {@link ChangeNotifier} that provides a static set of records.
 */
class StaticChangeNotifier<T> extends AbstractChangeNotifier<T> {

  private final Set<T> records;

  /**
   * Create a static {@link ChangeNotifier}.
   *
   * @param records The records to provide.
   */
  StaticChangeNotifier(final Set<T> records) {
    this.records = ImmutableSet.copyOf(records);
  }

  @Override
  public Set<T> current() {
    return records;
  }

  @Override
  protected void closeImplementation() {
  }
}

