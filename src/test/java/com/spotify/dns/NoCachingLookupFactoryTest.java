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

public class NoCachingLookupFactoryTest {
    NoCachingLookupFactory factory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        factory = new NoCachingLookupFactory();
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