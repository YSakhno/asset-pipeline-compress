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

import asset.pipeline.AssetCompiler


/**
 * @author Yuri Sakhno
 */
@CompileStatic
public class PropertiesWrapper extends Properties {

    /**
     *
     */
    private static final long serialVersionUID = 5398089545381819664L

    private final AssetCompiler compiler

    private final Properties wrappedProperties

    public PropertiesWrapper(AssetCompiler compiler) {
        this.compiler = compiler
        this.wrappedProperties = compiler?.manifestProperties ?: new Properties()
    }

    @Override
    public Enumeration<?> propertyNames() {
        return this.wrappedProperties.propertyNames()
    }

    @Override
    public Set<String> stringPropertyNames() {
        return this.wrappedProperties.stringPropertyNames()
    }

    @Override
    public String getProperty(String key) {
        return this.wrappedProperties.getProperty(key)
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return this.wrappedProperties.getProperty(key, defaultValue)
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        return this.wrappedProperties.setProperty(key, value)
    }

    @Override
    public synchronized void load(Reader reader) throws IOException {
        this.wrappedProperties.load(reader)
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException {
        this.wrappedProperties.load(inStream)
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        CompressHelper.compressFiles(this.compiler, (Map<String, String>) this.wrappedProperties)
        this.wrappedProperties.store(writer, comments)
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        CompressHelper.compressFiles(this.compiler, (Map<String, String>) this.wrappedProperties)
        this.wrappedProperties.store(out, comments)
    }

    @Override
    public synchronized void loadFromXML(InputStream inStream) throws IOException, InvalidPropertiesFormatException {
        this.wrappedProperties.loadFromXML(inStream)
    }

    @Override
    public void storeToXML(OutputStream out, String comment) throws IOException {
        this.wrappedProperties.storeToXML(out, comment)
    }

    @Override
    public void storeToXML(OutputStream out, String comment, String encoding) throws IOException {
        this.wrappedProperties.storeToXML(out, comment, encoding)
    }

    @Override
    public void list(PrintStream out) {
        this.wrappedProperties.list(out)
    }

    @Override
    public void list(PrintWriter out) {
        this.wrappedProperties.list(out)
    }
}
