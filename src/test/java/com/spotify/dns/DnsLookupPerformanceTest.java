package com.spotify.dns;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class DnsLookupPerformanceTest {
    private static AtomicInteger failureCount = new AtomicInteger(0);

    @Test
    public static void runTest() throws InterruptedException {
        int numThreads = 50;
        final ExecutorService smallExecutorService = Executors.newFixedThreadPool(5);
        final ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<String> records = List.of("one.one.one.one.",
                "dns.quad9.net.",
                "dns11.quad9.net.",
                "lookup1.resolver.lax-noc.com.",
                "b.resolvers.Level3.net.",
                "dns1.nextdns.io.",
                "dns2.nextdns.io.",
                "resolver.qwest.net.",
                "dns1.ncol.net.",
                "ny.ahadns.net.",
                "dns1.puregig.net.",
                "primary.dns.bright.net.",
                "edelta2.DADNS.america.net.",
                "ns2.frii.com.",
                "dns3.dejazzd.com.",
                "ns7.dns.tds.net.",
                "ns1.ceinetworks.com.",
                "nsorl.fdn.com.",
                "dns2.norlight.net.",
                "safeservedns.com.",
                "unkname.unk.edu.",
                "redirect.centurytel.net.",
                "dns2.nextdns.io.",
                "Edelta.DADNS.america.net.",
                "gatekeeper.poly.edu.",
                "ns1.wavecable.com.",
                "ns2.wavecable.com.",
                "nrcns.s3woodstock.ga.atlanta.comcast.net.",
                "resolver1.opendns.com.",
                "cns1.Atlanta2.Level3.net.",
                "redirect.centurytel.net.",
                "x.ns.gin.ntt.net.",
                "rec1pubns2.ultradns.net.",
                "dns2.dejazzd.com.",
                "c.resolvers.level3.net.",
                "dnscache2.izoom.net.",
                "ns2.nyc.pnap.net.",
                "yardbird.cns.vt.edu.",
                "cns4.Atlanta2.Level3.net.",
                "nscache.prserv.net.",
                "nscache07.us.prserv.net.",
                "hvdns1.centurylink.net.",
                "a.resolvers.level3.net.",
                "ns2.socket.net.",
                "res1.dns.cogentco.com.",
                "rdns.dynect.net.");

        CountDownLatch done = new CountDownLatch(records.size() * 2);
        records.stream()
                .forEach(
                        fqdn -> {
                            executorService.submit(() -> resolve(fqdn, done));
                            CompletableFuture.runAsync(DnsLookupPerformanceTest::blockCommonPool)
                                    .whenComplete((v, ex) -> done.countDown());
                        });
        done.await(1, TimeUnit.MINUTES);
        executorService.shutdown();

        System.out.println("Number of threads: " + numThreads);
        System.out.println("Number of records: " + records.size());
        System.out.println("Failed lookups: " + failureCount);
    }

    private static void blockCommonPool() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void resolve(String fqdn, CountDownLatch done) {
        try {
            System.out.println("Resolving: " + fqdn);
            Lookup lookup = forName(fqdn);
            lookup.run();

            switch (lookup.getResult()) {
                case Lookup.SUCCESSFUL:
                    System.out.println(fqdn + "...ok!");
                    break;
                case Lookup.HOST_NOT_FOUND:
                case Lookup.TYPE_NOT_FOUND:
                    System.out.format("No results returned for query '%s'; result from XBill: %s - %s",
                            fqdn, lookup.getResult(), lookup.getErrorString());
                    break;
                default:
                    failureCount.incrementAndGet();
                    throw new RuntimeException(
                            String.format("Lookup of '%s' failed with code: %d - %s ",
                                    fqdn, lookup.getResult(), lookup.getErrorString()));
            }
        } catch (Exception e) {
            System.err.format("%s ... failed!\n", fqdn);
            e.printStackTrace(System.err);
        } finally {
            done.countDown();
        }
    }

    public static Lookup forName(String fqdn) {
        try {
            final Lookup lookup = new Lookup(fqdn, Type.A, DClass.IN);
            final ExtendedResolver resolver = new ExtendedResolver();
            resolver.setTimeout(Duration.ofMillis(5_000)); // Passes if raised a bit
            lookup.setResolver(resolver);
            return lookup;
        } catch (TextParseException e) {
            throw new RuntimeException("unable to create lookup for name: " + fqdn, e);
        }
    }
}