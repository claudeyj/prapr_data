/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.cli2.validation;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.apache.commons.cli2.resource.ResourceConstants;
import org.apache.commons.cli2.resource.ResourceHelper;

public class EnumValidatorTest
    extends TestCase {
    private final static ResourceHelper resources = ResourceHelper.getResourceHelper();
    private final Set enumSet = new TreeSet(Arrays.asList(new Object[] { "red", "green", "blue" }));

    public void testValidate()
        throws InvalidArgumentException {
        final Object[] array = new Object[] { "red", "green" };

        {
            final List list = Arrays.asList(array);
            final EnumValidator validator = new EnumValidator(enumSet);
            assertEquals("valid values are incorrect", enumSet, validator.getValidValues());
            validator.validate(list);

            final Iterator i = list.iterator();
            assertEquals("red", i.next());
            assertEquals("green", i.next());
            assertFalse(i.hasNext());
        }
    }

    public void testNonMember() {
        final Object[] array = new Object[] { "red", "pink" };
        final List list = Arrays.asList(array);
        final EnumValidator validator = new EnumValidator(enumSet);

        try {
            validator.validate(list);
            fail("InvalidArgumentException");
        } catch (InvalidArgumentException e) {
            assertEquals(resources.getMessage(ResourceConstants.ENUM_ILLEGAL_VALUE,
                                              new Object[] { "pink", validator.getValuesAsString() }),
                         e.getMessage());
        }
    }
}
