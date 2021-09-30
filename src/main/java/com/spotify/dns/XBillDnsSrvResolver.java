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

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;

import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;
import org.xbill.DNS.lookup.NoSuchRRSetException;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * A DnsSrvResolver implementation that uses the dnsjava implementation:
 * https://github.com/dnsjava/dnsjava
 */
class XBillDnsSrvResolver implements DnsSrvResolver {
  private static final Logger LOG = LoggerFactory.getLogger(XBillDnsSrvResolver.class);

  private final LookupFactory lookupFactory;

  XBillDnsSrvResolver(LookupFactory lookupFactory) {
    this.lookupFactory = requireNonNull(lookupFactory, "lookupFactory");
  }

  @Override
  public List<LookupResult> resolve(final String fqdn) {
    try {
      return resolveAsync(fqdn).toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      throw new DnsException("Failed lookup: " + e.getMessage());
    }
  }

  @Override
  public CompletionStage<List<LookupResult>> resolveAsync(final String fqdn) {
    LookupSession lookup = lookupFactory.sessionForName(fqdn);
    Name name;
    try {
      name = Name.fromString(fqdn);
    } catch (TextParseException e) {
      throw new DnsException("unable to create lookup for name: " + fqdn, e);
    }

    return lookup.lookupAsync(name, Type.SRV, DClass.IN).handle((result, ex) ->{
      if (ex == null){
        return toLookupResults(result);
      } else{
        Throwable cause = ex;
        if (ex instanceof CompletionException && ex.getCause() != null) {
          cause = ex.getCause();
        }
        if (cause instanceof NoSuchRRSetException || cause instanceof NoSuchDomainException) {
          LOG.warn("No results returned for query '{}'; result from dnsjava: {}",
                  fqdn, ex.getMessage());
          return ImmutableList.of();
        }
        throw new DnsException(
                String.format("Lookup of '%s' failed: %s ", fqdn, ex.getMessage()), ex);
      }
    });
  }

  private static List<LookupResult> toLookupResults(org.xbill.DNS.lookup.LookupResult queryResult) {
    ImmutableList.Builder<LookupResult> builder = ImmutableList.builder();

    for (Record record: queryResult.getRecords()) {
      if (record instanceof SRVRecord) {
        SRVRecord srvRecord = (SRVRecord) record;
        builder.add(LookupResult.create(srvRecord.getTarget().toString(),
                                        srvRecord.getPort(),
                                        srvRecord.getPriority(),
                                        srvRecord.getWeight(),
                                        srvRecord.getTTL()));
      }
    }

    return builder.build();
  }
}
