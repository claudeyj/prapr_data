/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.junitrunner;

import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnit44Runner;
import org.mockitoutil.TestBase;

@RunWith(MockitoJUnit44Runner.class)
@SuppressWarnings({"unchecked", "deprecation"})
public class JUnit44RunnerTest extends TestBase {
    
    @Mock private List list;
    
    @Test
    public void shouldInitMocksUsingRunner() {
        list.add("test");
        verify(list).add("test");
    }
}