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
package com.google.gson.wsf.inject;

import com.google.gson.webservice.definition.RequestBodySpec;
import com.google.gson.webservice.definition.RequestSpec;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Guice provider for the {@link RequestBodySpec} to map to the incoming requests.
 * 
 * @author inder
 */
public final class RequestBodySpecProvider implements Provider<RequestBodySpec> {

  private final RequestSpec requestSpec;

  @Inject
  public RequestBodySpecProvider(RequestSpec requestSpec) {
    this.requestSpec = requestSpec;
  }
  
  @Override
  public RequestBodySpec get() {
    return requestSpec.getBodySpec();
  }
}
