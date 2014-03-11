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

package com.spotify.dns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TextParseException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


public class SimpleLookupFactoryTest {
  SimpleLookupFactory factory;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    factory = new SimpleLookupFactory();
  }

  @Test
  public void shouldCreateLookups() throws Exception {
    assertThat(factory.forName("some.domain."), is(notNullValue()));
  }

  @Test
  public void shouldCreateNewLookupsEachTime() throws Exception {
    Lookup first = factory.forName("some.other.name.");
    Lookup second = factory.forName("some.other.name.");

    assertThat(first == second, is(false));
  }

  @Test
  public void shouldRethrowXBillExceptions() throws Exception {
    thrown.expect(DnsException.class);
    thrown.expectCause(isA(TextParseException.class));

    factory.forName("bad\\1 name");
  }
}
