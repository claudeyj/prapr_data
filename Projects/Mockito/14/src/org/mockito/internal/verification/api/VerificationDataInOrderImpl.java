package org.mockito.internal.verification.api;

import java.util.List;

import org.mockito.internal.invocation.Invocation;
import org.mockito.internal.invocation.InvocationMatcher;

public class VerificationDataInOrderImpl implements VerificationDataInOrder {

    private final InOrderContext inOrder;
    private final List<Invocation> allInvocations;
    private final InvocationMatcher wanted;

    public VerificationDataInOrderImpl(InOrderContext inOrder, List<Invocation> allInvocations, InvocationMatcher wanted) {
        this.inOrder = inOrder;
        this.allInvocations = allInvocations;
        this.wanted = wanted;        
    }

    @Override
    public List<Invocation> getAllInvocations() {
        return allInvocations;
    }

    @Override
    public InOrderContext getOrderingContext() {
        return inOrder;
    }

    @Override
    public InvocationMatcher getWanted() {
        return wanted;
    }

}
