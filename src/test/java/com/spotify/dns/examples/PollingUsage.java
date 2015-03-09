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

package com.spotify.dns.examples;

import com.google.common.collect.Sets;

import com.spotify.dns.ChangeNotifier;
import com.spotify.dns.DnsException;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.DnsSrvWatcher;
import com.spotify.dns.DnsSrvWatchers;
import com.spotify.dns.ErrorHandler;
import com.spotify.dns.LookupResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public final class PollingUsage {

  public static void main(String[] args) throws IOException {
    DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
        .cachingLookups(true)
        .dnsLookupTimeoutMillis(1000)
        .build();

    DnsSrvWatcher<LookupResult> watcher = DnsSrvWatchers.newBuilder(resolver)
        .polling(1, TimeUnit.SECONDS)
        .withErrorHandler(new ErrorPrinter())
        .build();

    boolean quit = false;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (!quit) {
      System.out.print("Enter a SRV name (empty to quit): ");
      String line = in.readLine();

      if (line == null || line.isEmpty()) {
        quit = true;
      } else {
        try {
          ChangeNotifier<LookupResult> notifier = watcher.watch(line);
          notifier.setListener(new ChangeListener(line), false);
        } catch (DnsException e) {
          e.printStackTrace(System.out);
        }
      }
    }
  }

  static class ErrorPrinter implements ErrorHandler {

    @Override
    public void handle(String fqdn, DnsException exception) {
      System.out.println("Error with " + fqdn);
      exception.printStackTrace();
    }
  }

  static class ChangeListener implements ChangeNotifier.Listener<LookupResult> {

    final String name;

    ChangeListener(String name) {
      this.name = name;
    }

    @Override
    public void onChange(ChangeNotifier.ChangeNotification<LookupResult> changeNotification) {
      System.out.println("\nEndpoints changed for " + name);
      for (LookupResult endpoint : changeNotification.previous()) {
        System.out.println("  prev: " + endpoint);
      }

      for (LookupResult endpoint : changeNotification.current()) {
        System.out.println("  curr: " + endpoint);
      }

      final Sets.SetView<LookupResult> unchanged =
          Sets.intersection(changeNotification.current(), changeNotification.previous());

      for (LookupResult endpoint : unchanged) {
        System.out.println("  noch: " + endpoint);
      }
    }
  }
}
