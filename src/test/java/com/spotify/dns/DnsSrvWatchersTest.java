package com.spotify.dns;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

public class DnsSrvWatchersTest {

  @Test
  public void triggerRace() throws Exception {
    int limit = 100000;
    while (limit-- > 0) {
      System.out.println(limit);
      resolve();
    }
  }

  private void resolve() throws Exception {
    final DnsSrvResolver srvResolver = new MockResolver(
        "horse.sto3.spotify.net", LookupResult.create("localhost", 1, 0, 0, 0));

    final AtomicReference<Set<LookupResult>> hosts = new AtomicReference<Set<LookupResult>>();
    final CountDownLatch latch = new CountDownLatch(1);

    final ChangeNotifier.Listener<LookupResult> listener = new MockListener(hosts, latch);

    DnsSrvWatcher<LookupResult> watcher = DnsSrvWatchers.newBuilder(srvResolver)
        .polling(1, TimeUnit.MILLISECONDS)
        .build();
    final ChangeNotifier<LookupResult> notifier = watcher.watch("horse.sto3.spotify.net");

    // since I set fire to true I'd expect being notified either right away or after a millisecond
    notifier.setListener(listener, true);
    latch.await();
    notifier.close();
    watcher.close();
    assertThat(hosts.get(), not(empty()));
  }

  static class MockListener implements ChangeNotifier.Listener<LookupResult> {

    private final AtomicReference<Set<LookupResult>> hosts;
    private final CountDownLatch latch;

    MockListener(AtomicReference<Set<LookupResult>> hosts, CountDownLatch latch) {
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

  static class MockResolver implements DnsSrvResolver {

    private final String fqdn;
    private final LookupResult result;

    public MockResolver(String fqdn, LookupResult result) {
      this.fqdn = fqdn;
      this.result = result;
    }

    @Override
    public List<LookupResult> resolve(String fqdn) {
      if (this.fqdn.equals(fqdn)) {
        return ImmutableList.of(result);
      } else {
        return null;
      }
    }
  }
}
