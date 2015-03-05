/*
 * Copyright (c) 2012-2015 Spotify AB
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


import com.google.common.base.Function;
import com.google.common.base.Functions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.collect.ImmutableList.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServiceResolvingChangeNotifierTest {

  private static final String FQDN = "example.com";

  @Mock
  public DnsSrvResolver resolver;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldCallListenerOnChange() throws Exception {
    ChangeNotifierFactory.RunnableChangeNotifier<LookupResult> sut = createNotifier();
    ChangeNotifier.Listener<LookupResult> listener = mock(ChangeNotifier.Listener.class);
    sut.setListener(listener, false);

    LookupResult result1 = result("host", 1234);
    LookupResult result2 = result("host", 4321);
    when(resolver.resolve(FQDN))
        .thenReturn(of(result1), of(result1, result2));

    sut.run();
    sut.run();

    ArgumentCaptor<ChangeNotifier.ChangeNotification> captor =
        ArgumentCaptor.forClass(ChangeNotifier.ChangeNotification.class);
    verify(listener, times(2)).onChange(captor.capture());

    List<ChangeNotifier.ChangeNotification> notifications = captor.getAllValues();
    assertThat(notifications.size(), is(2));

    ChangeNotifier.ChangeNotification change1 = notifications.get(0);
    assertThat(change1.previous().size(), is(0));
    assertThat(change1.current().size(), is(1));
    Assert.<Set<LookupResult>>assertThat(change1.current(), containsInAnyOrder(result1));

    ChangeNotifier.ChangeNotification change2 = notifications.get(1);
    assertThat(change2.previous().size(), is(1));
    Assert.<Set<LookupResult>>assertThat(change2.previous(), containsInAnyOrder(result1));
    assertThat(change2.current().size(), is(2));
    Assert.<Set<LookupResult>>assertThat(change2.current(), containsInAnyOrder(result1, result2));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldCallListenerOnSet() throws Exception {
    ChangeNotifierFactory.RunnableChangeNotifier<LookupResult> sut = createNotifier();
    ChangeNotifier.Listener<LookupResult> listener = mock(ChangeNotifier.Listener.class);

    LookupResult result = result("host", 1234);
    when(resolver.resolve(FQDN))
        .thenReturn(of(result));

    sut.run();
    sut.setListener(listener, true);

    ArgumentCaptor<ChangeNotifier.ChangeNotification> captor =
        ArgumentCaptor.forClass(ChangeNotifier.ChangeNotification.class);
    verify(listener).onChange(captor.capture());

    ChangeNotifier.ChangeNotification notification = captor.getValue();
    assertThat(notification.previous().size(), is(0));
    assertThat(notification.current().size(), is(1));
    Assert.<Set<LookupResult>>assertThat(notification.current(), containsInAnyOrder(result));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldOnlyChangeIfTransformedValuesChange() throws Exception {
    ChangeNotifierFactory.RunnableChangeNotifier<String> sut = createHostNotifier();
    ChangeNotifier.Listener<String> listener = mock(ChangeNotifier.Listener.class);
    sut.setListener(listener, false);

    LookupResult result1 = result("host", 1234);
    LookupResult result2 = result("host", 4321);
    when(resolver.resolve(FQDN))
        .thenReturn(of(result1), of(result1, result2));

    sut.run();
    sut.run();

    ArgumentCaptor<ChangeNotifier.ChangeNotification> captor =
        ArgumentCaptor.forClass(ChangeNotifier.ChangeNotification.class);
    verify(listener).onChange(captor.capture());

    ChangeNotifier.ChangeNotification notification = captor.getValue();
    assertThat(notification.previous().size(), is(0));
    assertThat(notification.current().size(), is(1));
    Assert.<Set<String>>assertThat(notification.current(), containsInAnyOrder("host"));
  }

  private ChangeNotifierFactory.RunnableChangeNotifier<LookupResult> createNotifier() {
    return new ServiceResolvingChangeNotifier<LookupResult>(
        resolver, FQDN, Functions.<LookupResult>identity());
  }

  private ChangeNotifierFactory.RunnableChangeNotifier<String> createHostNotifier() {
    return new ServiceResolvingChangeNotifier<String>(
        resolver, FQDN, new Function<LookupResult, String>() {
      @Nullable
      @Override
      public String apply(@Nullable LookupResult input) {
        return input != null ? input.host() : null;
      }
    });
  }

  private LookupResult result(String host, int port) {
    return LookupResult.create(host, port, 1, 5000, 300);
  }
}
