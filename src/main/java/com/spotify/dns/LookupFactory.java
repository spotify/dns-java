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

import org.xbill.DNS.Lookup;
import org.xbill.DNS.lookup.LookupSession;

/**
 * Library-internal interface used for finding or creating {@link LookupSession} or {@link Lookup} instances.
 */
interface LookupFactory {
  /**
   * Returns a {@link Lookup} instance capable of doing SRV lookups for the supplied FQDN.
   * @deprecated
   * This synchronous method is being deprecated.
   * Use {@link LookupFactory#sessionForName(String)} instead
   *
   * @param fqdn the name to do lookups for
   * @return a Lookup instance
   */
  @Deprecated
  Lookup forName(String fqdn);

  /**
   * Returns a {@link LookupSession} instance capable of doing SRV lookups for the supplied FQDN.
   * @param fqdn the name to do lookups for
   * @return a Lookup instance
   */
  LookupSession sessionForName(String fqdn);
}
