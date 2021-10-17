/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.runners;

import org.junit.internal.runners.InitializationError;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.runners.util.FrameworkUsageValidator;

@SuppressWarnings("deprecation")
public class JUnit44RunnerImpl implements RunnerImpl {

    JUnit4ClassRunner runner;

    public JUnit44RunnerImpl(Class<?> klass) throws InitializationError {
        this.runner = new JUnit4ClassRunner(klass) {
            @Override
            protected Object createTest() throws Exception {
                Object test = super.createTest();
                MockitoAnnotations.initMocks(test);
                return test;
            }
        };
    }

    public void run(RunNotifier notifier) {
        // add listener that validates framework usage at the end of each test
        notifier.addListener(new FrameworkUsageValidator(notifier));

        runner.run(notifier);
    }

    public Description getDescription() {
        return runner.getDescription();
    }
    public void filter(org.junit.runner.manipulation.Filter filter) throws org.junit.runner.manipulation.NoTestsRemainException {
        // filter is required because without it UnrootedTests show up in Eclipse
        runner.filter(filter);
    }
}
