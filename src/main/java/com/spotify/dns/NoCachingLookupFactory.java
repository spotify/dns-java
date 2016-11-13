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

import org.xbill.DNS.*;

/**
 * A LookupFactory that always returns new instances and setting the lookup to not use the cache
 */
public class NoCachingLookupFactory implements LookupFactory {

  @Override
  public Lookup forName(String fqdn) {
    try {
      Lookup lookup = new Lookup(fqdn, Type.SRV, DClass.IN);
      lookup.setCache(null);
      return lookup;
    } catch (TextParseException e) {
      throw new DnsException("unable to create lookup for name: " + fqdn, e);
    }
  }
}
