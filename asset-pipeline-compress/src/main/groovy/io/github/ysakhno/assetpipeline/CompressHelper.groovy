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
package io.github.ysakhno.assetpipeline

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import asset.pipeline.AssetCompiler
import asset.pipeline.AssetHelper
import asset.pipeline.AssetPipelineConfigHolder

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult

import io.github.ysakhno.assetpipeline.compress.Compressor


@CompileStatic
class CompressHelper {

    public static void compressFiles(AssetCompiler compiler, Map<String, String> files) {
        if (compiler.options.enableGzip == false) {
            return
        }

        final def compressors = CompressHelper.findCompressorClasses()
        final Set<String> excludes =
            CompressHelper.collectExcludes(CompressHelper.fixGzipExcludesOption(compressors, compiler.options))

        for (def normalFileName : files.keySet()) {
            final def digestedFileName = files[normalFileName]

            final File normalFile = getFile(compiler, excludes, normalFileName)
            final File digestedFile = getFile(compiler, excludes, digestedFileName)

            if (normalFile || digestedFile) {
                CompressHelper.triggerEvent(compiler, "Compressing file $normalFileName")
                CompressHelper.compress(compressors, normalFile, digestedFile)
            }
        }
    }

    @CompileDynamic
    private static triggerEvent(AssetCompiler compiler, String message) {
        compiler.eventListener?.triggerEvent("StatusUpdate", message)
    }

    private static Collection<Class<?>> findCompressorClasses() {
        final def scanner = new FastClasspathScanner()
        final def result = scanner.scan()

        final def classNames = new TreeSet()

        classNames += result.getNamesOfClassesWithAnnotation(AssetCompressor)
        classNames += result.getNamesOfClassesImplementing(Compressor)

        final ClassLoader classLoader = Thread.currentThread().contextClassLoader

        classLoader.getResources("META-INF/asset-pipeline/compressor.specs").each { URL res ->
            classNames += res.getText("UTF-8").split(/\r?\n/).
                    collect { String str -> str.trim() }.findAll{ String str -> str.length() }
        }

        final List instances = []

        for (String className : classNames) {
            final Class<?> classRef = result.classNameToClassRef(className.trim(), true)
            if (classRef && !classRef.interface && !classRef.enum) {
                instances.add(classRef.newInstance())
            }
        }

        return instances
    }

    @CompileDynamic
    private static Map fixGzipExcludesOption(Collection compressors, Map options) {
        def defaultExcludedExtensions = [ "gz", "br" ] as Set

        for (def compObj : compressors) {
            if (compObj instanceof Compressor) {
                final Compressor compressor = (Compressor) compObj

                defaultExcludedExtensions += compressor.ignoredExtensions
            } else if (compObj.metaClass.hasProperty(compObj, "ignoredExtensions")) {
                defaultExcludedExtensions += compObj.ignoredExtensions
            }
        }

        if (options.excludesGzip instanceof Collection) {
            options.excludesGzip += defaultExcludedExtensions
        } else {
            options.excludesGzip = defaultExcludedExtensions
        }

        return options
    }

    @CompileDynamic
    private static Set<String> collectExcludes(Map options) {
        return options.excludesGzip.collect { it.toLowerCase(Locale.US) }.toSet() ?: Collections.emptySet()
    }

    private static File getFile(AssetCompiler compiler, Set<String> excludes, String filename) {
        if (excludes.contains(AssetHelper.extensionFromURI(filename).toLowerCase(Locale.US))) {
            return null
        }

        final File file = new File((String) compiler.options.compileDir, filename)

        return (file.exists() && !file.directory) ? file : null
    }

    @CompileDynamic
    private static void compress(Collection compressors, File normalFile, File digestedFile) {
        final def config = AssetPipelineConfigHolder.config.compressors
        final File fileToCompress = normalFile ?: digestedFile

        for (def compObj : compressors) {
            File compressedFile = null

            if (compObj instanceof Compressor) {
                final Compressor compressor = (Compressor) compObj
                final Map options = config?.get(compressor.name) ?: [:]

                compressedFile = compressor.compress(fileToCompress, options)
            } else if (compObj.metaClass.respondsTo(compObj, "compress", File, Map)) {
                final String compressorName = (compObj.metaClass.hasProperty(compObj, "name")) ? compObj.name : null
                final Map options = ((compressorName) ? config?.get(compressorName) : null) ?: [:]

                compressedFile = compObj.compress(fileToCompress, options)
            }

            if (compressedFile) {
                if (compressedFile.size() >= fileToCompress.size() &&
                    AssetPipelineConfigHolder.config.compressorKeepBiggerFiles != true) {
                    compressedFile.delete()
                    continue
                }

                if (normalFile && digestedFile) {
                    String compressedExtension = null

                    if (compObj instanceof Compressor) {
                        compressedExtension = ((Compressor) compObj).compressedExtension
                    } else if (compObj.metaClass.hasProperty(compObj, "compressedExtension")) {
                        compressedExtension = compObj.compressedExtension
                    }

                    if (compressedExtension?.length() > 0) {
                        Files.copy(compressedFile.toPath(),
                                Paths.get("${digestedFile.absolutePath}.$compressedExtension"),
                                StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }
}
