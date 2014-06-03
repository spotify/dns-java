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

package com.spotify.dns.statistics;

/**
 * Implement to report statistics for DNS request.
 *
 * This interface exists to allow implementers to bridge the statistics
 * collected through the use of this library with their own statistics solution.
 */
public interface DnsReporter {
  /**
   * Report resolve timing.
   * @return A new timing context.
   */
  DnsTimingContext resolveTimer();

  /**
   * Report that an empty response has been received from a resolve.
   */
  void reportEmpty();

  /**
   * Report that a resolve resulting in a failure.
   * @param error The exception causing the failure.
   */
  void reportFailure(Throwable error);
}
