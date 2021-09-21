package com.spotify.dns;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.xbill.DNS.lookup.NoSuchDomainException;

public class DnsSrvWatchersTest {

  @Test
  public void noRaceBetweenSetListenerAndPollingForUpdates() throws Exception {
    int limit = 20000;
    while (limit-- > 0) {
      resolve();
    }
  }

  private void resolve() throws Exception {
    final DnsSrvResolver srvResolver = new FakeResolver(
        "horse.sto3.spotify.net", LookupResult.create("localhost", 1, 0, 0, 0));

    final AtomicReference<Set<LookupResult>> hosts = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);

    final ChangeNotifier.Listener<LookupResult> listener = new FakeListener(hosts, latch);

    DnsSrvWatcher<LookupResult> watcher = DnsSrvWatchers.newBuilder(srvResolver)
        .polling(1, TimeUnit.MILLISECONDS)
        .build();
    final ChangeNotifier<LookupResult> notifier = watcher.watch("horse.sto3.spotify.net");

    // since I set fire to true I'd expect being notified either right away or after a millisecond
    notifier.setListener(listener, true);
    latch.await();
    notifier.close();
    watcher.close();
    assertThat(hosts.get(), contains(is(LookupResult.create("localhost", 1, 0, 0, 0))));
  }

  static class FakeListener implements ChangeNotifier.Listener<LookupResult> {

    private final AtomicReference<Set<LookupResult>> hosts;
    private final CountDownLatch latch;

    FakeListener(AtomicReference<Set<LookupResult>> hosts, CountDownLatch latch) {
      this.hosts = hosts;
      this.latch = latch;
    }

    @Override
    public void onChange(ChangeNotifier.ChangeNotification<LookupResult> changeNotification) {

      hosts.set(changeNotification.current());
      if (!changeNotification.current().isEmpty()) {
        latch.countDown();
      }

    }
  }

  static class FakeResolver implements DnsSrvResolver {

    private final String fqdn;
    private final LookupResult result;

    public FakeResolver(String fqdn, LookupResult result) {
      this.fqdn = fqdn;
      this.result = result;
    }

    @Override
    public List<LookupResult> resolve(String fqdn) {
      if (this.fqdn.equals(fqdn)) {
        return Arrays.asList(result);
      } else {
        return null;
      }
    }

    @Override
    public CompletionStage<List<LookupResult>> resolveAsync(String fqdn) {
      if (this.fqdn.equals(fqdn)) {
        return CompletableFuture.completedFuture(Arrays.asList(result));
      } else {
        return DnsTestUtil.failedFuture(new DnsException(this.fqdn + " != " + fqdn));
      }
    }
  }
}
