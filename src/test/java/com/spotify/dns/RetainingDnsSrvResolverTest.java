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

import static com.spotify.dns.DnsTestUtil.nodes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
  public void setUp() {
    delegate = mock(DnsSrvResolver.class);

    resolver = new RetainingDnsSrvResolver(delegate, RETENTION_TIME_MILLIS);

    nodes1 = nodes("noden1", "noden2");
    nodes2 = nodes("noden3", "noden5", "somethingelse");
  }

  @Test
  public void shouldReturnResultsFromDelegate() throws ExecutionException, InterruptedException {
    when(delegate.resolve(FQDN)).thenReturn(CompletableFuture.completedFuture(nodes1));

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get(), equalTo(nodes1));
  }

  @Test
  public void shouldReturnResultsFromDelegateEachTime() throws ExecutionException, InterruptedException {
    when(delegate.resolve(FQDN))
            .thenReturn(CompletableFuture.completedFuture(nodes1))
            .thenReturn(CompletableFuture.completedFuture(nodes2));

    resolver.resolve(FQDN).toCompletableFuture().get();

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get(), equalTo(nodes2));
  }

  @Test
  public void shouldRetainDataIfNewResultEmpty() throws ExecutionException, InterruptedException {
    when(delegate.resolve(FQDN))
            .thenReturn(CompletableFuture.completedFuture(nodes1))
            .thenReturn(CompletableFuture.completedFuture(nodes()));

    resolver.resolve(FQDN).toCompletableFuture().get();

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get(), equalTo(nodes1));
  }

  @Test
  public void shouldRetainDataOnFailure() throws ExecutionException, InterruptedException {
    when(delegate.resolve(FQDN))
        .thenReturn(CompletableFuture.completedFuture(nodes1))
        .thenReturn(CompletableFuture.failedFuture(new DnsException("expected")));

    resolver.resolve(FQDN).toCompletableFuture().get();

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get(), equalTo(nodes1));
  }

  @Test
  public void shouldThrowOnFailureAndNoDataAvailable() throws ExecutionException, InterruptedException {
    DnsException cause = new DnsException("expected");
    when(delegate.resolve(FQDN)).thenReturn(CompletableFuture.failedFuture(cause));

    thrown.expect(ExecutionException.class);
    thrown.expectCause(is(cause));

    resolver.resolve(FQDN).toCompletableFuture().get();
  }

  @Test
  public void shouldReturnEmptyOnEmptyAndNoDataAvailable() throws ExecutionException, InterruptedException {
    when(delegate.resolve(FQDN)).thenReturn(CompletableFuture.completedFuture(nodes()));

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get().isEmpty(), is(true));
  }

  @Test
  public void shouldNotStoreEmptyResults() throws ExecutionException, InterruptedException {
    DnsException cause = new DnsException("expected");
    when(delegate.resolve(FQDN))
        .thenReturn(CompletableFuture.completedFuture(nodes()))
        .thenReturn(CompletableFuture.failedFuture(cause));

    resolver.resolve(FQDN).toCompletableFuture().get();

    thrown.expect(ExecutionException.class);
    thrown.expectCause(is(cause));

    resolver.resolve(FQDN).toCompletableFuture().get();
  }

  @Test
  public void shouldNotRetainPastEndOfRetentionOnEmptyResults() throws Exception {
    when(delegate.resolve(FQDN))
        .thenReturn(CompletableFuture.completedFuture(nodes("aresult")))
        .thenReturn(CompletableFuture.completedFuture(nodes()));

    resolver.resolve(FQDN).toCompletableFuture().get();

    // expire retained entry
    Thread.sleep(RETENTION_TIME_MILLIS);

    assertThat(resolver.resolve(FQDN).toCompletableFuture().get().isEmpty(), is(true));
  }

  @Test
  public void shouldNotRetainPastEndOfRetentionOnException() throws Exception {
    DnsException expected = new DnsException("expected");
    when(delegate.resolve(FQDN))
        .thenReturn(CompletableFuture.completedFuture(nodes("aresult")))
        .thenReturn(CompletableFuture.failedFuture(expected));

    resolver.resolve(FQDN).toCompletableFuture().get();

    // expire retained entry
    Thread.sleep(RETENTION_TIME_MILLIS);

    thrown.expectCause(equalTo(expected));

    resolver.resolve(FQDN).toCompletableFuture().get();
  }

  @Test
  public void shouldThrowIfRetentionNegative() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("-4787");

    new RetainingDnsSrvResolver(delegate, -4787);
  }
}
