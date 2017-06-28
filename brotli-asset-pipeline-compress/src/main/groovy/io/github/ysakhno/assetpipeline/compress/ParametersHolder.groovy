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


/**
 * Holds Brotli compressor parameters ({@code Brotli.Parameter} instance) along with additional parameters specific for
 * the asset-pipeline plugin such as whether tinkering with Brotli parameters should be done to find best compression.
 *
 * @author Yuri Sakhno
 * @see #fromOptions(Map)
 */
@CompileStatic
class ParametersHolder {

    /**
     * An instance of the parameters for the Brotli compressor.
     *
     * <p><b>Implementation notes:</b> This field is initialized at construction time and is immutable ever since. Can
     * be accessed via the {@link #getParameters()} method. Note: only the reference is immutable, the parameters that
     * this field actually describes <i>are</i> mutable.</p>
     */
    private final Brotli.Parameter parameters = new Brotli.Parameter()

    /**
     * Determines whether the brotli-compressor asset-pipeline plugin should do several trials with different
     * compression modes and select the mode with the best compression result.
     *
     * <p><b>Value:</b> If several trials should be done, this field contains {@code true}; if the compression should be
     * performed in just one mode (specified by the {@link #parameters} field), this field contains {@code false}.</p>
     *
     * <p><b>Implementation notes:</b> At construction time this field is initialized to {@code false}. After that it
     * can be changed by the {@link #determineCompressionMode} private method depending on the value of the argument
     * passed to that method, and accessed via the {@link #isFindBestMode()} method.</p>
     */
    private boolean findBestMode = false

    /**
     * Determines whether the brotli-compressor asset-pipeline plugin should do several trials with different window
     * sizes and select the smallest window that still provides best compression.
     *
     * <p><b>Value:</b> If several trials should be done, this field contains {@code true}; if the compression should be
     * performed using just one window size (specified by the {@link #parameters} field), this field contains
     * {@code false}.</p>
     *
     * <p><b>Implementation notes:</b> At construction time this field is initialized to {@code false}. After that it
     * can be changed by the {@link #determineWindowSize} private method depending on the value of the argument passed
     * to that method, and accessed via the {@link #isFindSmallestWindow()} method.</p>
     */
    private boolean findSmallestWindow = false

    /**
     * Initializes a new instance of the {@link ParametersHolder} class.
     */
    public ParametersHolder() {
        // empty constructor
    }

    /**
     * Gets the Brotli compression parameters (${@code Brotli.Parameter} instance) stored by this class.
     *
     * <p>Consumers of this class can alter the returned instance directly, and this will affect the parameters stored
     * internally.</p>
     *
     * @return the instance of ${@link Brotli.Parameter} representing Brotli compression parameters; never {@code null}.
     */
    public Brotli.Parameter getParameters() {
        return this.parameters
    }

    /**
     * Gets a boolean value that determines whether the brotli-compressor asset-pipeline plugin should do several trials
     * with different compression modes and select the mode with the best compression result.
     *
     * @return {@code true} if several trials should be done, otherwise {@code false}. If {@code false} is returned, the
     *         compression should be performed in just one mode (specified by the parameters accessible via the
     *         {@link #getParameters()} method).
     * @see #getParameters()
     */
    public boolean isFindBestMode() {
        return this.@findBestMode
    }

    /**
     * Gets a boolean value that determines whether the brotli-compressor asset-pipeline plugin should do several trials
     * with different window sizes and select the smallest window that still provides best compression.
     *
     * @return {@code true} if several trials should be done, otherwise {@code false}. If {@code false} is returned, the
     *         compression should be performed using just one window size (specified by the parameters accessible via
     *         the {@link #getParameters()} method).
     * @see #getParameters()
     */
    public boolean isFindSmallestWindow() {
        return this.@findSmallestWindow
    }

    /**
     * Creates a new instance of the {@link ParametersHolder} and initializes it from the {@code options} argument.
     *
     * @param options
     *            the map containing options that should be used to initialize the newly-created parameters holder
     *            instance. This argument can be {@code null} or an empty map, in which case reasonable defaults are
     *            assumed.
     * @return newly created and initialized {@code ParametersHolder} instance.
     */
    public static ParametersHolder fromOptions(Map options) {
        def holder = new ParametersHolder()

        holder.determineCompressionMode(options?.mode)
        holder.determineCompressionQuality(options?.quality)
        holder.determineWindowSize(options?.lgwin)

        return holder
    }

    /**
     * Advances to the next compression mode in the set of all possible Brotli compression modes and returns an
     * indication of whether the advancing operation was successful.
     *
     * @return {@code true} if successfully advanced to the next mode; the brotli compression parameters stored
     *         internally (and accessible via the {@link getParameters()} method) are now reconfigured to the new mode.
     *         <p>If advancement was unsuccessful, returns {@code false}. This can happen either because currently
     *         stored configuration does not specify that several modes should be attempted, or if the list of all
     *         possible modes has been exhausted. If the return value is {@code false}, configured compression mode is
     *         retained (i.e. stays the same as was set prior to calling this method).</p>
     *
     * @see #getParameters()
     * @see #isFindBestMode()
     */
    public boolean nextMode() {
        if (!isFindBestMode()) {
            return false
        }

        final int value = this.parameters.mode.mode
        final def values = Brotli.Mode.values()

        if (value+1 < values.length) {
            this.parameters.mode = values[value+1]
            return true
        } else {
            return false
        }
    }

