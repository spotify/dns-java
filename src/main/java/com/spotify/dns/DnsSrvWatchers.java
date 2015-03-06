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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Provides builders for configuring and instantiating {@link DnsSrvWatcher}s.
 */
public final class DnsSrvWatchers {

  /**
   * Creates a {@link DnsSrvWatcherBuilder} using the given {@link DnsSrvResolver}. The builder
   * can be configured to have the desired behavior.
   *
   * <p>Exactly one of {@link DnsSrvWatcherBuilder#polling(long, TimeUnit)} or
   * {@link DnsSrvWatcherBuilder#customTrigger(DnsSrvWatcherFactory)} must be used.
   *
   * @param resolver The resolver to use for lookups
   * @return a builder for further configuring the watcher
   */
  public static DnsSrvWatcherBuilder<LookupResult> newBuilder(DnsSrvResolver resolver) {
    checkNotNull(resolver, "resolver");

    return new DnsSrvWatcherBuilder<LookupResult>(resolver, Functions.<LookupResult>identity());
  }

  /**
   * Creates a {@link DnsSrvWatcherBuilder} using the given {@link DnsSrvResolver}. The builder
   * can be configured to have the desired behavior.
   *
   * <p>This watcher will use a function that transforms the {@link LookupResult}s into an
   * arbitrary type that will be used throughout the {@link DnsSrvWatcher} api.
   *
   * <p>Exactly one of {@link DnsSrvWatcherBuilder#polling(long, TimeUnit)} or
   * {@link DnsSrvWatcherBuilder#customTrigger(DnsSrvWatcherFactory)} must be used.
   *
   * @param resolver          The resolver to use for lookups
   * @param resultTransformer The transformer function
   * @return a builder for further configuring the watcher
   */
  public static <T> DnsSrvWatcherBuilder<T> newBuilder(
      DnsSrvResolver resolver,
      Function<LookupResult, T> resultTransformer) {

    checkNotNull(resolver, "resolver");
    checkNotNull(resultTransformer, "resultTransformer");

    return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer);
  }

  public static final class DnsSrvWatcherBuilder<T> {

    private final DnsSrvResolver resolver;
    private final Function<LookupResult, T> resultTransformer;

    private final boolean polling;
    private final long pollingInterval;
    private final TimeUnit pollingIntervalUnit;

    private final DnsSrvWatcherFactory<T> dnsSrvWatcherFactory;

    private final ScheduledExecutorService scheduledExecutorService;

    private DnsSrvWatcherBuilder(
        DnsSrvResolver resolver,
        Function<LookupResult, T> resultTransformer) {
      this(resolver, resultTransformer, false, 0, null, null, null);
    }

    private DnsSrvWatcherBuilder(
        DnsSrvResolver resolver,
        Function<LookupResult, T> resultTransformer,
        boolean polling,
        long pollingInterval,
        TimeUnit pollingIntervalUnit,
        DnsSrvWatcherFactory<T> dnsSrvWatcherFactory,
        ScheduledExecutorService scheduledExecutorService) {
      this.resolver = resolver;
      this.resultTransformer = resultTransformer;
      this.polling = polling;
      this.pollingInterval = pollingInterval;
      this.pollingIntervalUnit = pollingIntervalUnit;
      this.dnsSrvWatcherFactory = dnsSrvWatcherFactory;
      this.scheduledExecutorService = scheduledExecutorService;
    }

    public DnsSrvWatcher<T> build() {
      checkState(polling ^ dnsSrvWatcherFactory != null, "specify either polling or custom trigger");

      final ChangeNotifierFactory<T> changeNotifierFactory =
          new ChangeNotifierFactory<T>() {
            @Override
            public RunnableChangeNotifier<T> create(String fqdn) {
              return new ServiceResolvingChangeNotifier<T>(resolver, fqdn, resultTransformer);
            }
          };

      DnsSrvWatcherFactory<T> watcherFactory;
      if (polling) {
        final ScheduledExecutorService executor =
            scheduledExecutorService != null
            ? scheduledExecutorService
            : MoreExecutors.getExitingScheduledExecutorService(
                new ScheduledThreadPoolExecutor(
                    1, new ThreadFactoryBuilder().setNameFormat("dns-lookup-%d").build()),
                0, SECONDS);

        watcherFactory = new DnsSrvWatcherFactory<T>() {
          @Override
          public DnsSrvWatcher<T> create(ChangeNotifierFactory<T> changeNotifierFactory) {
            return new PollingDnsSrvWatcher<T>(changeNotifierFactory, executor, pollingInterval,
                                               pollingIntervalUnit);
          }
        };
      } else {
        watcherFactory = checkNotNull(dnsSrvWatcherFactory);
      }

      return watcherFactory.create(changeNotifierFactory);
    }

    public DnsSrvWatcherBuilder<T> polling(long pollingInterval, TimeUnit pollingIntervalUnit) {
      checkArgument(pollingInterval > 0);
      checkNotNull(pollingIntervalUnit, "pollingIntervalUnit");

      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, true, pollingInterval,
                                         pollingIntervalUnit, dnsSrvWatcherFactory,
                                         scheduledExecutorService);
    }

    public DnsSrvWatcherBuilder<T> usingExecutor(ScheduledExecutorService scheduledExecutorService) {
      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, polling, pollingInterval,
                                         pollingIntervalUnit, dnsSrvWatcherFactory,
                                         scheduledExecutorService);
    }

    public DnsSrvWatcherBuilder<T> customTrigger(DnsSrvWatcherFactory<T> watcherFactory) {
      checkNotNull(watcherFactory, "watcherFactory");

      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, true, pollingInterval,
                                         pollingIntervalUnit, watcherFactory,
                                         scheduledExecutorService);
    }
  }

  private DnsSrvWatchers() {
    // prevent instantiation
  }
}
