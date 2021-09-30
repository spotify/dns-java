package com.spotify.dns;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LookupExecutor {

  private static int NUM_THREADS = 10;
  private static LookupExecutor lookupExecutor = null;

  public ExecutorService executorService;

  private LookupExecutor() {
    executorService = Executors.newFixedThreadPool(NUM_THREADS);
  }

  public static LookupExecutor getInstance() {
    if (lookupExecutor == null) {
      lookupExecutor = new LookupExecutor();
    }

    return lookupExecutor;
  }

}
