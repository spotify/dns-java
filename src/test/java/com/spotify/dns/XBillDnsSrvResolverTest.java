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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class XBillDnsSrvResolverTest {
  XBillDnsSrvResolver resolver;

  LookupFactory lookupFactory;
  Resolver xbillResolver;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() {
    lookupFactory = mock(LookupFactory.class);

    resolver = new XBillDnsSrvResolver(lookupFactory);

    xbillResolver = mock(Resolver.class);
  }

  @After
  public void tearDown() {
    Lookup.refreshDefault();
  }

  @Test
  public void shouldReturnResultsFromLookup() throws Exception {
    String fqdn = "thefqdn.";
    String[] resultNodes = new String[] { "node1.domain.", "node2.domain." };

    setupResponseForQuery(fqdn, fqdn, resultNodes);

    List<LookupResult> actual = resolver.resolve(fqdn);

    Set<String> nodeNames = actual.stream().map(LookupResult::host).collect(Collectors.toSet());

    assertThat(nodeNames, containsInAnyOrder(resultNodes));
  }

  @Test
  public void shouldIndicateCauseFromXBillIfLookupFails() throws Exception {
    thrown.expect(DnsException.class);
    thrown.expectMessage("response does not match query");

    String fqdn = "thefqdn.";
    setupResponseForQuery(fqdn, "somethingelse.", "node1.domain.", "node2.domain.");

    resolver.resolve(fqdn);
  }

  @Test
  public void shouldIndicateNameIfLookupFails() throws Exception {
    thrown.expect(DnsException.class);
    thrown.expectMessage("thefqdn.");

    String fqdn = "thefqdn.";
    setupResponseForQuery(fqdn, "somethingelse.", "node1.domain.", "node2.domain.");

    resolver.resolve(fqdn);
  }

  @Test
  public void shouldReturnEmptyForHostNotFound() throws Exception {
    String fqdn = "thefqdn.";

    when(lookupFactory.forName(fqdn)).thenReturn(testLookup(fqdn));
    when(xbillResolver.send(any(Message.class))).thenReturn(messageWithRCode(fqdn, Rcode.NXDOMAIN));

    assertThat(resolver.resolve(fqdn).isEmpty(), is(true));
  }

  // not testing for type not found, as I don't know how to set that up...

  private Message messageWithRCode(String query, int rcode) throws TextParseException {
    Name queryName = Name.fromString(query);
    Record question = Record.newRecord(queryName, Type.SRV, DClass.IN);
    Message queryMessage = Message.newQuery(question);
    Message result = new Message();
    result.setHeader(queryMessage.getHeader());
    result.addRecord(question, Section.QUESTION);

    result.getHeader().setRcode(rcode);

    return result;
  }

  private void setupResponseForQuery(String queryFqdn, String responseFqdn, String... results)
      throws IOException {
    when(lookupFactory.forName(queryFqdn)).thenReturn(testLookup(queryFqdn));
    when(xbillResolver.send(any(Message.class)))
        .thenReturn(messageWithNodes(responseFqdn, results));
  }

  private Lookup testLookup(String thefqdn) throws TextParseException {
    Lookup result = new Lookup(thefqdn, Type.SRV);

    result.setResolver(xbillResolver);

    return result;
  }

  private Message messageWithNodes(String query, String[] names) throws TextParseException {
    Name queryName = Name.fromString(query);
    Record question = Record.newRecord(queryName, Type.SRV, DClass.IN);
    Message queryMessage = Message.newQuery(question);
    Message result = new Message();
    result.setHeader(queryMessage.getHeader());
    result.addRecord(question, Section.QUESTION);

    for (String name1 : names) {
      result.addRecord(
          new SRVRecord(queryName, DClass.IN, 1, 1, 1, 8080, Name.fromString(name1)),
          Section.ANSWER);
    }

    return result;
  }
}
