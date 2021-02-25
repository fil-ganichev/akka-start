package juddy.transport.api;

import juddy.transport.api.common.Api;
import juddy.transport.impl.serializer.dto.Car;
import juddy.transport.impl.serializer.dto.Driver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Api
public interface TestApiCars {

    CompletableFuture<Void> registerCar(Car car, List<Driver> drivers);
}
