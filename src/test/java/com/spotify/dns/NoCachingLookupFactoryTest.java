package com.spotify.dns;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.xbill.DNS.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class NoCachingLookupFactoryTest {
    NoCachingLookupFactory factory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    LookupFactory delegate;

    @Before
    public void setUp() throws Exception {
        factory = new NoCachingLookupFactory(delegate);
    }

    @Test
    public void shouldCallDelegate() throws Exception {
        Mockito.when(delegate.forName(Mockito.anyString())).thenReturn(new Lookup("some.domain.", Type.SRV, DClass.IN));

        assertThat(factory.forName("some.domain."), is(notNullValue()));

        Mockito.verify(delegate).forName(Mockito.eq("some.domain."));
    }

    @Test
    public void shouldNotAllowNullDelegate() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Delegate lookup factory cannot be null");
        new NoCachingLookupFactory(null);
    }

    @Test
    public void shouldPropagateWhenErrorOnDelegate() throws Exception {
        Mockito.when(delegate.forName(Mockito.anyString())).thenThrow(new RuntimeException("Error resolving"));
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error resolving");
        factory.forName("some.domain.");
    }
}