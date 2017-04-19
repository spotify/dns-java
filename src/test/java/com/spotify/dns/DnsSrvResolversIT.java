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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.jayway.awaitility.Awaitility;
import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    assertThat(resolver.resolve("_spotify-client._tcp.spotify.com").isEmpty(), is(false));
  }

  @Test
  public void testCorrectSequenceOfNotifications() throws Exception {
    ChangeNotifier<LookupResult> notifier = ChangeNotifiers.aggregate(
        DnsSrvWatchers.newBuilder(resolver)
            .polling(100, TimeUnit.MILLISECONDS)
            .build().watch("_spotify-client._tcp.spotify.com"));

    final List<String> changes = Collections.synchronizedList(Lists.<String>newArrayList());

    notifier.setListener(new ChangeNotifier.Listener<LookupResult>() {
      @Override
      public void onChange(ChangeNotifier.ChangeNotification<LookupResult> changeNotification) {
        Set<LookupResult> current = changeNotification.current();
        if (!ChangeNotifiers.isInitialEmptyData(current)) {
          changes.add(current.isEmpty() ? "empty" : "data");
        }
      }
    }, true);
    assertEquals(ImmutableList.of(), changes);
    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return changes.size() >= 1;
      }
    });
    assertEquals(ImmutableList.of("data"), changes);
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
    verify(reporter, times(1)).reportEmpty();
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

    @Test
    public void shouldSucceedCreatingNotUsingLookupCache() throws Exception {
        DnsSrvResolvers.newBuilder().useLookupCache(false).build();
    }

  // TODO: it would be nice to be able to also test things like intermittent DNS failures, etc.,
  // but that takes a lot of work setting up a DNS infrastructure that can be made to fail in a
  // controlled way, so I'm skipping that.
}
