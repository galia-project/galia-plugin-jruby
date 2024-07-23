/*
 * Copyright © 2024 Baird Creek Software LLC
 *
 * Licensed under the PolyForm Noncommercial License, version 1.0.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://polyformproject.org/licenses/noncommercial/1.0.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package is.galia.plugin.jruby;

import is.galia.delegate.Delegate;
import org.slf4j.LoggerFactory;

/**
 * Logger for use by delegates.
 */
@SuppressWarnings("unused")
public final class Logger {

    private static final org.slf4j.Logger LOGGER =
            LoggerFactory.getLogger(Delegate.class);

    public static void trace(String message) {
        LOGGER.trace(message);
    }

    public static void trace(String message, Throwable throwable) {
        LOGGER.trace(message, throwable);
    }

    public static void debug(String message) {
        LOGGER.debug(message);
    }

    public static void debug(String message, Throwable throwable) {
        LOGGER.debug(message, throwable);
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void info(String message, Throwable throwable) {
        LOGGER.info(message, throwable);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void warn(String message, Throwable throwable) {
        LOGGER.warn(message, throwable);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    private Logger() {}

}
