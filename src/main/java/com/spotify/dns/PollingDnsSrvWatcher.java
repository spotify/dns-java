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

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.spotify.dns.ChangeNotifierFactory.RunnableChangeNotifier;

class PollingDnsSrvWatcher<T> implements DnsSrvWatcher<T> {

  private final ChangeNotifierFactory<T> changeNotifierFactory;

  private final ScheduledExecutorService executor;

  private final long pollingInterval;
  private final TimeUnit pollingIntervalUnit;

  PollingDnsSrvWatcher(ChangeNotifierFactory<T> changeNotifierFactory,
                       ScheduledExecutorService executor,
                       long pollingInterval,
                       TimeUnit pollingIntervalUnit) {
    this.changeNotifierFactory = checkNotNull(changeNotifierFactory, "changeNotifierFactory");
    this.executor = checkNotNull(executor, "executor");
    this.pollingInterval = pollingInterval;
    this.pollingIntervalUnit = checkNotNull(pollingIntervalUnit, "pollingIntervalUnit");
  }

  @Override
  public ChangeNotifier<T> watch(String fqdn) {
    final RunnableChangeNotifier<T> changeNotifier = changeNotifierFactory.create(fqdn);

    final ScheduledFuture<?> updaterFuture =
        executor.scheduleWithFixedDelay(changeNotifier, 0, pollingInterval, pollingIntervalUnit);

    return changeNotifier;
  }

  @Override
  public void close() throws IOException {
    executor.shutdownNow();
  }
}
