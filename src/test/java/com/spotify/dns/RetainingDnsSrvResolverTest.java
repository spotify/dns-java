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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Date;
import java.util.List;

import static com.spotify.dns.DnsTestUtil.nodes;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetainingDnsSrvResolverTest {
  private static final String FQDN = "heythere";

  RetainingDnsSrvResolver resolver;

  DnsSrvResolver delegate;

  List<LookupResult> nodes1;
  List<LookupResult> nodes2;
  List<LookupResult> nodesEmpty;

  private void invalidateCacheDate() {
    final Date now = new Date();
    final Date outOfDate = new Date(now.getTime() - RetainingDnsSrvResolver.VALID_CACHE_LIFETIME);
    resolver.setLastValidResultDate(outOfDate);
  }

  private void updateCacheDate() {
    final Date now = new Date();
    final Date outOfDate = new Date(now.getTime() - RetainingDnsSrvResolver.VALID_CACHE_LIFETIME + 1);
    resolver.setLastValidResultDate(outOfDate);
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    delegate = mock(DnsSrvResolver.class);

    resolver = new RetainingDnsSrvResolver(delegate);

    nodesEmpty = nodes();
    nodes1 = nodes("noden1", "noden2");
    nodes2 = nodes("noden3", "noden5", "somethingelse");
  }

  @Test
  public void shouldReturnEmptyWhenDelegateResolveEmptyAndCacheIsEmpty() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodesEmpty);
    assertTrue(resolver.resolve(FQDN).isEmpty());
  }

  @Test
  public void shouldReturnEmptyWhenDelegateResolveEmptyAndCacheIsOutOfDate() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodesEmpty);

    resolver.resolve(FQDN);
    invalidateCacheDate();

    assertTrue(resolver.resolve(FQDN).isEmpty());
  }

  @Test
  public void shouldReturnCachedResultWhenDelegateResolveEmptyAndCacheIsValid() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodesEmpty);

    resolver.resolve(FQDN);
    updateCacheDate();

    assertThat(resolver.resolve(FQDN), equalTo(nodes1));
  }

  @Test
  public void shouldReturnResultWhenDelegateResolveResultAndCacheIsEmpty() throws Exception {
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
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodesEmpty);
    resolver.resolve(FQDN);
    assertThat(resolver.resolve(FQDN), equalTo(nodes1));
  }

  @Test
  public void shouldReturnLatestResultWhenDelegateResolveResultAndCacheIsOutOfDate() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodes2);

    resolver.resolve(FQDN);
    invalidateCacheDate();

    assertThat(resolver.resolve(FQDN), equalTo(nodes2));
  }

  @Test
  public void shouldReturnLatestResultWhenDelegateResolveResultAndCacheIsValid() throws Exception {
    when(delegate.resolve(FQDN)).thenReturn(nodes1).thenReturn(nodes2);

    resolver.resolve(FQDN);
    updateCacheDate();

    assertThat(resolver.resolve(FQDN), equalTo(nodes2));
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
  public void shouldNotStoreEmptyResults() throws Exception {
    when(delegate.resolve(FQDN))
        .thenReturn(nodesEmpty)
        .thenThrow(new DnsException("expected"));

    thrown.expect(DnsException.class);
    thrown.expectMessage("expected");

    resolver.resolve(FQDN);
    resolver.resolve(FQDN);
  }
}
