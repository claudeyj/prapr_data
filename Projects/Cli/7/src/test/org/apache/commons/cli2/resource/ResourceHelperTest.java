/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.cli2.resource;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import junit.framework.TestCase;

/**
 * A utility class used to provide internationalisation support.
 *
 * @author John Keyes
 */
public class ResourceHelperTest extends TestCase {
    /** system property */
    private static final String PROP_LOCALE = "org.apache.commons.cli2.resource.bundle";

    private static ResourceHelper helper;

    /** resource bundle */
    private ResourceBundle bundle;

    public void setUp() {
    	System.setProperty(PROP_LOCALE, "org.apache.commons.cli2.resource.TestBundle");
    	helper = ResourceHelper.getResourceHelper();
    }
    
    public void tearDown() {
    	System.setProperty(PROP_LOCALE, "org.apache.commons.cli2.resource.CLIMessageBundle_en_US.properties");
    }
    
    /**
     * Create a new ResourceHelper for the specified class.
     *
     * @param clazz the Class that requires some resources
     */
    public ResourceHelperTest() {
    	super("ResourceHelperTest");
    }
    
    public void testOverridden() {
    	assertEquals("wrong message", "The class name \"ResourceHelper\" is invalid.", helper.getMessage("ClassValidator.bad.classname", "ResourceHelper"));
    }
    
    public void testNewMessage1Param() {
    	assertEquals("wrong message", "Some might say we will find a brighter day.", helper.getMessage("test.message"));
    }

    public void testNewMessage2Params() {
    	assertEquals("wrong message", "Some might say we will find a brighter day.", helper.getMessage("test.message", "Some"));
    }

    public void testNewMessage3Params() {
    	assertEquals("wrong message", "Some might say we will find a brighter day.", helper.getMessage("test.message", "Some", "might"));
    }

    public void testNewMessage4Params() {
    	assertEquals("wrong message", "Some might say we will find a brighter day.", helper.getMessage("test.message", "Some", "might", "say"));
    }

    public void testDefaultBundle() {
    	System.setProperty(PROP_LOCALE, "madeupname.properties");
    	helper = ResourceHelper.getResourceHelper();
    	assertEquals("wrong message", "The class name \"ResourceHelper\" is invalid.", helper.getMessage("ClassValidator.bad.classname", "ResourceHelper"));
    }
}
