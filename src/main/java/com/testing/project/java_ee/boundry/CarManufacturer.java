package com.testing.project.java_ee.boundry;

import com.testing.project.java_ee.control.CarFactory;
import com.testing.project.java_ee.control.CarProcessor;
import com.testing.project.java_ee.control.CarRepository;
import com.testing.project.java_ee.entity.Car;
import com.testing.project.java_ee.entity.CarCreated;
import com.testing.project.java_ee.entity.Specification;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;


public class CarManufacturer {

    @Inject
    CarFactory carFactory;

    @Inject
    CarRepository carRepository;

    @Inject
    CarProcessor carProcessor;

    @Inject
    Event<CarCreated> carCreated;

    @PersistenceContext
    EntityManager entityManager;

    public Car manufactureCar(Specification specification){
        Car car = carFactory.createCar(specification);
       // carRepository.store(car);
        //carProcessor.processNewCar(car);
      //  carCreated.fire(new CarCreated(car.getIdentifier()));
        entityManager.persist(car);
        return car;
    }

    public List<Car> retrieveCars(){
        //return carRepository.loadCars();
        return entityManager.createNamedQuery(Car.FIND_ALL,Car.class).getResultList();
    }
}
