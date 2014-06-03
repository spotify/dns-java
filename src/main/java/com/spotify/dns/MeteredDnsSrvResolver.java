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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.net.HostAndPort;
import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;

/**
 * Tracks metrics for DnsSrvResolver calls.
 */
class MeteredDnsSrvResolver implements DnsSrvResolver {
  private final DnsSrvResolver delegate;
  private final DnsReporter reporter;

  MeteredDnsSrvResolver(DnsSrvResolver delegate, DnsReporter reporter) {
    this.delegate = checkNotNull(delegate, "delegate");
    this.reporter = checkNotNull(reporter, "reporter");
  }

  @Override
  public List<HostAndPort> resolve(String fqdn) {
    // Only catch and report RuntimeException to avoid Error's since that would
    // most likely only aggravate any condition that causes them to be thrown.

    final DnsTimingContext resolveTimer = reporter.resolveTimer();

    final List<HostAndPort> result;

    try {
      result = delegate.resolve(fqdn);
    } catch (RuntimeException error) {
      reporter.reportFailure(error);
      throw error;
    } finally {
      resolveTimer.stop();
    }

    if (result.isEmpty()) {
      reporter.reportEmpty();
    }

    return result;
  }
}
