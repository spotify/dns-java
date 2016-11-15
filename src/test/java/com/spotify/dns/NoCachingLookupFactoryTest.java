package com.spotify.dns;

import com.google.common.base.Throwables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Type;

@RunWith(MockitoJUnitRunner.class)
public class NoCachingLookupFactoryTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    LookupFactory delegate;

    @Test
    public void shouldCallDelegateTwiceWhenResolvingADomainTwice() throws Exception {
        String fdn = "_spotify-client._tcp.spotify.com.";

        final Resolver resolver = Mockito.mock(Resolver.class);

        // Lets fetch one record just to save some mocking code
        Record someRecord = new SimpleLookupFactory().forName(fdn).run()[0];

        Mockito.when(resolver.send(Mockito.any(Message.class))).thenReturn(Message.newQuery(someRecord));

        delegate = new LookupFactory() {
            @Override
            public Lookup forName(String fqdn) {
                try {
                    Lookup lookupResult = new Lookup(fqdn, Type.SRV, DClass.IN);
                    lookupResult.setResolver(resolver);
                    return lookupResult;
                } catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        };

        NoCachingLookupFactory factory = new NoCachingLookupFactory(delegate);

        factory.forName(fdn).run();
        factory.forName(fdn).run();

        Mockito.verify(resolver, Mockito.times(2)).send(Mockito.any(Message.class));
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
        new NoCachingLookupFactory(delegate).forName("some.domain.");
    }
}