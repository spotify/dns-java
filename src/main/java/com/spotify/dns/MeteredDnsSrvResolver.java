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

import com.google.common.net.HostAndPort;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tracks metrics for DnsSrvResolver calls.
 */
class MeteredDnsSrvResolver implements DnsSrvResolver {
  private final DnsSrvResolver delegate;
  private final Timer timer;
  private final Counter failureCounter;
  private final Counter emptyCounter;

  MeteredDnsSrvResolver(DnsSrvResolver delegate, Timer timer, Counter failureCounter, Counter emptyCounter) {
    this.delegate = checkNotNull(delegate, "delegate");
    this.timer = checkNotNull(timer, "timer");
    this.failureCounter = checkNotNull(failureCounter, "failureCounter");
    this.emptyCounter = checkNotNull(emptyCounter, "emptyCounter");
  }

  @Override
  public List<HostAndPort> resolve(String fqdn) {
    // using a boolean to track whether or not an exception was thrown - that means I don't
    // need to worry about how to rethrow the exception; instead, I can take whatever action I
    // want in the finally clause.
    boolean success = false;
    final TimerContext context = timer.time();

    try {
      List<HostAndPort> result = delegate.resolve(fqdn);
      if (result.isEmpty()) {
        emptyCounter.inc();
      }
      success = true;
      return result;
    }
    finally {
      context.stop();
      if (!success) {
        failureCounter.inc();
      }
    }
  }
}
