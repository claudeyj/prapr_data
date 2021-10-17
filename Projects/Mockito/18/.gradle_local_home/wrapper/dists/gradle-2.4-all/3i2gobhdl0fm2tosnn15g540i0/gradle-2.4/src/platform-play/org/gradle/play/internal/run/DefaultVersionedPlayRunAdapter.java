/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.play.internal.run;

import org.gradle.internal.UncheckedException;
import org.gradle.internal.classpath.DefaultClassPath;
import org.gradle.scala.internal.reflect.ScalaMethod;
import org.gradle.scala.internal.reflect.ScalaReflectionUtil;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.jar.JarFile;

public abstract class DefaultVersionedPlayRunAdapter implements VersionedPlayRunAdapter, Serializable {

    protected abstract Class<?> getBuildLinkClass(ClassLoader classLoader) throws ClassNotFoundException;

    protected abstract Class<?> getDocHandlerFactoryClass(ClassLoader classLoader) throws ClassNotFoundException;

    protected abstract Class<?> getBuildDocHandlerClass(ClassLoader docsClassLoader) throws ClassNotFoundException;

    public Object getBuildLink(ClassLoader classLoader, final File projectPath, final Iterable<File> classpath) throws ClassNotFoundException {
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{getBuildLinkClass(classLoader)}, new InvocationHandler() {
            private volatile boolean shouldReloadNextTime = true;

            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("projectPath")) {
                    return projectPath;
                } else if (method.getName().equals("reload")) {
                    if (shouldReloadNextTime) {
                        shouldReloadNextTime = false; //reload only once for now
                        DefaultClassPath projectClasspath = new DefaultClassPath(classpath);
                        return new URLClassLoader(projectClasspath.getAsURLs().toArray(new URL[]{}), Thread.currentThread().getContextClassLoader());
                    } else {
                        return null;
                    }
                } else if (method.getName().equals("settings")) {
                    return new HashMap<String, String>();
                }
                //TODO: all methods
                return null;
            }
        });
    }

    public Object getBuildDocHandler(ClassLoader docsClassLoader, Iterable<File> classpath) throws NoSuchMethodException, ClassNotFoundException, IOException, IllegalAccessException {
        Class<?> docHandlerFactoryClass = getDocHandlerFactoryClass(docsClassLoader);
        Method docHandlerFactoryMethod = docHandlerFactoryClass.getMethod("fromJar", JarFile.class, String.class);
        JarFile documentationJar = findDocumentationJar(classpath);
        try {
            return docHandlerFactoryMethod.invoke(null, documentationJar, "play/docs/content");
        } catch (InvocationTargetException e) {
            throw UncheckedException.unwrapAndRethrow(e);
        }
    }

    private JarFile findDocumentationJar(Iterable<File> classpath) throws IOException {
        // TODO:DAZ Use the location of the DocHandlerFactoryClass instead.
        File docJarFile = null;
        for (File file : classpath) {
            if (file.getName().startsWith("play-docs")) {
                docJarFile = file;
                break;
            }
        }
        return new JarFile(docJarFile);
    }


    public ScalaMethod getNettyServerDevHttpMethod(ClassLoader classLoader, ClassLoader docsClassLoader) throws ClassNotFoundException {
        return ScalaReflectionUtil.scalaMethod(classLoader, "play.core.server.NettyServer", "mainDevHttpMode", getBuildLinkClass(classLoader), getBuildDocHandlerClass(docsClassLoader), int.class);
    }

}
