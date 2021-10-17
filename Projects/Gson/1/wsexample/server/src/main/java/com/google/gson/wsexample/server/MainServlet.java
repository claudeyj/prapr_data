/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gson.wsexample.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.example.model.Cart;
import com.google.gson.example.model.Order;
import com.google.gson.example.model.TypedKeys;
import com.google.gson.example.service.SampleJsonService;
import com.google.gson.webservice.definition.HeaderMap;
import com.google.gson.webservice.definition.RequestBody;
import com.google.gson.webservice.definition.RequestSpec;
import com.google.gson.webservice.definition.ResponseBody;
import com.google.gson.webservice.definition.ResponseSpec;
import com.google.gson.webservice.definition.WebServiceCallSpec;
import com.google.gson.webservice.definition.WebServiceRequest;
import com.google.gson.webservice.definition.WebServiceResponse;
import com.google.gson.webservice.typeadapters.RequestBodyGsonConverter;
import com.google.gson.webservice.typeadapters.ResponseBodyGsonConverter;
import com.google.gson.wsf.server.RequestReceiver;
import com.google.gson.wsf.server.ResponseSender;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * An example servlet that receives JSON web-service requests
 *
 * @author inder
 */
@SuppressWarnings("serial")
public class MainServlet extends HttpServlet {
  @Override
  public void service(HttpServletRequest req, HttpServletResponse res) {
    WebServiceCallSpec spec = SampleJsonService.PLACE_ORDER;
    RequestSpec requestSpec = spec.getRequestSpec();
    ResponseSpec responseSpec = spec.getResponseSpec();
    Gson gson = new GsonBuilder()
        .registerTypeAdapter(RequestBody.class,
            new RequestBodyGsonConverter(requestSpec.getBodySpec()))
        .registerTypeAdapter(ResponseBody.class, 
            new ResponseBodyGsonConverter(responseSpec.getBodySpec()))
        .create();
    RequestReceiver requestReceiver = new RequestReceiver(gson, requestSpec);
    WebServiceRequest webServiceRequest = requestReceiver.receive(req);

    Cart cart = webServiceRequest.getBody().get(TypedKeys.RequestBody.CART);
    String authToken = webServiceRequest.getHeader(TypedKeys.Request.AUTH_TOKEN);

    Order order = placeOrder(cart, authToken);

    // Empty headers per the spec
    HeaderMap responseHeaders = new HeaderMap.Builder(responseSpec.getHeadersSpec()).build();
    ResponseBody responseBody = new ResponseBody.Builder(responseSpec.getBodySpec())
        .put(TypedKeys.ResponseBody.ORDER, order)
        .build();
    WebServiceResponse response = new WebServiceResponse(responseHeaders, responseBody);
    ResponseSender responseSender = new ResponseSender(gson);
    responseSender.send(res, response);
  }
  
  private Order placeOrder(Cart cart, String authToken) {
    // Create an order, in this case a dummy one.
    return new Order(cart, "Order123");
  }
}
