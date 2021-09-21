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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.awaitility.Awaitility;
import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.xbill.DNS.SimpleResolver;

/**
 * Integration tests for the DnsSrvResolversIT class.
 */
public class DnsSrvResolversIT {

  private DnsSrvResolver resolver;

  @Before
  public void setUp() {
    resolver = DnsSrvResolvers.newBuilder().build();
  }

  @Test
  public void shouldReturnResultsForValidQuery() throws ExecutionException, InterruptedException {
    assertThat(resolver.resolve("_spotify-client._tcp.spotify.com").isEmpty(), is(false));
    assertThat(resolver.resolveAsync("_spotify-client._tcp.spotify.com").toCompletableFuture().get().isEmpty(), is(false));
  }

  @Test
  public void testCorrectSequenceOfNotifications() {
    ChangeNotifier<LookupResult> notifier = ChangeNotifiers.aggregate(
        DnsSrvWatchers.newBuilder(resolver)
            .polling(100, TimeUnit.MILLISECONDS)
            .build().watch("_spotify-client._tcp.spotify.com"));

    final List<String> changes = Collections.synchronizedList(new ArrayList<>());

    notifier.setListener(changeNotification -> {
      Set<LookupResult> current = changeNotification.current();
      if (!ChangeNotifiers.isInitialEmptyData(current)) {
        changes.add(current.isEmpty() ? "empty" : "data");
      }
    }, true);
    assertThat(changes, Matchers.empty());
    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> changes.size() >= 1);
    assertThat(changes, containsInAnyOrder("data"));
  }

  @Test
  public void shouldTrackMetricsWhenToldTo() throws ExecutionException, InterruptedException {
    final DnsReporter reporter = mock(DnsReporter.class);
    final DnsTimingContext timingReporter = mock(DnsTimingContext.class);

    resolver = DnsSrvResolvers.newBuilder()
        .metered(reporter)
        .build();

    when(reporter.resolveTimer()).thenReturn(timingReporter);
    resolver.resolveAsync("_spotify-client._tcp.sto.spotify.net").toCompletableFuture().get();
    verify(timingReporter).stop();
    verify(reporter, never()).reportFailure(isA(RuntimeException.class));
    verify(reporter, times(1)).reportEmpty();
  }

  @Test
  public void shouldFailForBadHostNamesAsync() throws Exception {
    try {
      resolver.resolveAsync("nonexistenthost").toCompletableFuture().get();
    }
    catch (DnsException e) {
      assertThat(e.getMessage(), containsString("host not found"));
    }
  }

  @Test
  public void shouldFailForBadHostNames() {
    try {
      resolver.resolve("nonexistenthost");
    }
    catch (DnsException e) {
      assertThat(e.getMessage(), containsString("host not found"));
    }
  }

  @Test
  public void shouldReturnResultsUsingSpecifiedServers() throws Exception {
    final String server = new SimpleResolver().getAddress().getHostName();
    final DnsSrvResolver resolver = DnsSrvResolvers
        .newBuilder()
        .servers(Arrays.asList(server))
        .build();
    assertThat(resolver.resolve("_spotify-client._tcp.spotify.com").isEmpty(), is(false));
    assertThat(resolver.resolveAsync("_spotify-client._tcp.spotify.com").toCompletableFuture().get().isEmpty(), is(false));
  }

  @Test
  public void shouldSucceedCreatingRetainingDnsResolver() {
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
