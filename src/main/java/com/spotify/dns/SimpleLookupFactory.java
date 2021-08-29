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

import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupSession;

/** A LookupFactory that always returns new instances. */
public class SimpleLookupFactory implements LookupFactory {
  private final LookupSession session;

  public SimpleLookupFactory() {
    this(Lookup.getDefaultResolver());
  }

  public SimpleLookupFactory(Resolver resolver) {
    session = LookupSession.builder().resolver(resolver).build();
  }

  @Override
  public LookupSession forName(String fqdn) {
    return session;
  }
}
