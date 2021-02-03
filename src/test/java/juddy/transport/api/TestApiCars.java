package juddy.transport.api;

import juddy.transport.impl.helper.dto.Car;
import juddy.transport.impl.helper.dto.Driver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApiCars {

    CompletableFuture<Void> registerCar(Car car, List<Driver> drivers);
}
