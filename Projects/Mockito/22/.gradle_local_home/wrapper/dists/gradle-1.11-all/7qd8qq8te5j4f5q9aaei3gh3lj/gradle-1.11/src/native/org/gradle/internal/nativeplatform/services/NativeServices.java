/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.internal.nativeplatform.services;

import com.sun.jna.Native;
import net.rubygrapefruit.platform.*;
import net.rubygrapefruit.platform.Process;
import net.rubygrapefruit.platform.internal.DefaultProcessLauncher;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.nativeplatform.ProcessEnvironment;
import org.gradle.internal.nativeplatform.console.ConsoleDetector;
import org.gradle.internal.nativeplatform.console.NativePlatformConsoleDetector;
import org.gradle.internal.nativeplatform.console.NoOpConsoleDetector;
import org.gradle.internal.nativeplatform.console.WindowsConsoleDetector;
import org.gradle.internal.nativeplatform.filesystem.FileSystem;
import org.gradle.internal.nativeplatform.filesystem.FileSystems;
import org.gradle.internal.nativeplatform.jna.*;
import org.gradle.internal.nativeplatform.processenvironment.NativePlatformBackedProcessEnvironment;
import org.gradle.internal.os.OperatingSystem;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Provides various native platform integration services.
 */
public class NativeServices extends DefaultServiceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(NativeServices.class);
    private static boolean useNativePlatform = "true".equalsIgnoreCase(System.getProperty("org.gradle.native", "true"));
    private static final NativeServices INSTANCE = new NativeServices();

    /**
     * Initializes the native services to use the given user home directory to store native libs and other resources. Does nothing if already initialized. Will be implicitly initialized on first usage
     * of a native service. Also initializes the Native-Platform library using the given user home directory.
     */
    public static void initialize(File userHomeDir) {
        File nativeDir = new File(userHomeDir, "native");
        if (useNativePlatform) {
            try {
                net.rubygrapefruit.platform.Native.init(nativeDir);
            } catch (NativeIntegrationUnavailableException ex) {
                LOGGER.debug("Native-platform is not available.");
                useNativePlatform = false;
            } catch (NativeException ex) {
                LOGGER.debug("Unable to initialize native-platform. Failure: {}", format(ex));
                useNativePlatform = false;
            }
        }
        new JnaBootPathConfigurer().configure(nativeDir);
    }

    public static NativeServices getInstance() {
        return INSTANCE;
    }

    private NativeServices() {
    }

    @Override
    public void close() {
        // Don't close
    }

    protected OperatingSystem createOperatingSystem() {
        return OperatingSystem.current();
    }

    protected FileSystem createFileSystem() {
        return FileSystems.getDefault();
    }

    protected Jvm createJvm() {
        return Jvm.current();
    }

    protected ProcessEnvironment createProcessEnvironment(OperatingSystem operatingSystem) {
        if (useNativePlatform) {
            try {
                net.rubygrapefruit.platform.Process process = net.rubygrapefruit.platform.Native.get(Process.class);
                return new NativePlatformBackedProcessEnvironment(process);
            } catch (NativeIntegrationUnavailableException ex) {
                LOGGER.debug("Native-platform process integration is not available. Continuing with fallback.");
            } catch (NativeException ex) {
                LOGGER.debug("Unable to load from native-platform backed ProcessEnvironment. Continuing with fallback. Failure: {}", format(ex));
            }
        }

        try {
            if (operatingSystem.isUnix()) {
                return new LibCBackedProcessEnvironment(get(LibC.class));
            } else {
                return new UnsupportedEnvironment();
            }
        } catch (LinkageError e) {
            // Thrown when jna cannot initialize the native stuff
            LOGGER.debug("Unable to load native library. Continuing with fallback. Failure: {}", format(e));
            return new UnsupportedEnvironment();
        }
    }

    protected ConsoleDetector createConsoleDetector(OperatingSystem operatingSystem) {
        if (useNativePlatform) {
            try {
                Terminals terminals = net.rubygrapefruit.platform.Native.get(Terminals.class);
                return new NativePlatformConsoleDetector(terminals);
            } catch (NativeIntegrationUnavailableException ex) {
                LOGGER.debug("Native-platform terminal integration is not available. Continuing with fallback.");
            } catch (NativeException ex) {
                LOGGER.debug("Unable to load from native-platform backed ConsoleDetector. Continuing with fallback. Failure: {}", format(ex));
            }
        }

        try {
            if (operatingSystem.isWindows()) {
                return new WindowsConsoleDetector();
            }
            return new LibCBackedConsoleDetector(get(LibC.class));
        } catch (LinkageError e) {
            // Thrown when jna cannot initialize the native stuff
            LOGGER.debug("Unable to load native library. Continuing with fallback. Failure: {}", format(e));
            return new NoOpConsoleDetector();
        }
    }

    protected WindowsRegistry createWindowsRegistry(OperatingSystem operatingSystem) {
        if (operatingSystem.isWindows()) {
            return net.rubygrapefruit.platform.Native.get(WindowsRegistry.class);
        }
        return notAvailable(WindowsRegistry.class);
    }

    protected SystemInfo createSystemInfo() {
        try {
            return net.rubygrapefruit.platform.Native.get(SystemInfo.class);
        } catch (NativeIntegrationUnavailableException e) {
            LOGGER.debug("Native-platform system info is not available. Continuing with fallback.");
        }
        return notAvailable(SystemInfo.class);
    }

    protected ProcessLauncher createProcessLauncher() {
        try {
            return net.rubygrapefruit.platform.Native.get(ProcessLauncher.class);
        } catch (NativeIntegrationUnavailableException e) {
            LOGGER.debug("Native-platform process launcher is not available. Continuing with fallback.");
        }
        return new DefaultProcessLauncher();
    }

    private <T> T notAvailable(Class<T> type) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new BrokenService(type.getSimpleName()));
    }

    protected LibC createLibC() {
        return (LibC) Native.loadLibrary("c", LibC.class);
    }

    private static String format(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        builder.append(throwable.toString());
        for (Throwable current = throwable.getCause(); current != null; current = current.getCause()) {
            builder.append(SystemProperties.getLineSeparator());
            builder.append("caused by: ");
            builder.append(current.toString());
        }
        return builder.toString();
    }

    private static class BrokenService implements InvocationHandler {
        private final String type;

        private BrokenService(String type) {
            this.type = type;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return new org.gradle.internal.nativeplatform.NativeIntegrationUnavailableException(String.format("%s is not supported on this operating system.", type));
        }
    }
}
