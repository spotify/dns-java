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

import com.spotify.dns.DnsException;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.LookupResult;
import com.spotify.dns.statistics.DnsReporter;
import com.spotify.dns.statistics.DnsTimingContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class BasicUsage {

  private static final DnsReporter REPORTER = new StdoutReporter();

  public static void main(String[] args) throws IOException {
    DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
        .cachingLookups(true)
        .retainingDataOnFailures(true)
        .metered(REPORTER)
        .dnsLookupTimeoutMillis(1000)
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
          List<LookupResult> nodes = resolver.resolve(line);

          for (LookupResult node : nodes) {
            System.out.println(node);
          }
        } catch (DnsException e) {
          e.printStackTrace(System.out);
        }
      }
    }
  }

  public static class StdoutReporter implements DnsReporter {
    @Override
    public DnsTimingContext resolveTimer() {
      return new DnsTimingContext() {
        private final long start = System.currentTimeMillis();

        @Override
        public void stop() {
          final long now = System.currentTimeMillis();
          final long diff = now - start;
          System.out.println("Request took " + diff + "ms");
        }
      };
    }

    @Override
    public void reportFailure(Throwable error) {
      System.err.println("Error when resolving: " + error);
      error.printStackTrace(System.err);
    }

    @Override
    public void reportEmpty() {
      System.out.println("Empty response from server.");
    }
  }
}
