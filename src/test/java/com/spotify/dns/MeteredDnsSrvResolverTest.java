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
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.spotify.dns.DnsTestUtil.nodes;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MeteredDnsSrvResolverTest {
  public static final String FQDN = "nånting";
  MeteredDnsSrvResolver resolver;

  DnsSrvResolver delegate;
  Timer timer;
  Counter failureCounter;
  Counter emptyCounter;

  TimerContext context;
  List<HostAndPort> nodes;

  @Before
  public void setUp() throws Exception {
    delegate = mock(DnsSrvResolver.class);
    timer = mock(Timer.class);
    failureCounter = mock(Counter.class);
    emptyCounter = mock(Counter.class);

    resolver = new MeteredDnsSrvResolver(delegate, timer, failureCounter, emptyCounter);

    context = mock(TimerContext.class);
    when(timer.time()).thenReturn(context).thenThrow(new RuntimeException("didn't expect two calls"));

    nodes = nodes("node1", "node2", "nodtre");
  }

  @Test
  public void shouldReturnResultsFromDelegate() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes);
    assertThat(resolver.resolve(FQDN), equalTo(nodes));
  }

  @Test
  public void shouldTimeCalls() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes);

    resolver.resolve(FQDN);

    verify(context).stop();
  }

  @Test
  public void shouldTimeFailedCalls() throws Exception {
    DnsException expected = new DnsException("expected");
    when(delegate.resolve(FQDN)).thenThrow(expected);

    try {
      resolver.resolve(FQDN);
    } catch (DnsException e) {
      assertThat(e, equalTo(expected));
    }

    verify(context).stop();
  }

  @Test
  public void shouldCountErrors() throws Exception {
    DnsException expected = new DnsException("expected");
    when(delegate.resolve(FQDN)).thenThrow(expected);

    try {
      resolver.resolve(FQDN);
    } catch (DnsException e) {
      assertThat(e, equalTo(expected));
    }

    verify(failureCounter).inc();
  }

  @Test
  public void shouldNotCountSuccesses() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes);

    resolver.resolve(FQDN);

    verifyZeroInteractions(emptyCounter, failureCounter);
  }

  @Test
  public void shouldCountEmptyResults() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes());

    resolver.resolve(FQDN);

    verify(emptyCounter).inc();
  }
}
