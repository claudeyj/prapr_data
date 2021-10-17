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
package com.google.gson.webservice.typeadapters;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.webservice.definition.ResponseBody;
import com.google.gson.webservice.definition.ResponseBodySpec;

/**
 * Gson type adapter for {@link ResponseBody}. 
 * 
 * @author inder
 */
public final class ResponseBodyGsonConverter implements JsonSerializer<ResponseBody>, 
  JsonDeserializer<ResponseBody>, InstanceCreator<ResponseBody> {

  private final ResponseBodySpec spec;

  public ResponseBodyGsonConverter(ResponseBodySpec spec) {
    this.spec = spec;
  }
  
  @Override
  public JsonElement serialize(ResponseBody src, Type typeOfSrc, 
      JsonSerializationContext context) {
    JsonObject root = new JsonObject();
    for(Map.Entry<String, Object> entry : src.entrySet()) {
      String key = entry.getKey();
      Type entryType = src.getSpec().getTypeFor(key);
      JsonElement value = context.serialize(entry.getValue(), entryType);
      root.add(key, value);        
    }
    return root;
  }

  @Override
  public ResponseBody deserialize(JsonElement json, Type typeOfT, 
      JsonDeserializationContext context) throws JsonParseException {
    ResponseBody.Builder responseBodyBuilder = new ResponseBody.Builder(spec);
    for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
      String key = entry.getKey();
      Type entryType = spec.getTypeFor(key);
      Object value = context.deserialize(entry.getValue(), entryType);
      responseBodyBuilder.put(key, value, entryType);
    }
    return responseBodyBuilder.build();
  }

  @Override
  public ResponseBody createInstance(Type type) {
    return new ResponseBody.Builder(spec).build();
  }
}