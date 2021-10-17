/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockitousage.misuse;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.exceptions.misusing.MissingMethodInvocationException;
import org.mockitousage.IMethods;
import org.mockitoutil.TestBase;

public class CleaningUpPotentialStubbingTest extends TestBase {

    @Mock private IMethods mock;
    
    @Test
    public void shouldResetOngoingStubbingOnVerify() {
        // first test
        mock.booleanReturningMethod();
        verify(mock).booleanReturningMethod();
        
        // second test
        assertOngoingStubbingIsReset();
    }
    
    @Test
    public void shouldResetOngoingStubbingOnMock() {
        mock.booleanReturningMethod();
        mock(IMethods.class);
        assertOngoingStubbingIsReset();
    }
    
    @Test
    public void shouldResetOngoingStubbingOnInOrder() {
        mock.booleanReturningMethod();
        InOrder inOrder = inOrder(mock);
        inOrder.verify(mock).booleanReturningMethod();
        assertOngoingStubbingIsReset();
    }
    
    @Test
    public void shouldResetOngoingStubbingOnDoReturn() {
        mock.booleanReturningMethod();
        doReturn(false).when(mock).booleanReturningMethod();
        assertOngoingStubbingIsReset();
    }

    @Test
    public void shouldResetOngoingStubbingOnVerifyNoMoreInteractions() {
        mock.booleanReturningMethod();
        IMethods mock2 = mock(IMethods.class);
        verifyNoMoreInteractions(mock2);
        assertOngoingStubbingIsReset();
    }

    private void assertOngoingStubbingIsReset() {
        try {
            //In real, there might be a call to real object or a final method call
            //I'm modelling it with null
            when(null).thenReturn("anything");
            fail();
        } catch (MissingMethodInvocationException e) {}
    }
}