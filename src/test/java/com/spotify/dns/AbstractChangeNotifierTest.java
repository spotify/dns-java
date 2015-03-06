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

import com.google.common.collect.Sets;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AbstractChangeNotifierTest {

  @Mock
  public ChangeNotifier.Listener<String> listener;

  @Mock
  public ChangeNotifier.ChangeNotification<String> changeNotification;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  AbstractChangeNotifier<String> sut;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    sut = new AbstractChangeNotifier<String>() {
      @Override
      public Set<String> current() {
        return Sets.newHashSet("foo", "bar");
      }

      @Override
      public void closeImplementation() {
      }
    };
  }

  @Test
  public void shouldRegisterListener() throws Exception {
    sut.setListener(listener, false);
    sut.fireRecordsUpdated(changeNotification);

    verify(listener).onChange(changeNotification);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotFireImmediatelyIfFalse() throws Exception {
    sut.setListener(listener, false);

    verify(listener, never()).onChange(any(ChangeNotifier.ChangeNotification.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldFireImmediatelyIfTrue() throws Exception {
    sut.setListener(listener, true);

    final ArgumentCaptor<ChangeNotifier.ChangeNotification> captor =
        ArgumentCaptor.forClass(ChangeNotifier.ChangeNotification.class);
    verify(listener).onChange(captor.capture());

    final ChangeNotifier.ChangeNotification<String> notification = captor.getValue();
    assertThat(notification.previous().size(), is(0));
    assertThat(notification.current().size(), is(2));
    assertThat(notification.current(), containsInAnyOrder("foo", "bar"));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotFireAfterClose() throws Exception {
    sut.setListener(listener, false);
    sut.close();
    sut.fireRecordsUpdated(changeNotification);

    verify(listener, never()).onChange(any(ChangeNotifier.ChangeNotification.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldNotAllowMultipleListeners() throws Exception {
    sut.setListener(listener, false);

    thrown.expect(IllegalStateException.class);
    sut.setListener(mock(ChangeNotifier.Listener.class), false);
  }
}
