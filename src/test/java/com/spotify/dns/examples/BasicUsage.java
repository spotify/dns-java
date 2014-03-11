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

import com.google.common.net.HostAndPort;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.DnsException;
import com.spotify.dns.DnsSrvResolvers;
import com.spotify.statistics.MuninReporter;
import com.spotify.statistics.MuninReporterConfig;
import com.yammer.metrics.Metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BasicUsage {
  public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
    DnsSrvResolver resolver = DnsSrvResolvers.newBuilder()
        .cachingLookups(true)
        .retainingDataOnFailures(true)
        .metered(true)
        .dnsLookupTimeoutMillis(1000)
        .build();

    MuninReporterConfig reporterConfig = new MuninReporterConfig();

    DnsSrvResolvers.configureMuninGraphs(reporterConfig.category("dns"));

    MuninReporter reporter = new MuninReporter(Metrics.defaultRegistry(), reporterConfig);
    reporter.start();

    boolean quit = false;
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    while (!quit) {
      System.out.print("Enter a SRV name: ");
      String line = in.readLine();

      if (line == null) {
        quit = true;
      } else {
        try {
          List<HostAndPort> nodes = resolver.resolve(line);

          for (HostAndPort node : nodes) {
            System.out.println(node);
          }
        }
        catch (DnsException e) {
          e.printStackTrace(System.out);
        }
      }
    }
  }
}
