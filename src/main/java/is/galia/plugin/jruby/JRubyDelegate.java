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

import is.galia.async.VirtualThreadPool;
import is.galia.config.Configuration;
import is.galia.delegate.Delegate;
import is.galia.delegate.DelegateException;
import is.galia.plugin.Plugin;
import is.galia.resource.RequestContext;
import is.galia.util.FileUtils;
import is.galia.util.Stopwatch;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * @see <a href="https://github.com/jruby/jruby/wiki/RedBridge">Embedding
 *      JRuby</a>
 */
public final class JRubyDelegate implements Delegate, Plugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(JRubyDelegate.class);

    /**
     * Name of the delegate class.
     */
    private static final String DELEGATE_CLASS_NAME = "CustomDelegate";

    /**
     * Name of the setter method used to set the request context, which ust be
     * present in the {@link #DELEGATE_CLASS_NAME delegate class}.
     */
    private static final String RUBY_REQUEST_CONTEXT_SETTER = "context=";

    static final String PATHNAME_CONFIG_KEY =
            "delegate.JRubyDelegate.script_pathname";

    private static final AtomicBoolean IS_CLASS_INITIALIZED =
            new AtomicBoolean(false);

    private static final ScriptingContainer SCRIPTING_CONTAINER =
            new ScriptingContainer(LocalContextScope.CONCURRENT,
                    LocalVariableBehavior.TRANSIENT);

    /**
     * Read/write lock used to maintain thread-safe code reloading.
     */
    private static final StampedLock CODE_LOAD_LOCK = new StampedLock();

    private static Path scriptFile;

    private static ScriptWatcher scriptWatcher;

    private RequestContext requestContext;

    /**
     * The Ruby delegate object.
     */
    private Object delegate;


    private static void initializeClass() {
        if (!IS_CLASS_INITIALIZED.getAndSet(true)) {
            Path scriptFile = getScriptFile();
            try {
                String code = Files.readString(scriptFile);
                load(code);

                scriptWatcher = new ScriptWatcher(scriptFile);
                VirtualThreadPool.getInstance().submit(scriptWatcher);
            } catch (Exception e) {
                LOGGER.error("initializeClass(): {}", e.getMessage(), e);
            }
        }
    }

    static Path getScriptFile() {
        if (scriptFile == null) {
            final Configuration config = Configuration.forApplication();
            // The script name may be an absolute pathname or a filename.
            String value = config.getString(PATHNAME_CONFIG_KEY, "");
            if (!value.isBlank()) {
                scriptFile = FileUtils.locate(value);
            }
        }
        return scriptFile;
    }

    /**
     * Loads the given code into the script engine.
     */
    static synchronized void load(String code) {
        Stopwatch watch = new Stopwatch();
        initializeClass();
        LOGGER.debug("Loading script code");
        final long stamp = CODE_LOAD_LOCK.writeLock();
        try {
            SCRIPTING_CONTAINER.runScriptlet(code);
            LOGGER.trace("Script code loaded in {}", watch);
        } finally {
            CODE_LOAD_LOCK.unlock(stamp);
        }
    }

    //region Plugin methods

    @Override
    public Set<String> getPluginConfigKeys() {
        return Set.of(PATHNAME_CONFIG_KEY);
    }

    @Override
    public String getPluginName() {
        return JRubyDelegate.class.getSimpleName();
    }

    @Override
    public void onApplicationStart() {
        initializeClass();
    }

    @Override
    public void onApplicationStop() {
        scriptWatcher.stop();
    }

    @Override
    public void initializePlugin() {
        tryInstantiateDelegate();
    }

    //endregion
    //region Delegate methods

    @Override
    public RequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @param context Context to set.
     * @throws DelegateException if the delegate script does not contain a
     *                         {@link #RUBY_REQUEST_CONTEXT_SETTER setter
     *                         method for the context}.
     */
    @Override
    public void setRequestContext(RequestContext context)
            throws DelegateException {
        invoke(RUBY_REQUEST_CONTEXT_SETTER,
                Collections.unmodifiableMap(context.toMap()));
        this.requestContext = context;
    }

    @Override
    public Object authorize() throws DelegateException {
        return invoke(DelegateMethod.AUTHORIZE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> deserializeMetaIdentifier(String metaIdentifier)
            throws DelegateException {
        Object result = invoke(DelegateMethod.DESERIALIZE_META_IDENTIFIER,
                metaIdentifier);
        if (result != null) {
            return (Map<String, Object>) result;
        }
        return Collections.emptyMap();
    }

    @Override
    public void customizeIIIF1InformationResponse(Map<String,Object> info)
            throws DelegateException {
        invoke(DelegateMethod.CUSTOMIZE_IIIF1_INFORMATION_RESPONSE, info);
    }

    @Override
    public void customizeIIIF2InformationResponse(Map<String,Object> info)
            throws DelegateException {
        invoke(DelegateMethod.CUSTOMIZE_IIIF2_INFORMATION_RESPONSE, info);
    }

    @Override
    public void customizeIIIF3InformationResponse(Map<String,Object> info)
            throws DelegateException {
        invoke(DelegateMethod.CUSTOMIZE_IIIF3_INFORMATION_RESPONSE, info);
    }

    @Override
    public String getFilesystemSourcePathname() throws DelegateException {
        Object result = invoke(DelegateMethod.FILESYSTEMSOURCE_PATHMAME);
        return (String) result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,?> getHTTPSourceResourceInfo() throws DelegateException {
        Object result = invoke(DelegateMethod.HTTPSOURCE_RESOURCE_INFO);
        if (result instanceof String) {
            Map<String,String> map = new HashMap<>();
            map.put("uri", (String) result);
            return map;
        } else if (result instanceof Map) {
            return (Map<String,Object>) result;
        }
        return Collections.emptyMap();
    }

    @Override
    public String getMetadata() throws DelegateException {
        Object result = invoke(DelegateMethod.METADATA);
        return (String) result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String,Object> getOverlayProperties() throws DelegateException {
        Object result = invoke(DelegateMethod.OVERLAY);
        if (result != null) {
            return (Map<String, Object>) result;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String,Object>> getRedactions() throws DelegateException {
        Object result = invoke(DelegateMethod.REDACTIONS);
        if (result != null) {
            return Collections.unmodifiableList((List<Map<String,Object>>) result);
        }
        return Collections.emptyList();
    }

    @Override
    public String getSource() throws DelegateException {
        Object result = invoke(DelegateMethod.SOURCE);
        return (String) result;
    }

    @Override
    public Object authorizeBeforeAccess() throws DelegateException {
        return invoke(DelegateMethod.AUTHORIZE_BEFORE_ACCESS);
    }

    @Override
    public String serializeMetaIdentifier(Map<String, Object> metaIdentifier)
            throws DelegateException {
        Object result = invoke(DelegateMethod.SERIALIZE_META_IDENTIFIER,
                metaIdentifier);
        return (String) result;
    }

    /**
     * N.B.: The returned object should not be modified, as this could disrupt
     * the invocation cache.
     *
     * @param method Method to invoke.
     * @param args   Arguments to pass to the method.
     * @return       Return value of the method.
     */
    @Override
    public Object invoke(String method,
                         Object... args) throws DelegateException {
        final long stamp = CODE_LOAD_LOCK.readLock();

        final String argsList = (args.length > 0) ?
                Arrays.stream(args)
                        .map(a -> a.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")) : "none";
        LOGGER.debug("Invoking {}() with args: ({})", method, argsList);

        final Stopwatch watch = new Stopwatch();
        try {
            final Object retval = SCRIPTING_CONTAINER.callMethod(
                    delegate, method, args);
            if (!RUBY_REQUEST_CONTEXT_SETTER.equals(method)) {
                LOGGER.debug("{}() returned a {} for args: ({}) in {}",
                        method,
                        (retval != null) ? retval.getClass().getSimpleName() : "nil",
                        argsList,
                        watch);
            }
            return retval;
        } catch (InvokeFailedException e) {
            throw new DelegateException(e);
        } finally {
            CODE_LOAD_LOCK.unlock(stamp);
        }
    }

    //endregion
    //region Private methods

    /**
     * There is an apparent bug in JRuby (9.2.11.1) whereby invocation of
     * {@code new()} on the delegate class, under rare and unknown conditions
     * possibly involving high memory usage, will not return an instance,
     * leaving {@link #delegate} null. This method attempts to retry the
     * instantiation several times in that case, until it is no longer null.
     *
     * @see <a href="https://github.com/cantaloupe-project/cantaloupe/issues/402">
     *     https://github.com/cantaloupe-project/cantaloupe/issues/402</a>
     */
    private void tryInstantiateDelegate() {
        final short numAttempts = 10;
        short attempt = 0;
        do {
            if (delegate == null) {
                instantiateDelegate();
            } else {
                break;
            }
        } while (attempt++ < numAttempts);
    }

    private void instantiateDelegate() {
        final long stamp = CODE_LOAD_LOCK.readLock();
        try {
            delegate = SCRIPTING_CONTAINER.runScriptlet(DELEGATE_CLASS_NAME + ".new");
        } finally {
            CODE_LOAD_LOCK.unlock(stamp);
        }
    }

    private Object invoke(DelegateMethod method,
                          Object... args) throws DelegateException {
        return invoke(method.getMethodName(), args);
    }

}
