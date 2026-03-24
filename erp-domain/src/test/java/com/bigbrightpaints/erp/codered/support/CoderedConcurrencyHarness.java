package com.bigbrightpaints.erp.codered.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;
import java.util.function.Predicate;

public final class CoderedConcurrencyHarness {

  private CoderedConcurrencyHarness() {}

  public static <T> RunResult<T> run(
      int threads,
      int maxRetriesPerThread,
      Duration timeout,
      IntFunction<Callable<T>> callFactory,
      Predicate<Throwable> retryOn) {
    if (threads <= 0) {
      throw new IllegalArgumentException("threads must be > 0");
    }
    if (maxRetriesPerThread < 0) {
      throw new IllegalArgumentException("maxRetriesPerThread must be >= 0");
    }
    Objects.requireNonNull(timeout, "timeout");
    Objects.requireNonNull(callFactory, "callFactory");
    Objects.requireNonNull(retryOn, "retryOn");

    CyclicBarrier barrier = new CyclicBarrier(threads);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    List<Future<Outcome<T>>> futures = new ArrayList<>(threads);
    for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
      int idx = threadIndex;
      futures.add(
          executor.submit(() -> runOne(idx, barrier, maxRetriesPerThread, callFactory, retryOn)));
    }

    List<Outcome<T>> outcomes = new ArrayList<>(threads);
    try {
      long timeoutMs = Math.max(1, timeout.toMillis());
      long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
      for (Future<Outcome<T>> future : futures) {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0) {
          throw new TimeoutException("Timed out waiting for concurrency run results");
        }
        outcomes.add(future.get(remainingNanos, TimeUnit.NANOSECONDS));
      }
      return new RunResult<>(outcomes);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while waiting for concurrency run", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Concurrency run failed", e.getCause());
    } catch (TimeoutException e) {
      futures.forEach(f -> f.cancel(true));
      throw new RuntimeException("Concurrency run timed out after " + timeout, e);
    } finally {
      executor.shutdownNow();
    }
  }

  private static <T> Outcome<T> runOne(
      int threadIndex,
      CyclicBarrier barrier,
      int maxRetriesPerThread,
      IntFunction<Callable<T>> callFactory,
      Predicate<Throwable> retryOn) {
    try {
      barrier.await(10, TimeUnit.SECONDS);
    } catch (Exception e) {
      return Outcome.failure(threadIndex, 0, e);
    }

    int attempts = 0;
    while (true) {
      attempts++;
      try {
        Callable<T> callable = callFactory.apply(threadIndex);
        if (callable == null) {
          throw new IllegalStateException("callFactory returned null");
        }
        return Outcome.success(threadIndex, attempts, callable.call());
      } catch (Throwable t) {
        if (attempts <= (maxRetriesPerThread + 1) && retryOn.test(t)) {
          continue;
        }
        return Outcome.failure(threadIndex, attempts, t);
      }
    }
  }

  public record RunResult<T>(List<Outcome<T>> outcomes) {
    public RunResult {
      outcomes = List.copyOf(outcomes);
    }
  }

  public sealed interface Outcome<T> permits Outcome.Success, Outcome.Failure {
    int threadIndex();

    int attempts();

    record Success<T>(int threadIndex, int attempts, T value) implements Outcome<T> {}

    record Failure<T>(int threadIndex, int attempts, Throwable error) implements Outcome<T> {}

    static <T> Success<T> success(int threadIndex, int attempts, T value) {
      return new Success<>(threadIndex, attempts, value);
    }

    static <T> Failure<T> failure(int threadIndex, int attempts, Throwable error) {
      return new Failure<>(threadIndex, attempts, error);
    }
  }
}
