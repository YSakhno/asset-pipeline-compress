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

import groovy.transform.CompileStatic

import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.io.BrotliOutputStream


@CompileStatic
class BrotliAssetCompressor {

    static name = "brotli"

    static compressedExtension = "br"

    static ignoredExtensions = [ "br" ]

    File compress(File inputFile, Map options) {
        if (!BrotliLoader.loadLibrary()) {
            return null
        }

        final def parametersHolder = ParametersHolder.fromOptions(options)
        byte[] bestCompressedBuffer = null
        def bestMode = parametersHolder.parameters.mode

        for (;;) {
            final def compressedBuffer = compress0(inputFile, parametersHolder.parameters)

            if (!bestCompressedBuffer || compressedBuffer?.size() < bestCompressedBuffer.size()) {
                bestCompressedBuffer = compressedBuffer
                bestMode = parametersHolder.parameters.mode
            }

            if (!parametersHolder.findBestMode || !parametersHolder.nextMode()) {
                break
            }
        }

        if (!bestCompressedBuffer) {
            return null
        }

        if (parametersHolder.findSmallestWindow) {
            parametersHolder.parameters.mode = bestMode

            while (parametersHolder.parameters.lgwin > 10) {
                parametersHolder.parameters.lgwin = parametersHolder.parameters.lgwin-1

                final def compressedBuffer = compress0(inputFile, parametersHolder.parameters)

                if (compressedBuffer?.size() <= bestCompressedBuffer.size()) {
                    bestCompressedBuffer = compressedBuffer
                } else {
                    break
                }
            }
        }

        final File compressedFile = new File("${inputFile.absolutePath}.$compressedExtension")

        compressedFile.createNewFile()
        compressedFile.withOutputStream { it.write(bestCompressedBuffer) }

        return compressedFile
    }

    private static byte[] compress0(File inputFile, Brotli.Parameter parameters) {
        final def byteStream = new ByteArrayOutputStream()

        new FileInputStream(inputFile).withStream { inputStream ->
            new BrotliOutputStream(byteStream, parameters).withStream { outputStream ->
                byte[] buffer = new byte[(parameters.quality > 9) ? (int) inputFile.length() : 8192]
                int bytesRead

                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    if (byteStream.size() >= inputFile.length()) {
                        break
                    }
                }

                outputStream.flush()
            }
        }

        return (byteStream.size() < inputFile.length() && byteStream.size() > 0) ? byteStream.toByteArray() : null
    }
}
