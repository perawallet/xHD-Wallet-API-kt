/*
 * Copyright (c) Terl Tech Ltd • 01/04/2021, 12:31 • goterl.com
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/.
 */
package app.perawallet.lazysodium.utils;

import com.goterl.resourceloader.SharedLibraryLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import app.perawallet.lazysodium.Sodium;
import app.perawallet.lazysodium.SodiumJava;

/**
 * A simple library class which helps with loading dynamic sodium library stored in the
 * JAR archive. Works with JNA.
 *
 * <p>This class is thread-safe.
 *
 * @see <a href="http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar">http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar</a>
 * @see <a href="https://github.com/adamheinrich/native-utils">https://github.com/adamheinrich/native-utils</a>
 */
public final class LibraryLoader {

    private final Logger logger = LoggerFactory.getLogger(Constants.LAZYSODIUM_JAVA);

    /**
     * Library loading mode controls which libraries are attempted to be loaded (installed in the system or bundled
     * in the Lazysodium JAR) and in which order.
     */
    public enum Mode {

        /**
         * Try to load the system sodium first, if that fails — load the bundled version.
         *
         * <p>This is the recommended mode, because it allows the clients to upgrade the sodium library
         * as soon as it is available instead of waiting for lazysodium release and releasing a new version of
         * the client library/application.
         */
        PREFER_SYSTEM,

        /**
         * Load the bundled native libraries first, then fallback to finding it in the system.
         */
        PREFER_BUNDLED,

        /**
         * Load the bundled version, ignoring the system.
         *
         * <p>This mode might be useful if the system sodium turns out to be outdated and cannot be upgraded.
         */
        BUNDLED_ONLY,

        /**
         * Load the system sodium only, ignoring the bundled.
         *
         * <p>This mode is recommended if it is required to use the system sodium only, and the application
         * must fail if it is not installed.
         */
        SYSTEM_ONLY,
    }

    private List<Class> classes = new ArrayList<>();

    public LibraryLoader(List<Class> classesToRegister) {
        classes.addAll(classesToRegister);
    }

    public void loadAbsolutePath(String absPath) {
        SharedLibraryLoader.get().loadSystemLibrary(absPath, classes);
    }

    /**
     * Loads library from the current JAR archive and registers the native methods
     * of {@link Sodium} and {@link SodiumJava}. The library will be loaded at most once.
     *
     * <p>The file from JAR is copied into system temporary directory and then loaded.
     * The temporary file is deleted after exiting.
     */

    /**
     * Returns the absolute path to sodium library inside JAR (beginning with '/'), e.g. /linux/libsodium.so.
     *
     * @return The path to the libsodium binary.
     */


    private static String getPath(String folder, String name) {
        String separator = "/";
        return folder + separator + name;
    }
}
