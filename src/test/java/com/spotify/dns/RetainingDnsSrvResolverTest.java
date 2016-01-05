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

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static com.spotify.dns.DnsTestUtil.nodes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetainingDnsSrvResolverTest {
  private static final String FQDN = "heythere";
  private static final long RETENTION_TIME_MILLIS = 50L;

  RetainingDnsSrvResolver resolver;

  DnsSrvResolver delegate;

  List<LookupResult> nodes1;
  List<LookupResult> nodes2;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    delegate = mock(DnsSrvResolver.class);

    resolver = new RetainingDnsSrvResolver(delegate, RETENTION_TIME_MILLIS);

    nodes1 = nodes("noden1", "noden2");
    nodes2 = nodes("noden3", "noden5", "somethingelse");
  }

  @Test
  public void shouldReturnResultsFromDelegate() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1);

    assertThat(resolver.resolve(FQDN), equalTo(nodes1));
  }

  @Test
  public void shouldReturnResultsFromDelegateEachTime() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodes2);

    resolver.resolve(FQDN);

    assertThat(resolver.resolve(FQDN), equalTo(nodes2));
  }

  @Test
  public void shouldRetainDataIfNewResultEmpty() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodes());

    resolver.resolve(FQDN);

    assertThat(resolver.resolve(FQDN), equalTo(nodes1));
  }

  @Test
  public void shouldRetainDataOnFailure() throws Exception {
    when(delegate.resolve(FQDN))
        .thenReturn(nodes1)
        .thenThrow(new DnsException("expected"));

    resolver.resolve(FQDN);

    assertThat(resolver.resolve(FQDN), equalTo(nodes1));
  }

  @Test
  public void shouldThrowOnFailureAndNoDataAvailable() throws Exception {
    when(delegate.resolve(FQDN)).thenThrow(new DnsException("expected"));

    thrown.expect(DnsException.class);
    thrown.expectMessage("expected");

    resolver.resolve(FQDN);
  }

  @Test
  public void shouldReturnEmptyOnEmptyAndNoDataAvailable() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes());

    assertThat(resolver.resolve(FQDN).isEmpty(), is(true));
  }

  @Test
  public void shouldNotStoreEmptyResults() throws Exception {
    when(delegate.resolve(FQDN))
        .thenReturn(nodes())
        .thenThrow(new DnsException("expected"));

    resolver.resolve(FQDN);

    thrown.expect(DnsException.class);
    thrown.expectMessage("expected");

    resolver.resolve(FQDN);
  }

  @Test
  public void shouldNotRetainPastEndOfRetentionOnEmptyResults() throws Exception {
    when(delegate.resolve(FQDN))
        .thenReturn(nodes("aresult"))
        .thenReturn(nodes());

    resolver.resolve(FQDN);

    // expire retained entry
    Thread.sleep(RETENTION_TIME_MILLIS);

    assertThat(resolver.resolve(FQDN).isEmpty(), is(true));
  }

  @Test
  public void shouldNotRetainPastEndOfRetentionOnException() throws Exception {
    DnsException expected = new DnsException("expected");
    when(delegate.resolve(FQDN))
        .thenReturn(nodes("aresult"))
        .thenThrow(expected);

    resolver.resolve(FQDN);

    // expire retained entry
    Thread.sleep(RETENTION_TIME_MILLIS);

    thrown.expect(equalTo(expected));

    resolver.resolve(FQDN);
  }

  @Test
  public void shouldThrowIfRetentionNegative() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("-4787");

    new RetainingDnsSrvResolver(delegate, -4787);
  }
}
