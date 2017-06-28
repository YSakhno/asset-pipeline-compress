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

import java.util.zip.Deflater

import groovy.transform.CompileStatic

import io.github.ysakhno.assetpipeline.AssetCompressor


/**
 * @author Yuri Sakhno
 */
@AssetCompressor
@CompileStatic
class GzipAssetCompressor implements Compressor {

    public static final Set<String> DEFAULT_IGNORED_EXTENSIONS = Collections.unmodifiableSet([ "gz" ].toSet())

    public GzipAssetCompressor() {
        // empty constructor
    }

    @Override
    public String getName() {
        return "gzip"
    }

    @Override
    public String getCompressedExtension() {
        return "gz"
    }

    @Override
    public Collection<String> getIgnoredExtensions() {
        return GzipAssetCompressor.DEFAULT_IGNORED_EXTENSIONS
    }

    @Override
    public File compress(File inputFile, Map options) {
        File compressedFile = new File("${inputFile.absolutePath}.$compressedExtension")

        if (compressedFile.exists() && options.recompress != true) {
            return null
        }

        compressedFile.createNewFile()

        new FileInputStream(inputFile).withStream { inputStream ->
            new ConfigurableGZIPOutputStream(compressedFile.newOutputStream(), true).withStream { outputStream ->
                outputStream.level = determineCompressionLevel(options)

                byte[] buffer = new byte[8192]
                int bytesRead

                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.finish()
            }
        }

        return compressedFile
    }

    private static int determineCompressionLevel(Map options) {
        final def level = options.level

        if (level instanceof Integer) {
            return (Integer) level
        } else if (level instanceof CharSequence) {
            final def levelStr = level.toString()

            if (levelStr.equalsIgnoreCase("best")) {
                return Deflater.BEST_COMPRESSION
            } else if (levelStr.equalsIgnoreCase("fastest") || levelStr.equalsIgnoreCase("fast")) {
                return Deflater.BEST_SPEED
            } else if (levelStr.equalsIgnoreCase("default")) {
                return Deflater.DEFAULT_COMPRESSION
            } else if (levelStr.equalsIgnoreCase("store")) {
                return Deflater.NO_COMPRESSION
            } else if (level.isInteger()) {
                return level as Integer
            }
        }

        return Deflater.BEST_COMPRESSION
    }
}
