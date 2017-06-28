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

import java.util.zip.GZIPOutputStream

import groovy.transform.CompileStatic


/**
 * Implements a stream filter for writing compressed data in GZip file format while providing means to (re)configure the
 * underlying deflater.
 *
 * <p>This class is merely a wrapper around the {@link GZIPOutputStream} with some added accessor methods to let the
 * user configure the underlying deflater used to carry out the actual compression work.</p>
 *
 * @author Yuri Sakhno
 */
@CompileStatic
public class ConfigurableGZIPOutputStream extends GZIPOutputStream {

    /**
     * Creates a new GZip output stream with default buffer size.
     *
     * <p>The new output stream instance is created as if by invoking the 2-argument constructor
     * {@code ConfigurableGZIPOutputStream(out, false)}.</p>
     *
     * @param out
     *            the output stream.
     * @exception IOException
     *                if an I/O error occurred.
     */
    public ConfigurableGZIPOutputStream(OutputStream out) throws IOException {
        super(out)
    }

    /**
     * Creates a new GZip output stream with the specified buffer size.
     *
     * <p>The new output stream instance is created as if by invoking the 3-argument constructor
     * {@code GZIPOutputStream(out, size, false)}.</p>
     *
     * @param out
     *            the output stream.
     * @param size
     *            the output buffer size.
     * @exception IOException
     *                if an I/O error occurred.
     * @exception IllegalArgumentException
     *                if {@code size} is less than or equal to zero.
     */
    public ConfigurableGZIPOutputStream(OutputStream out, int size) throws IOException {
        super(out, size)
    }

    /**
     * Creates a new GZip output stream with a default buffer size and the specified flush mode.
     *
     * @param out
     *            the output stream.
     * @param syncFlush
     *            if {@code true}, invocation of the inherited {@link DeflaterOutputStream#flush() flush()} method of
     *            this instance flushes the compressor with flush mode {@link Deflater#SYNC_FLUSH} before flushing the
     *            output stream, otherwise only flushes the output stream.
     *
     * @exception IOException
     *                if an I/O error occurred.
     */
    public ConfigurableGZIPOutputStream(OutputStream out, boolean syncFlush) throws IOException {
        super(out, syncFlush)
    }

    /**
     * Creates a new output stream with the specified buffer size and flush mode.
     *
     * @param out
     *            the output stream.
     * @param size
     *            the output buffer size
     * @param syncFlush
     *            if {@code true} invocation of the inherited {@link DeflaterOutputStream#flush() flush()} method of
     *            this instance flushes the compressor with flush mode {@link Deflater#SYNC_FLUSH} before flushing the
     *            output stream, otherwise only flushes the output stream.
     * @exception IOException
     *                if an I/O error occurred.
     * @exception IllegalArgumentException
     *                if {@code size} is less than or equal to zero.
     */
    public ConfigurableGZIPOutputStream(OutputStream out, int size, boolean syncFlush) throws IOException {
        super(out, size, syncFlush)
    }

    /**
     * Sets the compression level to the specified value.
     *
     * <p>By default, the {@link Deflater.DEFAULT_COMPRESSION} is used.</p>
     *
     * @param level
     *            the new compression level. Valid compression levels are:
     *            <ul>
     *            <li>values in the range 1-9, where greater number indicates slightly better compression in expense of
     *            the computational resources needed at compression time (no impact during decompression)</li>
     *            <li>0 - store (no compression)</li>
     *            <li>-1 - the stream selects default level of compression, which provides a good trade off between
     *            compressed size and compression time.</li>
     *            </ul>
     * @exception IllegalArgumentException
     *                if the compression level is invalid.
     */
    public void setLevel(int level) {
        this.def.level = level
    }

    /**
     * Sets the compression strategy to the specified value.
     *
     * <p>By default, the {@link Deflater.DEFAULT_STRATEGY} is used.</p>
     *
     * @param strategy
     *            the new compression strategy. This has to be one of the values defined in {@link Deflater}:
     *            {@link Deflater.FILTERED FILTERED}, {@link Deflater.HUFFMAN_ONLY HUFFMAN_ONLY} or
     *            {@link Deflater.DEFAULT_STRATEGY DEFAULT_STRATEGY}.
     * @exception IllegalArgumentException
     *                if the compression strategy is invalid.
     */
    public void setStrategy(int strategy) {
        this.def.strategy = strategy
    }
}
