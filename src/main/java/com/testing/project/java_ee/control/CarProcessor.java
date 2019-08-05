package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.Car;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import java.util.concurrent.locks.LockSupport;

@Stateless
@Asynchronous
public class CarProcessor {

    @Asynchronous
    public void processNewCarAsync(Car car) {
        LockSupport.parkNanos(20000000000L);
        String result = "processed: " + car;
        System.out.println(result);

    }

    @Asynchronous
    public void processNewCar(Car car) {
        LockSupport.parkNanos(20000000000L);
        String result = "processed: " + car;
        System.out.println(result);

    }

}