    /**
     * Analyzes the {@code mode} argument and sets compression mode accordingly.
     *
     * @param mode
     *            either a string or an integer value specifying the compression mode to set. This argument can be
     *            {@code null} or an empty string, in which case string {@code "adaptive"} is assumed.
     *            <p>If this argument is an integer (or a string containing an integer number), the mode is set to one
     *            of the modes defined by the {@link Brotli.Mode} enumeration, depending on the order in which the enum
     *            definitions are listed in that enumeration. If the specified integer is negative, or greater that or
     *            equal to the number of supported compression modes, this method might throw an exception.</p>
     *
     * @see #fromOptions(Map)
     */
    private void determineCompressionMode(def mode) {
        if (!mode) {
            mode = "adaptive"
        }

        if (mode instanceof CharSequence) {
            final def modeStr = mode.toString()

            if (modeStr.equalsIgnoreCase("generic")) {
                this.parameters.mode = Brotli.Mode.GENERIC
            } else if (modeStr.equalsIgnoreCase("text")) {
                this.parameters.mode = Brotli.Mode.TEXT
            } else if (modeStr.equalsIgnoreCase("font")) {
                this.parameters.mode = Brotli.Mode.FONT
            } else if (modeStr.equalsIgnoreCase("default")) {
                this.parameters.mode = Brotli.DEFAULT_MODE
            } else if (modeStr.equalsIgnoreCase("adapt") || modeStr.equalsIgnoreCase("adaptive")) {
                this.parameters.mode = Brotli.Mode.values()[0]
                this.findBestMode = true
            } else if (mode.isInteger()) {
                mode = mode as Integer
            }
        }

        if (mode instanceof Integer) {
            this.parameters.mode = Brotli.Mode.values()[(Integer) mode]
        }
    }

    /**
     * Analyzes the {@code quality} argument and sets compression quality accordingly.
     *
     * <p>If this argument is an integer (or a string containing an integer number), the quality is set directly to the
     * number specified by the argument. The method does not perform any checks of whether the specified number is
     * within the quality boundaries allowed by the underlying compressor. This means that, although this method will
     * not throw any exceptions (at the time of the call) due to number specified by {@code quality} being out of
     * allowed range, an exception might be raised later when the actual compression takes place.</p>
     *
     * @param quality
     *            either a string or an integer value specifying the compression quality to set. This argument can be
     *            {@code null} or an empty string, in which case string {@code "best"} is assumed.
     *            <p>This argument can be a string containing an integer number.</p>
     *
     * @see #fromOptions(Map)
     */
    private void determineCompressionQuality(def quality) {
        if (!quality) {
            quality = "best"
        }

        if (quality instanceof CharSequence) {
            final def qualityStr = quality.toString()

            if (qualityStr.equalsIgnoreCase("best")) {
                this.parameters.quality = 11
            } else if (qualityStr.equalsIgnoreCase("fastest") || qualityStr.equalsIgnoreCase("fast")) {
                this.parameters.quality = 0
            } else if (qualityStr.equalsIgnoreCase("default")) {
                this.parameters.quality = Brotli.DEFAULT_QUALITY
            } else if (quality.isInteger()) {
                quality = quality as Integer
            }
        }

        if (quality instanceof Integer) {
            this.parameters.quality = (Integer) quality
        }
    }

    /**
     * Analyzes the {@code lgwin} argument and sets window size accordingly. Window size is roughly set to
     * 2<sup><i>lgwin</i></sup> bytes if argument {@code lgwin} is an integer (or a string containing an integer
     * number), and this number is greater than zero.
     *
     * <p>If this argument is an integer (or a string containing an integer number), the window size parameter is set
     * directly to the number specified by the argument. The method does not perform any checks of whether the specified
     * number is within the windows size boundaries allowed by the underlying compressor. This means that, although this
     * method will not throw any exceptions (at the time of the call) due to number specified by {@code lgwin} being out
     * of allowed range, an exception might be raised later when the actual compression takes place.</p>
     *
     * @param lgwin
     *            either a string or an integer value specifying the window size to set. This argument can be
     *            {@code null} or an empty string, in which case string {@code "fit"} is assumed.
     *            <p>This argument can be a string containing an integer number.</p>
     *
     * @see #fromOptions(Map)
     */
    private void determineWindowSize(def lgwin) {
        if (!lgwin) {
            lgwin = "fit"
        }

        if (lgwin instanceof CharSequence) {
            final def lgwinStr = lgwin.toString()

            if (lgwinStr.equalsIgnoreCase("default")) {
                this.parameters.lgwin = Brotli.DEFAULT_LGWIN
            } else if (lgwinStr.equalsIgnoreCase("fit")) {
                this.parameters.lgwin = 24
                this.findSmallestWindow = true
            } else if (lgwin.isInteger()) {
                lgwin = lgwin as Integer
            }
        }

        if (lgwin instanceof Integer) {
            this.parameters.lgwin = (Integer) lgwin
        }
    }
}
