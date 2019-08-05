package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.Car;
import com.testing.project.java_ee.entity.Specification;


import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class CarFactory {

    @Inject
    CarRepository carRepository;

    @Resource
    ManagedScheduledExecutorService mses;

    //Retrieving data from external system using client
    @Inject
    IdentifierAccessor identifierAccessor;



    @Transactional
    public Car createCar(Specification specification) {

        Car car = new Car();
        // car.setIdentifier(UUID.randomUUID().toString());
        car.setIdentifier(identifierAccessor.retrieveCarIdentification(specification));
        car.setColor(specification.getColor());
        car.setEngineType(specification.getEngineType());
        return car;
    }

    public void activateTimer() {
        mses.scheduleAtFixedRate(this::doSomething, 60, 10, TimeUnit.SECONDS);

    }


    public void doSomething() {
        System.out.println("Print something");
    }
}
