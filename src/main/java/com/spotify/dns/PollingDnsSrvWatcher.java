/*
 * Copyright (c) 2012-2014 Spotify AB
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

import com.google.common.base.Function;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

class PollingDnsSrvWatcher<T> implements DnsSrvWatcher<T> {

  private final DnsSrvResolver resolver;
  private final ScheduledExecutorService executor;
  private final Function<LookupResult, T> resultTransformer;

  private final long pollingInterval;
  private final TimeUnit pollingIntervalUnit;

  PollingDnsSrvWatcher(DnsSrvResolver resolver,
                       ScheduledExecutorService executor,
                       Function<LookupResult, T> resultTransformer,
                       long pollingInterval,
                       TimeUnit pollingIntervalUnit) {
    this.resolver = checkNotNull(resolver, "resolver");
    this.executor = checkNotNull(executor, "executor");
    this.resultTransformer = checkNotNull(resultTransformer, "resultTransformer");
    this.pollingInterval = pollingInterval;
    this.pollingIntervalUnit = checkNotNull(pollingIntervalUnit, "pollingIntervalUnit");
  }

  @Override
  public ChangeNotifier<T> watch(String fqdn) {
    final ServiceResolvingChangeNotifier<T> changeNotifier =
        new ServiceResolvingChangeNotifier<T>(resolver, fqdn, resultTransformer);
    final Runnable refreshTask = changeNotifier.refreshTask();

    final ScheduledFuture<?> updaterFuture =
        executor.scheduleWithFixedDelay(refreshTask, 0, pollingInterval, pollingIntervalUnit);

    return changeNotifier;
  }

  @Override
  public void close() throws IOException {
    executor.shutdownNow();
  }
}
