/*
 * Copyright (c) 2016 Spotify AB
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
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AggregatingChangeNotifierTest {
  @Test
  public void testEmptySet() throws Exception {
    MyNotifier childNotifier = new MyNotifier();
    AggregatingChangeNotifier<String> notifier = new AggregatingChangeNotifier<String>(ImmutableList.<ChangeNotifier<String>>of(childNotifier));

    ChangeNotifier.Listener listener = mock(ChangeNotifier.Listener.class);
    notifier.setListener(listener, false);

    verify(listener, never()).onChange(any(ChangeNotifier.ChangeNotification.class));

    childNotifier.set(ImmutableSet.<String>of());

    verify(listener, times(1)).onChange(any(ChangeNotifier.ChangeNotification.class));
    verifyNoMoreInteractions(listener);

  }

  private static class MyNotifier extends AbstractChangeNotifier<String> {
    private volatile Set<String> records = ImmutableSet.of();

    @Override
    protected void closeImplementation() {
    }

    @Override
    public Set<String> current() {
      return records;
    }

    public void set(Set<String> records) {
      fireRecordsUpdated(newChangeNotification(records, current()));
      this.records = records;
    }
  }
}
