package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.Car;
import com.testing.project.java_ee.entity.Specification;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.UUID;

public class CarFactory {

    @Inject
    CarRepository carRepository;

    @Transactional
    public Car createCar(Specification specification) {

        Car car = new Car();
        car.setIdentifier(UUID.randomUUID().toString());
        car.setColor(specification.getColor());
        car.setEngineType(specification.getEngineType());
        return car;


    }
}
