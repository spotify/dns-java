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

import com.google.common.base.Supplier;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

class DirectChangeNotifier<T> extends AbstractChangeNotifier<T>
    implements ChangeNotifierFactory.RunnableChangeNotifier<T> {

  private final Supplier<Set<T>> recordsSupplier;

  private volatile Set<T> records = ChangeNotifiers.initialEmptyDataInstance();
  private volatile boolean run = true;

  public DirectChangeNotifier(Supplier<Set<T>> recordsSupplier) {
    this.recordsSupplier = checkNotNull(recordsSupplier, "recordsSupplier");
  }

  @Override
  protected void closeImplementation() {
    run = false;
  }

  @Override
  public Set<T> current() {
    return records;
  }

  @Override
  public void run() {
    if (!run) {
      return;
    }

    final Set<T> current = recordsSupplier.get();
    if (ChangeNotifiers.isNoLongerInitial(current, records) || !current.equals(records)) {
      final ChangeNotification<T> changeNotification =
          newChangeNotification(current, records);
      records = current;

      fireRecordsUpdated(changeNotification);
    }
  }

}
