/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.stubbing;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.mockito.internal.invocation.InvocationMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SuppressWarnings("unchecked")
public class StubbedInvocationMatcher extends InvocationMatcher implements Answer {

    private final Queue<Answer> answers = new ConcurrentLinkedQueue<Answer>();

    public StubbedInvocationMatcher(InvocationMatcher invocation, Answer answer) {
        super(invocation.getInvocation(), invocation.getMatchers());
        this.answers.add(answer);
    }

    public Object answer(InvocationOnMock invocation) throws Throwable {
        //see ThreadsShareGenerouslyStubbedMockTest
        synchronized(answers) {
            return answers.size() == 1 ? answers.peek().answer(invocation) : answers.poll().answer(invocation);
        }
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    @Override
    public String toString() {
        return super.toString() + " stubbed with: " + answers;
    }
}