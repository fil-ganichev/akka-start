package org.lokrusta.samples.akka.perfomancetest;

import org.apache.commons.lang3.RandomUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleThreadPool {

    private static final int POOL_SIZE = 8;
    private static final int POWER_BASE = 1000000;
    private static final int LOG_STEP = 10;
    private static final int MAX_TASKS = 1024 * 1024;
    private ExecutorService executorService = Executors.newFixedThreadPool(POOL_SIZE);
    private AtomicLong counter = new AtomicLong();
    private long startTime;

    public static void main(String[] args) {
        SimpleThreadPool simpleThreadPool = new SimpleThreadPool();
        //simpleThreadPool.actionOneThread();
        simpleThreadPool.action();
    }

    protected void actionOneThread() {
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TASKS; i++) {
            work2(POWER_BASE);
        }
    }
    
    protected void action() {
        startTime = System.currentTimeMillis();
        for (int i = 0; i < MAX_TASKS; i++) {
            executorService.execute(() -> work2(POWER_BASE));
        }
    }

    protected void work(int power) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < power; i++) {
            long result = factorial(RandomUtils.nextInt(1, 25));
        }
        long end = System.currentTimeMillis();
        if (counter.incrementAndGet() % LOG_STEP == 0) {
            System.out.printf("%d Задач выполнено за %d секунд%n", counter.longValue(), (long) ((System.currentTimeMillis() - startTime) / 1000));
        }
        //System.out.printf("Время выполнения: %.3f%n", ((double) (end - start) / 1000));
    }

    protected void work2(int power) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < power; i++) {
            long result = factorial(20);
        }
        long end = System.currentTimeMillis();
        if (counter.incrementAndGet() % LOG_STEP == 0) {
            System.out.printf("%d Задач выполнено за %d секунд%n", counter.longValue(), (long) ((System.currentTimeMillis() - startTime) / 1000));
        }
        //System.out.printf("Время выполнения: %.3f%n", ((double) (end - start) / 1000));
    }


    protected void work3(int power) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < power; i++) {
            long result = factorial(ThreadLocalRandom.current().nextInt(1, 25));
        }
        long end = System.currentTimeMillis();
        if (counter.incrementAndGet() % LOG_STEP == 0) {
            System.out.printf("%d Задач выполнено за %d секунд%n", counter.longValue(), (long) ((System.currentTimeMillis() - startTime) / 1000));
        }
        //System.out.printf("Время выполнения: %.3f%n", ((double) (end - start) / 1000));
    }

    private long factorial(int power) {
        if (power == 1) return power;
        else return power * factorial(power - 1);
    }
}
