///*
// * Copyright (c) 2015 Spotify AB
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.spotify.dns;
//
//import static org.hamcrest.CoreMatchers.equalTo;
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.not;
//import static org.junit.Assert.assertThat;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import org.junit.Before;
//import org.junit.Test;
//import org.xbill.DNS.Lookup;
//
//public class CachingLookupFactoryTest {
//  CachingLookupFactory factory;
//
//  LookupFactory delegate;
//
//  Lookup lookup;
//  Lookup lookup2;
//
//  @Before
//  public void setUp() throws Exception {
//    delegate = mock(LookupFactory.class);
//
//    factory = new CachingLookupFactory(delegate);
//
//    lookup = new Lookup("hi");
//    lookup2 = new Lookup("hey");
//  }
//
//  @Test
//  public void shouldReturnResultsFromDelegate() {
//    when(delegate.forName("a name")).thenReturn(lookup);
//
//    assertThat(factory.forName("a name"), equalTo(lookup));
//  }
//
//  @Test
//  public void shouldCacheResultsForSubsequentQueries() {
//    when(delegate.forName("hej")).thenReturn(lookup, lookup2);
//
//    Lookup first = factory.forName("hej");
//    Lookup second = factory.forName("hej");
//
//    assertThat(first == second, is(true));
//  }
//
//  @Test
//  public void shouldReturnDifferentForDifferentQueries() {
//    when(delegate.forName("hej")).thenReturn(lookup);
//    when(delegate.forName("hopp")).thenReturn(lookup2);
//
//    Lookup first = factory.forName("hej");
//    Lookup second = factory.forName("hopp");
//
//    assertThat(first == second, is(false));
//  }
//
//  @Test
//  public void shouldReturnDifferentForDifferentThreads() throws Exception {
//    ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
//    factory = new CachingLookupFactory(new SimpleLookupFactory());
//
//    Lookup first = factory.forName("hej");
//    Lookup second = executorService.submit(() -> factory.forName("hej")).get();
//
//    assertThat(second, not(equalTo(first)));
//  }
//}
