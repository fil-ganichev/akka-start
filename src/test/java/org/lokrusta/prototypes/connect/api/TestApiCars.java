package org.lokrusta.prototypes.connect.api;

import org.lokrusta.prototypes.connect.impl.helper.dto.Car;
import org.lokrusta.prototypes.connect.impl.helper.dto.Driver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApiCars {

    CompletableFuture<Void> registerCar(Car car, List<Driver> drivers);
}
