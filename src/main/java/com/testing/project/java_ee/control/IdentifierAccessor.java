package com.testing.project.java_ee.control;

import com.testing.project.java_ee.entity.Specification;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ApplicationScoped
public class IdentifierAccessor {

    private Client client;
    private WebTarget target;

    @PostConstruct
    private void initClient() {
        client = ClientBuilder.newBuilder()
        .connectTimeout(1, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build();
        target = client.target("https://cars.examples.com/cars/identification");
    }


    public String retrieveCarIdentification(Specification specification) {

        //create a json object to post and get the identifier
        JsonObject entity = buildRequestBody(specification);
        Response response = sendRequest(entity);
        return extractIdenfier(response);

    }

    public List<String> retrieveIdentifications(){

        Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        GenericType<List<CarIdentifier>> listType = new GenericType<List<CarIdentifier>>(){};
        return  response.readEntity(listType)
                .stream().map(CarIdentifier::getIdentifier)
                .collect(Collectors.toList());

       // target.path("{id}")
         //       .resolveTemplate("id","12354")

    }

    private Response sendRequest(JsonObject entity) {
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(entity));
    }

    private JsonObject buildRequestBody(Specification specification) {
        return Json.createObjectBuilder()
                .add("engine", specification.getEngineType().name())
                .build();

    }

    private String extractIdenfier(Response response) {
        return response.readEntity(JsonObject.class).getString("identifier");

    }

    @PreDestroy
    private void closeClient() {
        client.close();
    }

    private class CarIdentifier{
        private  String identifier;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
    }

}
