package io.github.mundanej.mjdi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ProvidersTest {
  @Test
  void singletonMemoizesFirstValue() {
    AtomicInteger calls = new AtomicInteger();
    ContextProvider<Object> provider =
        Providers.singleton(
            ignored -> {
              calls.incrementAndGet();
              return new Object();
            });
    AppContext context = BootstrapAppContext.create(List.of());

    Object first = provider.get(context);
    Object second = provider.get(context);

    assertSame(first, second);
    assertEquals(1, calls.get());
  }

  @Test
  void singletonRejectsNullProvider() {
    assertThrows(NullPointerException.class, () -> Providers.singleton(null));
  }

  @Test
  void singletonMemoizesNullResult() {
    AtomicInteger calls = new AtomicInteger();
    ContextProvider<Object> provider =
        Providers.singleton(
            ignored -> {
              calls.incrementAndGet();
              return null;
            });
    AppContext context = BootstrapAppContext.create(List.of());

    assertNull(provider.get(context));
    assertNull(provider.get(context));

    assertEquals(1, calls.get());
  }

  @Test
  void singletonRetriesWhenDelegateThrows() {
    AtomicInteger calls = new AtomicInteger();
    RuntimeException failure = new RuntimeException("failed");
    ContextProvider<String> provider =
        Providers.singleton(
            ignored -> {
              if (calls.incrementAndGet() == 1) {
                throw failure;
              }
              return "created";
            });
    AppContext context = BootstrapAppContext.create(List.of());

    assertSame(failure, assertThrows(RuntimeException.class, () -> provider.get(context)));
    assertEquals("created", provider.get(context));
    assertEquals("created", provider.get(context));

    assertEquals(2, calls.get());
  }

  @Test
  void singletonPassesContextToDelegate() {
    Object value = new Object();
    AtomicReference<AppContext> seenContext = new AtomicReference<>();
    ContextProvider<Object> provider =
        Providers.singleton(
            context -> {
              seenContext.set(context);
              return value;
            });
    AppContext context = BootstrapAppContext.create(List.of());

    assertSame(value, provider.get(context));

    assertSame(context, seenContext.get());
  }

  @Test
  void singletonInitializesOnceForConcurrentCalls() throws Exception {
    int workers = 16;
    AtomicInteger calls = new AtomicInteger();
    Object value = new Object();
    CountDownLatch ready = new CountDownLatch(workers);
    CountDownLatch start = new CountDownLatch(1);
    ContextProvider<Object> provider =
        Providers.singleton(
            ignored -> {
              calls.incrementAndGet();
              sleepBriefly();
              return value;
            });
    AppContext context = BootstrapAppContext.create(List.of());
    ExecutorService executor = Executors.newFixedThreadPool(workers);

    try {
      List<Future<Object>> futures = new ArrayList<>();
      for (int i = 0; i < workers; i++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  assertTrue(start.await(5, TimeUnit.SECONDS));
                  return provider.get(context);
                }));
      }

      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();

      HashSet<Object> results = new HashSet<>();
      for (Future<Object> future : futures) {
        results.add(future.get(5, TimeUnit.SECONDS));
      }

      assertEquals(1, results.size());
      assertSame(value, results.iterator().next());
      assertEquals(1, calls.get());
    } finally {
      executor.shutdownNow();
    }
  }

  private static void sleepBriefly() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
