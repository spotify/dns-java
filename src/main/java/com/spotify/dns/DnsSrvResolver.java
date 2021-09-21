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
import java.util.concurrent.CompletionStage;

/**
 * Contract for doing SRV lookups.
 */
public interface DnsSrvResolver {
  /**
   * Does a DNS SRV lookup for the supplied fully qualified domain name, and returns the
   * matching results.
   * @deprecated
   * This method is deprecated in favor of the asynchronous version.
   * Use {@link DnsSrvResolver#resolveAsync(String)} instead
   *
   * @param fqdn a DNS name to query for
   * @return a possibly empty list of matching records
   * @throws DnsException if there was an error doing the DNS lookup
   */
  @Deprecated
  List<LookupResult> resolve(String fqdn);

  /**
   * Does a DNS SRV lookup for the supplied fully qualified domain name, and returns the
   * matching results.
   *
   * @param fqdn a DNS name to query for
   * @return a possibly empty list of matching records
   * @throws DnsException if there was an error doing the DNS lookup
   */
  default CompletionStage<List<LookupResult>> resolveAsync(String fqdn) {
    throw new java.lang.UnsupportedOperationException("Not implemented");
  }
}
