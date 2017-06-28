/*
 * Copyright © 2017, Yuri Sakhno.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.ysakhno.assetpipeline.compress

import java.lang.reflect.Field

import groovy.transform.CompileStatic

import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader


@CompileStatic
public final class BrotliLoader {

    private static final Object sync = new Object()

    private static Field loadedLibrayNames_field

    private static Throwable originalFailure = null

    static {
        /*
         * The following block of code can fail if running not inside
         * a Sun-compatible JVM or if the security is too tight
         */
        try {
            BrotliLoader.loadedLibrayNames_field = ClassLoader.class.getDeclaredField("loadedLibraryNames")

            if (BrotliLoader.loadedLibrayNames_field.type != Vector) {
                throw new RuntimeException("Not of type java.util.Vector: $loadedLibrayNames_field.type.name")
            }

            BrotliLoader.loadedLibrayNames_field.accessible = true
        } catch (Throwable t) {
            BrotliLoader.originalFailure = t
            BrotliLoader.loadedLibrayNames_field = null
        }
    }

    /**
     * Returns names of all native libraries currently loaded by the specified {@code ClassLoader}.
     *
     * @param loader
     *            class loader to inspect. This argument must not be {@code null}.
     * @return array with names of libraries currently loaded by the specified class loader. This array can be empty but
     *         never {@code null}.
     * @throws IllegalArgumentException
     *                if argument {@code loader} is {@code null}.
     * @throws RuntimeException
     *                if the &quot;{@code loadedLibrayNames}&quot; field hack is not possible in this JRE.
     */
    public static String[] getLoadedLibraries(final ClassLoader loader) {
        if (loader == null) {
            throw new IllegalArgumentException("Argument 'loader' must not be null.")
        }
        if (BrotliLoader.loadedLibrayNames_field == null) {
            throw new RuntimeException("Impossible to obtain the list of loaded libraries in this JRE",
                    BrotliLoader.originalFailure)
        }

        try {
            final Vector<String> libraries = (Vector<String>) BrotliLoader.loadedLibrayNames_field.get(loader)

            if (libraries == null) {
                return new String[0]
            }

            // NOTE: Vector is synchronized in Java 2, which helps us make
            // the following into a safe critical section:
            synchronized (libraries) {
                return libraries.toArray(new String[0])
            }
        } catch (IllegalAccessException iae) {
            iae.printStackTrace(System.err)
            return new String[0]
        }
    }

    /**
     * A convenience multi-loader version of {@link #getLoadedLibraries(ClassLoader)}.
     *
     * @param loaders
     *            array of defining class loaders to inspect. This argument must not be {@code null}, but can be an
     *            empty array.
     * @return array with names of loaded libraries. This array can be empty but never {@code null}.
     * @throws IllegalArgumentException
     *                if argument {@code loaders} is {@code null}.
     * @throws RuntimeException
     *                if the &quot;{@code loadedLibrayNames}&quot; field hack is not possible in this JRE.
     */
    public static String[] getLoadedLibraries(final ClassLoader[] loaders) {
        if (loaders == null) {
            throw new IllegalArgumentException("Argument 'loaders' must not be null.")
        }

        final def libraries = []

        for (int i = 0; i < loaders.length; ++i) {
            final def loader = loaders[i]

            if (loader) {
                libraries += getLoadedLibraries(loader).toList()
            }
        }

        return libraries.toArray(new String[libraries.size()])
    }

    public static String[] listAllLoadedNativeLibraries() {
        ClassLoader[] loaders = [ ClassLoader.getSystemClassLoader(), BrotliLoader.class.getClassLoader() ]

        return BrotliLoader.getLoadedLibraries(loaders)
    }

    public static boolean loadLibrary() {
        synchronized (BrotliLoader.sync) {
            final boolean loaded = listAllLoadedNativeLibraries().find { String lib ->
                return (lib.endsWith("brotli.dll") || lib.endsWith("libbrotli.so") || lib.endsWith("libbrotli.dylib"))
            }

            if (!loaded) {
                BrotliLibraryLoader.loadBrotli()
            }
        }

        return true
    }

    /**
     * This constructor is declared private to prohibit class's instantiation.
     */
    private BrotliLoader() {
        // does nothing
    }
}
