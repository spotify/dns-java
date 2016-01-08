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

import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the DnsSrvResolversIT class.
 */
public class DnsSrvResolversIT {

  private DnsSrvResolver resolver;

  @Before
  public void setUp() throws Exception {
    resolver = DnsSrvResolvers.newBuilder().build();
  }

  @Test
  public void shouldReturnResultsForValidQuery() throws Exception {
    assertThat(resolver.resolve("_spotify-client._tcp.sto.spotify.net").isEmpty(), is(false));
  }

  @Test
  public void shouldTrackMetricsWhenToldTo() throws Exception {
    final DnsReporter reporter = mock(DnsReporter.class);
    final DnsTimingContext timingReporter = mock(DnsTimingContext.class);

    resolver = DnsSrvResolvers.newBuilder()
        .metered(reporter)
        .build();

    when(reporter.resolveTimer()).thenReturn(timingReporter);
    resolver.resolve("_spotify-client._tcp.sto.spotify.net");
    verify(timingReporter).stop();
    verify(reporter, never()).reportFailure(isA(RuntimeException.class));
    verify(reporter, never()).reportEmpty();
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

  @Test
  public void shouldSucceedCreatingRetainingDnsResolver() throws Exception {
    try {
      resolver = DnsSrvResolvers.newBuilder().retainingDataOnFailures(true).build();
    }
    catch (DnsException e) {
      assertTrue("DNS exception should not be thrown", false);
    }
    catch (IllegalArgumentException e) {
      assertTrue("Illegal argument exception should not be thrown", false);
    }
  }

  // TODO: it would be nice to be able to also test things like intermittent DNS failures, etc.,
  // but that takes a lot of work setting up a DNS infrastructure that can be made to fail in a
  // controlled way, so I'm skipping that.
}
