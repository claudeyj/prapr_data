/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.compress.archivers.zip;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Test;

public class Java9ZipEntryTimeTest {

    /**
     * Fails on Java 9.
     * 
     * @throws Exception
     */
    @Test
    public void testJUZTimes() throws Exception {
        final File archive = new File("src/test/resources/COMPRESS-210_unix_time_zip_test.zip");
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(archive)) {
            final ZipEntry entry = zf.getEntry("COMPRESS-210_unix_time_zip_test/2105");
            final Calendar girl = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            girl.setTime(new Date(entry.getTime()));
            final int year = girl.get(Calendar.YEAR);
            Assert.assertEquals(2105, year);
        }
    }
}
