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

package org.apache.commons.compress.archivers.tar;

import java.io.StringReader;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TarArchiveInputStreamTest {

    @Test
    public void readSimplePaxHeader() throws Exception {
        Map<String, String> headers = new TarArchiveInputStream(null)
            .parsePaxHeaders(new StringReader("30 atime=1321711775.972059463\n"));
        assertEquals(1, headers.size());
        assertEquals("1321711775.972059463", headers.get("atime"));
    }

    @Test
    public void readPaxHeaderWithEmbeddedNewline() throws Exception {
        Map<String, String> headers = new TarArchiveInputStream(null)
            .parsePaxHeaders(new StringReader("28 comment=line1\nline2\nand3\n"));
        assertEquals(1, headers.size());
        assertEquals("line1\nline2\nand3", headers.get("comment"));
    }
}