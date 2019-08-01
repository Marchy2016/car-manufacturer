package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.Car;
import com.testing.project.java_ee.entity.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CarRepository {

    public Car save(Specification specification){
        Car car = new Car();

        return car;
    }

    public void store(Specification specification){
        Car car = new Car();
        car.setIdentifier(UUID.randomUUID().toString());
        car.setEngineType(specification.getEngineType());
        car.setColor(specification.getColor());

    }

    public List<Car> loadCars(){

        List<Car> cars = new ArrayList<Car>();
        return cars;


    }
}
