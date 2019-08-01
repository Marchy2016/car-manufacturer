package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.CarCreated;

import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;
import java.util.concurrent.locks.LockSupport;

@Asynchronous
@Stateless
public class CarCreationListener {

    public  void onCarCreation(@Observes CarCreated carCreated){

        LockSupport.parkNanos(20000000000L);
        System.out.println("new car created with id : " + carCreated.getIdentifier());

    }
}
