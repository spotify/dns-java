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

package com.spotify.dns.examples;

import com.google.common.base.Function;

import com.spotify.dns.DnsException;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.dns.EndpointProvider;
import com.spotify.dns.LookupResult;
import com.spotify.dns.PollingDnsSrvResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public final class PollingUsage {

  public static void main(String[] args) throws IOException {
    DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
        .cachingLookups(true)
        .retainingDataOnFailures(true)
        .dnsLookupTimeoutMillis(1000)
        .build();

    PollingDnsSrvResolver<String> poller = DnsSrvResolvers.pollingResolver(resolver,
      new Function<LookupResult, String>() {
        @Nullable
        @Override
        public String apply(@Nullable LookupResult input) {
          return input.toString() + System.currentTimeMillis() / 5000;
        }
      }
    );

    boolean quit = false;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (!quit) {
      System.out.print("Enter a SRV name (empty to quit): ");
      String line = in.readLine();

      if (line == null || line.isEmpty()) {
        quit = true;
      } else {
        try {
          poller.poll(line, 1, TimeUnit.SECONDS)
              .setListener(new EndpointListener(line), false);
        }
        catch (DnsException e) {
          e.printStackTrace(System.out);
        }
      }
    }
  }

  static class EndpointListener implements EndpointProvider.Listener<String> {

    final String name;

    EndpointListener(String name) {
      this.name = name;
    }

    @Override
    public void endpointsChanged(EndpointProvider<String> endpointProvider) {
      System.out.println("\nEndpoints changed for " + name);
      for (String endpoint : endpointProvider.getEndpoints()) {
        System.out.println("  " + endpoint);
      }
    }
  }
}
