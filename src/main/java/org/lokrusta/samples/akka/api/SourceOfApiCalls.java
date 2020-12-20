package org.lokrusta.samples.akka.api;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SourceOfApiCalls {

    private final ArrayBlockingQueue<String> blockingQueue = new ArrayBlockingQueue<>(10);
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private final AtomicLong value = new AtomicLong();
    private static final String EMPTY = "EMPTY";

    Source<String, NotUsed> of(ConcurrentLinkedQueue<String> queue) {
        Source<String, NotUsed> source = Source.repeat("").map(s -> {
            String call = queue.poll();
            return call == null ? EMPTY : call;
        });
        return source;
    }

    Source<String, NotUsed> of(ArrayBlockingQueue<String> blockingQueue) {
        Source<String, NotUsed> source = Source.repeat("").map(s -> blockingQueue.take());
        return source;
    }

    void startCalls() {
        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                queue.add(String.format("Some string %d", value.incrementAndGet()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void startBlockingCalls() {
        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                blockingQueue.add(String.format("Some string %d", value.incrementAndGet()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void startSource() {
        Source<String, NotUsed> source = of(queue);
        ActorSystem system = ActorSystem.create("QuickStart");
        source.to(Sink.foreach(call -> {
            if (!EMPTY.equals(call)) {
                System.out.println(call);
            }
        })).run(system);
    }

    void startBlockingSource() {
        Source<String, NotUsed> source = of(blockingQueue);
        ActorSystem system = ActorSystem.create("QuickStart");
        source.to(Sink.foreach(call -> {
            if (!EMPTY.equals(call)) {
                System.out.println(call);
            }
        })).run(system);
    }

    public static void main(String[] args) throws Exception {
        SourceOfApiCalls sourceOfApiCalls = new SourceOfApiCalls();
        //sourceOfApiCalls.startCalls();
        //sourceOfApiCalls.startSource();
        sourceOfApiCalls.startBlockingCalls();
        sourceOfApiCalls.startBlockingSource();
        System.in.read();
    }
}
