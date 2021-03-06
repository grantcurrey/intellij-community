/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.util.containers;

import com.intellij.util.ReflectionCache;
import gnu.trove.THashMap;

import java.util.Collection;
import java.util.Map;

/**
 * @author peter
 */
public class ClassMap<T> {
  protected final Map<Class, T> myMap;

  public ClassMap() {
    this(new THashMap<Class, T>());
  }
  protected ClassMap(Map<Class, T> map) {
    myMap = map; 
  }

  public void put(Class aClass, T value) {
    myMap.put(aClass, value);
  }
  public void remove(Class aClass) {
    myMap.remove(aClass);
  }

  public T get(Class aClass) {
    T t = myMap.get(aClass);
    if (t != null) {
      return t;
    }
    for (final Class aClass1 : ReflectionCache.getInterfaces(aClass)) {
      t = get(aClass1);
      if (t != null) {
        myMap.put(aClass, t);
        return t;
      }
    }
    final Class superclass = ReflectionCache.getSuperClass(aClass);
    if (superclass != null) {
      t = get(superclass);
      if (t != null) {
        myMap.put(aClass, t);
        return t;
      }
    }
    return null;
  }

  public final Collection<T> values() {
    return myMap.values();
  }

  public void clear() {
    myMap.clear();
  }
}
