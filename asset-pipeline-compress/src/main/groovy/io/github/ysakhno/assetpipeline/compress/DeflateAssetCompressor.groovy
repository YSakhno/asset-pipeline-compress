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
import java.util.zip.DeflaterOutputStream

import groovy.transform.CompileStatic

import io.github.ysakhno.assetpipeline.AssetCompressor


@AssetCompressor
@CompileStatic
class DeflateAssetCompressor {

    static name = "deflate"

    static compressedExtension = "deflate"

    static ignoredExtensions = [ "deflate" ]

    File compress(File inputFile, Map options) {
        File compressedFile = new File("${inputFile.absolutePath}.$compressedExtension")

        compressedFile.createNewFile()

        new FileInputStream(inputFile).withStream { inputStream ->
            final def deflater = new Deflater(Deflater.BEST_COMPRESSION)

            new DeflaterOutputStream(compressedFile.newOutputStream(), deflater, true).withStream { outputStream ->
                byte[] buffer = new byte[8192]
                int bytesRead

                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.finish()
            }
        }

        return compressedFile
    }
}
