
package com.globocom.grou.groot.jbender.executors;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.strands.Strand;

public class SleepyRequestExecutor<Q> implements RequestExecutor<Q, Void> {
  private final int sleepMillis;
  private final int sleepNanos;

  public SleepyRequestExecutor(int sleepMillis, int sleepNanos) {
    this.sleepMillis = sleepMillis;
    this.sleepNanos = sleepNanos;
  }

  @Override
  public Void execute(final long nanoTime, final Q request) throws SuspendExecution, InterruptedException {
    Strand.sleep(sleepMillis, sleepNanos);
    return null;
  }
}
