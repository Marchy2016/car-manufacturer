package com.testing.project.java_ee.entity;

import javax.persistence.*;

@Entity
@Table(name = "car_created")
public class CarCreated {

    @Id
    private  String identifier;

    @Enumerated(EnumType.STRING)
    private  Color color;

    @Enumerated(EnumType.STRING)
    private  EngineType engineType;


    public CarCreated(String identifier) {
        this.identifier = identifier;
    }

    public CarCreated(String identifier, Color color, EngineType engineType) {
        this.identifier = identifier;
        this.color = color;
        this.engineType = engineType;
    }

    public CarCreated() {
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public EngineType getEngineType() {
        return engineType;
    }

    public void setEngineType(EngineType engineType) {
        this.engineType = engineType;
    }

}
