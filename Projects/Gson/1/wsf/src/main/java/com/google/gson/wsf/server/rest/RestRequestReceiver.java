/*
 * Copyright (C) 2008 Google Inc.
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
package com.google.gson.wsf.server.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.webservice.definition.HeaderMap;
import com.google.gson.webservice.definition.HeaderMapSpec;
import com.google.gson.webservice.definition.HttpMethod;
import com.google.gson.webservice.definition.WebServiceSystemException;
import com.google.gson.webservice.definition.rest.RestRequest;
import com.google.gson.webservice.definition.rest.RestRequestSpec;
import com.google.gson.webservice.definition.rest.RestResource;

/**
 * Receives and parses a request at the server side on a {@link HttpServletRequest}.  
 * 
 * @author inder
 */
public final class RestRequestReceiver<R extends RestResource<R>> {

  private final Gson gson;
  private final RestRequestSpec spec;

  public RestRequestReceiver(Gson gson, RestRequestSpec spec) {
    this.gson = gson;
    this.spec = spec;
  }
  
  public RestRequest<R> receive(HttpServletRequest request) {
    try {
      HeaderMap requestParams = buildRequestParams(request);
      R requestBody = buildRequestBody(request);
      
      HttpMethod method = HttpMethod.getMethod(request.getMethod());
      return new RestRequest<R>(method, requestParams, requestBody, spec.getResourceType());
    } catch (IOException e) {
      throw new WebServiceSystemException(e);
    } catch (JsonParseException e) {
      // Not a Web service request
      throw new WebServiceSystemException(e);
    }
  }
  
  private HeaderMap buildRequestParams(HttpServletRequest request) {
    HeaderMapSpec paramsSpec = this.spec.getHeadersSpec();
    HeaderMap.Builder paramsBuilder = new HeaderMap.Builder(paramsSpec);
    for (Map.Entry<String, Type> param : paramsSpec.entrySet()) {
      String name = param.getKey();
      Type type = param.getValue();
      String header = request.getHeader(name);
      if (header == null || header.equals("")) {
        // check parameter map for the value
        header = request.getParameter(name);
      }
      if (header != null && !header.equals("")) { 
        Object value = gson.fromJson(header, type);
        paramsBuilder.put(name, value);
      }
    }
    return paramsBuilder.build();
  }
  
  private R buildRequestBody(HttpServletRequest request) throws IOException {
    Reader reader = new BufferedReader(new InputStreamReader(request.getInputStream()));
    R requestBody = gson.fromJson(reader, spec.getResourceType());
    return requestBody;
  }
}
