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

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for the DnsSrvResolversIT class.
 */
public class DnsSrvResolversIT {

  private DnsSrvResolver resolver;

  @Before
  public void setUp() throws Exception {
    resolver = DnsSrvResolvers.newBuilder().build();
  }

  @After
  public void tearDown() throws Exception {
    Metrics.defaultRegistry().removeMetric(DnsSrvResolver.class, "lookups");
  }

  @Test
  public void shouldReturnResultsForValidQuery() throws Exception {
    assertThat(resolver.resolve("_spotify-client._tcp.sto.spotify.net").isEmpty(), is(false));
  }

  @Test
  public void shouldTrackMetricsWhenToldTo() throws Exception {
    resolver = DnsSrvResolvers.newBuilder()
        .metered(true)
        .build();

    MetricsRegistry registry = Metrics.defaultRegistry();
    Timer timer = (Timer) registry.allMetrics().get(new MetricName(DnsSrvResolver.class, "lookups"));

    long countBefore = timer.count();

    resolver.resolve("_spotify-client._tcp.sto.spotify.net");

    assertThat(timer.count(), greaterThan(countBefore));
  }

  @Test
  public void shouldNotTrackMetricsWhenToldNotTo() throws Exception {
    resolver = DnsSrvResolvers.newBuilder()
        .metered(false)
        .build();

    MetricsRegistry registry = Metrics.defaultRegistry();
    assertThat(registry.allMetrics().get(new MetricName(DnsSrvResolver.class, "lookups")), is(nullValue()));
  }

  @Test
  public void shouldFailForBadHostNames() throws Exception {
    try {
      resolver.resolve("nonexistenthost");
    }
    catch (DnsException e) {
      assertThat(e.getMessage(), containsString("host not found"));
    }
  }

  // TODO: it would be nice to be able to also test things like intermittent DNS failures, etc.,
  // but that takes a lot of work setting up a DNS infrastructure that can be made to fail in a
  // controlled way, so I'm skipping that.
}
