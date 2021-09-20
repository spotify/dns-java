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

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ChangeNotifier} that resolves and provides records using a {@link DnsSrvResolver}.
 *
 * <p>The records are refreshable when {@link #run()} is called.
 */
class ServiceResolvingChangeNotifier<T> extends AbstractChangeNotifier<T>
    implements ChangeNotifierFactory.RunnableChangeNotifier<T> {

  private static final Logger log = LoggerFactory.getLogger(ServiceResolvingChangeNotifier.class);

  private final DnsSrvResolver resolver;
  private final String fqdn;
  private final Function<LookupResult, T> resultTransformer;

  private final ErrorHandler errorHandler;

  private volatile Set<T> records = ChangeNotifiers.initialEmptyDataInstance();
  private volatile boolean waitingForFirstEvent = true;

  private volatile boolean run = true;

  /**
   * Create a {@link ChangeNotifier} that tracks changes from a {@link DnsSrvResolver}.
   *
   * <p>The list of {@link LookupResult}s will be transformed using the provided function
   * and put into a set. The set will then be compared to the previous set and if a
   * change is detected, the notifier will fire.
   *
   * <p>An optional {@link ErrorHandler} can be used to react on {@link DnsException}s thrown
   * by the {@link DnsSrvResolver}.
   *
   * @param resolver            The resolver to use.
   * @param fqdn                The name to lookup SRV records for
   * @param resultTransformer   The transform function
   * @param errorHandler        The error handler that will receive exceptions (nullable)
   */
  ServiceResolvingChangeNotifier(final DnsSrvResolver resolver,
                                 final String fqdn,
                                 final Function<LookupResult, T> resultTransformer,
                                 final ErrorHandler errorHandler) {

    this.resolver = requireNonNull(resolver, "resolver");
    this.fqdn = requireNonNull(fqdn, "fqdn");
    this.resultTransformer = requireNonNull(resultTransformer, "resultTransformer");
    this.errorHandler = errorHandler;
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

    resolver.resolveAsync(fqdn).whenComplete((nodes, e) -> {
      if (e instanceof DnsException) {
        if (errorHandler != null) {
          errorHandler.handle(fqdn, (DnsException) e);
        }
        log.error(e.getMessage(), e);
        fireIfFirstError();
      } else if (e != null) {
        log.error(e.getMessage(), e);
        fireIfFirstError();
      } else {
        final Set<T> current;
        try {
          ImmutableSet.Builder<T> builder = ImmutableSet.builder();
          for (LookupResult node : nodes) {
            T transformed = resultTransformer.apply(node);
            builder.add(requireNonNull(transformed, "transformed"));
          }
          current = builder.build();
        } catch (Exception transformerException) {
          log.error(transformerException.getMessage(), transformerException);
          fireIfFirstError();
          return;
        }

        if (ChangeNotifiers.isNoLongerInitial(current, records) || !current.equals(records)) {
          // This means that any subsequent DNS error will be ignored and the existing result will be kept
          waitingForFirstEvent = false;
          final ChangeNotification<T> changeNotification =
                  newChangeNotification(current, records);
          records = current;

          fireRecordsUpdated(changeNotification);
        }
      }
    });
  }

  private void fireIfFirstError() {
    if (waitingForFirstEvent) {
      waitingForFirstEvent = false;
      Set<T> previous = current();
      records = ImmutableSet.of();
      fireRecordsUpdated(newChangeNotification(records, previous));
    }
  }
}

