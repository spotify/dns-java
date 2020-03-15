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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
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
    requireNonNull(resolver, "resolver");

    return new DnsSrvWatcherBuilder<>(resolver, Function.identity());
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

    requireNonNull(resolver, "resolver");
    requireNonNull(resultTransformer, "resultTransformer");

    return new DnsSrvWatcherBuilder<>(resolver, resultTransformer);
  }

  /**
   * @deprecated Use {@link #newBuilder(DnsSrvResolver, java.util.function.Function)}
   */
  @Deprecated(since = "3.1.6")
  public static <T> DnsSrvWatcherBuilder<T> newBuilder(
      DnsSrvResolver resolver,
      com.google.common.base.Function<LookupResult, T> resultTransformer) {
    return newBuilder(resolver, resultTransformer);
  }

  public static final class DnsSrvWatcherBuilder<T> {

    private final DnsSrvResolver resolver;
    private final Function<LookupResult, T> resultTransformer;

    private final boolean polling;
    private final long pollingInterval;
    private final TimeUnit pollingIntervalUnit;

    private final ErrorHandler errorHandler;

    private final DnsSrvWatcherFactory<T> dnsSrvWatcherFactory;

    private final ScheduledExecutorService scheduledExecutorService;

    private DnsSrvWatcherBuilder(
        DnsSrvResolver resolver,
        Function<LookupResult, T> resultTransformer) {
      this(resolver, resultTransformer, false, 0, null, null, null, null);
    }

    private DnsSrvWatcherBuilder(
        DnsSrvResolver resolver,
        Function<LookupResult, T> resultTransformer,
        boolean polling,
        long pollingInterval,
        TimeUnit pollingIntervalUnit,
        ErrorHandler errorHandler,
        DnsSrvWatcherFactory<T> dnsSrvWatcherFactory,
        ScheduledExecutorService scheduledExecutorService) {
      this.resolver = resolver;
      this.resultTransformer = resultTransformer;
      this.polling = polling;
      this.pollingInterval = pollingInterval;
      this.pollingIntervalUnit = pollingIntervalUnit;
      this.errorHandler = errorHandler;
      this.dnsSrvWatcherFactory = dnsSrvWatcherFactory;
      this.scheduledExecutorService = scheduledExecutorService;
    }

    public DnsSrvWatcher<T> build() {
      checkState(polling ^ dnsSrvWatcherFactory != null, "specify either polling or custom trigger");

      DnsSrvWatcherFactory<T> watcherFactory;
      if (polling) {
        final ScheduledExecutorService executor =
            scheduledExecutorService != null
            ? scheduledExecutorService
            : MoreExecutors.getExitingScheduledExecutorService(
                new ScheduledThreadPoolExecutor(
                    1, new ThreadFactoryBuilder().setNameFormat("dns-lookup-%d").build()),
                0, SECONDS);

        watcherFactory =
            cnf -> new PollingDnsSrvWatcher<>(cnf, executor, pollingInterval, pollingIntervalUnit);
      } else {
        watcherFactory = requireNonNull(dnsSrvWatcherFactory, "dnsSrvWatcherFactory");
      }

      final ChangeNotifierFactory<T> changeNotifierFactory =
          fqdn -> new ServiceResolvingChangeNotifier<>(
              resolver, fqdn, resultTransformer, errorHandler);

      return watcherFactory.create(changeNotifierFactory);
    }

    public DnsSrvWatcherBuilder<T> polling(long pollingInterval, TimeUnit pollingIntervalUnit) {
      checkArgument(pollingInterval > 0);
      requireNonNull(pollingIntervalUnit, "pollingIntervalUnit");

      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, true, pollingInterval,
                                         pollingIntervalUnit, errorHandler, dnsSrvWatcherFactory,
                                         scheduledExecutorService);
    }

    public DnsSrvWatcherBuilder<T> usingExecutor(ScheduledExecutorService scheduledExecutorService) {
      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, polling, pollingInterval,
                                         pollingIntervalUnit, errorHandler, dnsSrvWatcherFactory,
                                         scheduledExecutorService);
    }

    public DnsSrvWatcherBuilder<T> customTrigger(DnsSrvWatcherFactory<T> watcherFactory) {
      requireNonNull(watcherFactory, "watcherFactory");

      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, true, pollingInterval,
                                         pollingIntervalUnit, errorHandler, watcherFactory,
                                         scheduledExecutorService);
    }

    public DnsSrvWatcherBuilder<T> withErrorHandler(ErrorHandler errorHandler) {
      requireNonNull(errorHandler, "errorHandler");

      return new DnsSrvWatcherBuilder<T>(resolver, resultTransformer, true, pollingInterval,
                                         pollingIntervalUnit, errorHandler, dnsSrvWatcherFactory,
                                         scheduledExecutorService);
    }
  }

  private DnsSrvWatchers() {
    // prevent instantiation
  }
}
