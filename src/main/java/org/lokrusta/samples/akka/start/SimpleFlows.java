package org.lokrusta.samples.akka.start;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class SimpleFlows {

    private List<Integer> parseLine(String line) {
        String[] fields = line.split(";");
        return Arrays.stream(fields)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private Flow<String, Integer, NotUsed> parseContent() {
        return Flow.of(String.class)
                .mapConcat(this::parseLine);
    }

    private Flow<Integer, Double, NotUsed> computeAverage() {
        return Flow.of(Integer.class)
                .grouped(2)
                .mapAsyncUnordered(8, integers ->
                        CompletableFuture.supplyAsync(() -> integers.stream()
                                .mapToDouble(v -> v)
                                .average()
                                .orElse(-1.0)));
    }

    Flow<String, Double, NotUsed> calculateAverage() {
        return Flow.of(String.class)
                .via(parseContent())
                .via(computeAverage());
    }

    CompletionStage<Done> calculateAverageForContent(String content) {
        return Source.single(content)
                .via(calculateAverage())
                .runWith(storeAverages(), ActorMaterializer.create(ActorSystem.create("QuickStart")))
                .whenComplete((d, e) -> {
                    if (d != null) {
                        System.out.println("Import finished ");
                    } else {
                        e.printStackTrace();
                    }
                });
    }

    private Sink<Double, CompletionStage<Done>> storeAverages() {
        return Flow.of(Double.class)
                .mapAsyncUnordered(4, SimpleFlows::save)
                .toMat(Sink.ignore(), Keep.right());
    }

    static CompletionStage<Double> save(Double average) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println(average);
            return average;
        });
    }

    public static void main(String[] args) {
        new SimpleFlows().parseLine("3526;67;80;99;23").forEach(System.out::println);
        System.out.println("---next step----");
        new SimpleFlows().calculateAverageForContent("3526;67;80;99;23");
    }
}
