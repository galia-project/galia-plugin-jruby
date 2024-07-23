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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import is.galia.util.FilesystemWatcher;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Listens for changes to a script file and reloads it when it has changed.
 */
final class ScriptWatcher implements Runnable {

    private static class ChangeCallback implements FilesystemWatcher.Callback {

        private byte[] currentChecksum = new byte[0];

        @Override
        public void created(Path path) {
            handle(path);
        }

        @Override
        public void deleted(Path path) {
            Path scriptFile = JRubyDelegate.getScriptFile();
            if (scriptFile.equals(path)) {
                LOGGER.warn("Delegate script no longer exists: {}", path);
            }
        }

        @Override
        public void modified(Path path) {
            handle(path);
        }

        private void handle(Path path) {
            try {
                if (path.equals(JRubyDelegate.getScriptFile())) {
                    // Some filesystems will generate multiple events that
                    // could result in this method being invoked multiple
                    // times. To avoid that, we will calculate the checksum of
                    // the file contents and compare it to what has already
                    // been loaded. If the checksums match, skip the load.
                    final byte[] fileBytes = Files.readAllBytes(path);
                    final MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(fileBytes);
                    byte[] newChecksum = md.digest();

                    if (!Arrays.equals(newChecksum, currentChecksum)) {
                        LOGGER.debug("Script checksums differ; reloading");
                        currentChecksum = newChecksum;
                        final String code = new String(fileBytes, StandardCharsets.UTF_8);
                        JRubyDelegate.load(code);
                    } else {
                        LOGGER.debug("Script checksums match; skipping reload");
                    }
                }
            } catch (NoSuchFileException e) {
                LOGGER.error("File not found: {}", e.getMessage());
            } catch (NoSuchAlgorithmException | IOException e) {
                LOGGER.error(e.getMessage());
            }
        }

    }

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ScriptWatcher.class);

    private FilesystemWatcher filesystemWatcher;

    /**
     * @param scriptFile File to watch.
     */
    ScriptWatcher(Path scriptFile) {
        Path path = scriptFile.toAbsolutePath().getParent();
        try {
            filesystemWatcher = new FilesystemWatcher(
                    path, new ChangeCallback());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void run() {
        if (filesystemWatcher != null) {
            filesystemWatcher.start();
        }
    }

    public void stop() {
        if (filesystemWatcher != null) {
            filesystemWatcher.stop();
        }
    }

}