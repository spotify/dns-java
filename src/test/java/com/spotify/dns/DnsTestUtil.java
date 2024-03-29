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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility functions that are shared between tests.
 */
public class DnsTestUtil {
  static List<LookupResult> nodes(String... nodeNames) {
    return Stream.of(nodeNames)
        .map(input -> LookupResult.create(input, 8080, 1, 2, 999))
        .collect(Collectors.toList());
  }

  /**
   * method to replace CompletableFuture.failedFuture() from Java 9 in Java 8
   */
  static CompletableFuture<List<LookupResult>> failedFuture(Exception ex) {
    CompletableFuture<List<LookupResult>> future = new CompletableFuture<>();
    future.completeExceptionally(ex);
    return future;
  }

  /**
   * method to replace CompletableFuture.failedFuture() from Java 9 in Java 8
   */
  static CompletableFuture<List<LookupResult>> failedFuture(Error error) {
    CompletableFuture<List<LookupResult>> future = new CompletableFuture<>();
    future.completeExceptionally(error);
    return future;
  }
}
