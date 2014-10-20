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

import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MeteredDnsSrvResolverTest {
  private static final String FQDN = "nånting";
  private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();
  private static final Error ERROR = new Error();

  @SuppressWarnings("unchecked")
  private static final List<LookupResult> EMPTY = mock(List.class);
  @SuppressWarnings("unchecked")
  private static final List<LookupResult> NOT_EMPTY = mock(List.class);

  static {
    when(EMPTY.isEmpty()).thenReturn(true);
    when(NOT_EMPTY.isEmpty()).thenReturn(false);
  }

  private DnsSrvResolver delegate;
  private DnsReporter reporter;
  private DnsTimingContext timingReporter;

  private DnsSrvResolver resolver;

  @Before
  public void before() {
    delegate = mock(DnsSrvResolver.class);
    reporter = mock(DnsReporter.class);
    timingReporter = mock(DnsTimingContext.class);

    resolver = new MeteredDnsSrvResolver(delegate, reporter);

    when(reporter.resolveTimer()).thenReturn(timingReporter);
  }

  @After
  public void after() {
    // XXX: would be really strange if this was not called under the current circumstances.
    verify(reporter).resolveTimer();
    verify(timingReporter).stop();
  }

  @Test
  public void shouldCountSuccessful() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(NOT_EMPTY);

    resolver.resolve(FQDN);

    verify(reporter, never()).reportEmpty();
    verify(reporter, never()).reportFailure(RUNTIME_EXCEPTION);
  }

  @Test
  public void shouldReportEmpty() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(EMPTY);

    resolver.resolve(FQDN);

    verify(reporter).reportEmpty();
    verify(reporter, never()).reportFailure(RUNTIME_EXCEPTION);
  }

  @Test
  public void shouldReportRuntimeException() throws Exception {
    when(delegate.resolve(FQDN)).thenThrow(RUNTIME_EXCEPTION);

    try {
      resolver.resolve(FQDN);
      fail("resolve should have thrown exception");
    } catch(RuntimeException e) {
      assertEquals(RUNTIME_EXCEPTION, e);
    }

    verify(reporter, never()).reportEmpty();
    verify(reporter).reportFailure(RUNTIME_EXCEPTION);
  }

  @Test
  public void shouldNotReportError() throws Exception {
    when(delegate.resolve(FQDN)).thenThrow(ERROR);

    try {
      resolver.resolve(FQDN);
      fail("resolve should have thrown exception");
    } catch(Error e) {
      assertEquals(ERROR, e);
    }

    verify(reporter, never()).reportEmpty();
    verify(reporter, never()).reportFailure(RUNTIME_EXCEPTION);
  }
}
