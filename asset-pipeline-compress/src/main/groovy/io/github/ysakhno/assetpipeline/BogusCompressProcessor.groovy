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

import groovy.transform.CompileStatic

import asset.pipeline.AbstractProcessor
import asset.pipeline.AssetCompiler
import asset.pipeline.AssetFile


/**
 * @author Yuri Sakhno
 */
@CompileStatic
class BogusCompressProcessor extends AbstractProcessor {

    private static final Object sync = new Object()

    public BogusCompressProcessor(AssetCompiler compiler) {
        super(compiler)
        if (compiler?.options?.compressorsTapped != true) {
            tapIntoAssetCompiler()
        }
    }

    @Override
    public String process(String inputText, AssetFile assetFile) {
        return inputText
    }

    private void tapIntoAssetCompiler() {
        synchronized (BogusCompressProcessor.sync) {
            if (this.precompiler?.options?.compressorsTapped != true) {
                this.precompiler.manifestProperties = new PropertiesWrapper(this.precompiler)
            }
            this.precompiler.options.compressorsTapped = true
        }
    }
}
