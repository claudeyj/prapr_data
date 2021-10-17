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
package com.google.gson.webservice.definition.rest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * An id for a rest resource
 *
 * @author inder
 *
 * @param <R> type variable for the rest resource
 */
public final class Id<R> {
  private static final long NULL_VALUE = -1;
  private final long value;
  private final Type typeOfId;

  private Id(long value, Type typeOfId) {
    Preconditions.checkArgument(value != NULL_VALUE);
    this.value = value;
    this.typeOfId = typeOfId;
  }

  public long getValue() {
    return value;
  }

  public static long getValue(Id<?> id) {
    return id == null ? NULL_VALUE : id.getValue();
  }
  public Type getTypeOfId() {
    return typeOfId;
  }

  @Override
  public int hashCode() {
    return (int) value;
  }

  public static boolean isValid(Id<?> id) {
    return id != null && id.value != NULL_VALUE;
  }

  /**
   * A more efficient comparison method for ids that take into account of ids being nullable.
   * Since the method is parameterized and both ids are of the same type, this method compares
   * only id values, not their types. Note that this shortcut doesn't work if you pass raw ids
   * to this method
   */
  public static <T> boolean equals(/* @Nullable */ Id<T> id1, /* @Nullable */ Id<T> id2) {
    if ((id1 == null && id2 != null) || (id1 != null && id2 == null)) {
      return false;
    }
    if (id1 == null && id2 == null) {
      return true;
    }
    return id1.value == id2.value;
  }

  @Override  
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    @SuppressWarnings("unchecked")
    Id<R> other = (Id<R>)obj;
    if (typeOfId == null) {
      if (other.typeOfId != null) return false;
    } else if (!equivalentTypes(typeOfId, other.typeOfId)) return false;
    if (value != other.value) return false;
    return true;
  }

  /**
   * Returns true for equivalentTypes(Class<?>, Class)
   * Visible for testing only 
   */
  @SuppressWarnings("rawtypes")
  static boolean equivalentTypes(Type type1, Type type2) {
    if (type1 instanceof ParameterizedType && type2 instanceof Class) {
      return areEquivalentTypes((ParameterizedType)type1, (Class)type2);
    } else if (type2 instanceof ParameterizedType && type1 instanceof Class) {
      return areEquivalentTypes((ParameterizedType)type2, (Class)type1);
    }
    return type1.equals(type2);
  }

  /**
   * Visible for testing only
   */
  @SuppressWarnings("rawtypes")
  static boolean areEquivalentTypes(ParameterizedType type, Class clazz) {
    Class rawClass = (Class) type.getRawType();
    if (!clazz.equals(rawClass)) {
      return false;
    }
    for (Type typeVariable : type.getActualTypeArguments()) {
      if (typeVariable instanceof WildcardType) {
        continue;
      }
      // This is a real parameterized type, not just ?
      return false;
    }
    return true;
  }

  public static <RS> Id<RS> get(long value, Type typeOfId) {
    return new Id<RS>(value, typeOfId);
  }

  @Override
  public String toString() {
    String typeAsString = getSimpleTypeName(typeOfId);
    return String.format("{value:%s,type:%s}", value, typeAsString);
  }

  @SuppressWarnings("rawtypes")
  private static String getSimpleTypeName(Type type) {
    if (type == null) {
      return "null";
    }
    if (type instanceof Class) {
      return ((Class)type).getSimpleName();
    } else if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      StringBuilder sb = new StringBuilder(getSimpleTypeName(pType.getRawType()));
      sb.append('<');
      boolean first = true;
      for (Type argumentType : pType.getActualTypeArguments()) {
        if (first) {
          first = false;
        } else {
          sb.append(',');
        }
        sb.append(getSimpleTypeName(argumentType));
      }
      sb.append('>');
      return sb.toString();
    } else if (type instanceof WildcardType) {
      return "?";
    }
    return type.toString();
  }
}
