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

import is.galia.config.Configuration;
import is.galia.delegate.DelegateException;
import is.galia.plugin.jruby.test.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import is.galia.image.Identifier;
import is.galia.resource.RequestContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JRubyDelegateTest {

    private JRubyDelegate instance;

    @BeforeAll
    public static void beforeClass() {
        new JRubyDelegate().onApplicationStart();
    }

    @BeforeEach
    public void setUp() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("cats"));

        Path scriptFile = TestUtils.getFixture("delegates.rb");
        String code = Files.readString(scriptFile);
        JRubyDelegate.load(code);

        instance = new JRubyDelegate();
        instance.initializePlugin();
        instance.setRequestContext(context);
    }

    /* getScriptFile() */

    @Test
    void getScriptFileWithScriptPathSetInConfiguration()
            throws Exception {
        Path scriptFile = TestUtils.getFixture("delegates.rb");
        Configuration config = Configuration.forApplication();
        try {
            config.setProperty(
                    JRubyDelegate.PATHNAME_CONFIG_KEY,
                    scriptFile.toString());

            Path actual = JRubyDelegate.getScriptFile();
            String code = Files.readString(actual);
            assertTrue(code.contains("CustomDelegate"));
        } finally {
            config.clearProperty(JRubyDelegate.PATHNAME_CONFIG_KEY);
        }
    }

    @Test
    void getScriptFileWithScriptPathNotSetInConfiguration()
            throws Exception {
        Path actual = JRubyDelegate.getScriptFile();
        String code = Files.readString(actual);
        assertTrue(code.contains("CustomDelegate"));

    }

    //region Plugin methods

    @Test
    void getPluginConfigKeys() {
        assertEquals(Set.of(JRubyDelegate.PATHNAME_CONFIG_KEY),
                instance.getPluginConfigKeys());
    }

    @Test
    void getPluginName() {
        assertEquals(JRubyDelegate.class.getSimpleName(),
                instance.getPluginName());
    }

    //endregion
    //region Delegate methods

    /* authorize() */

    @Test
    void authorizeRaisingError() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("error"));
        instance.setRequestContext(context);

        assertThrows(DelegateException.class, () -> instance.authorize());
    }

    @Test
    void authorizeReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("allowed.jpg"));
        instance.setRequestContext(context);

        assertTrue((boolean) instance.authorize());
    }

    @Test
    void authorizeReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance.setRequestContext(context);

        assertFalse((boolean) instance.authorize());
    }

    @Test
    void authorizeReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect"));
        instance.setRequestContext(context);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) instance.authorize();
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* customizeIIIF1InformationResponse() */

    @Test
    void customizeIIIF1InformationResponse() throws Exception {
        Map<String,Object> info = new LinkedHashMap<>();
        instance.customizeIIIF1InformationResponse(info);
        assertEquals("new value", info.get("new_key"));
    }

    /* customizeIIIF2InformationResponse() */

    @Test
    void customizeIIIF2InformationResponse() throws Exception {
        Map<String,Object> info = new LinkedHashMap<>();
        instance.customizeIIIF2InformationResponse(info);
        assertEquals("new value", info.get("new_key"));
    }

    /* customizeIIIF3InformationResponse() */

    @Test
    void customizeIIIF3InformationResponse() throws Exception {
        Map<String,Object> info = new LinkedHashMap<>();
        instance.customizeIIIF3InformationResponse(info);
        assertEquals("new value", info.get("new_key"));
    }

    /* deserializeMetaIdentifier() */

    @Test
    void deserializeMetaIdentifier() throws Exception {
        Map<String,Object> result = instance.deserializeMetaIdentifier("whatever;3;2:3");
        assertEquals("whatever", result.get("identifier"));
        assertEquals(3L, result.get("page_number"));
        assertEquals(List.of(2L, 3L), result.get("scale_constraint"));
    }

    /* getFilesystemSourcePathname() */

    @Test
    void getFilesystemSourcePathnameReturningString() throws Exception {
        String result = instance.getFilesystemSourcePathname();
        assertEquals("cats", result);
    }

    @Test
    void getFilesystemSourcePathnameReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("missing"));
        instance.setRequestContext(context);

        assertNull(instance.getFilesystemSourcePathname());
    }

    /* getHTTPSourceResourceInfo() */

    @Test
    void getHTTPSourceResourceInfoReturningString() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("string"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHTTPSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/foxes", result.get("uri"));
    }

    @Test
    void getHTTPSourceResourceInfoReturningHash() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("hash"));
        instance.setRequestContext(context);

        Map<String,?> result = instance.getHTTPSourceResourceInfo();
        assertEquals(1, result.size());
        assertEquals("http://example.org/birds", result.get("uri"));
    }

    @Test
    void getHTTPSourceResourceInfoReturningNil() throws Exception {
        Map<String,?> result = instance.getHTTPSourceResourceInfo();
        assertTrue(result.isEmpty());
    }

    /* getMetadata() */

    @Test
    void getMetadata() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("metadata"));
        instance.setRequestContext(context);

        String result = instance.getMetadata();
        assertEquals("<rdf:RDF>variant metadata</rdf:RDF>", result);
    }

    /* getOverlayProperties() */

    @Test
    void getOverlayPropertiesReturningHash() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("image"));
        instance.setRequestContext(context);

        Map<String,Object> result = instance.getOverlayProperties();
        assertEquals(3, result.size());
    }

    @Test
    void getOverlayPropertiesReturningNil() throws Exception {
        assertEquals(0, instance.getOverlayProperties().size());
    }

    /* getRedactions() */

    @Test
    void getRedactions() throws Exception {
        instance.getRequestContext().setIdentifier(new Identifier("redacted"));
        List<Map<String,Object>> result = instance.getRedactions();
        assertEquals(1, result.size());
    }

    @Test
    void getRedactionsReturningEmptyArray() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("empty"));
        instance.setRequestContext(context);

        List<Map<String,Object>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    @Test
    void getRedactionsReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        List<Map<String,Object>> result = instance.getRedactions();
        assertTrue(result.isEmpty());
    }

    /* getSource() */

    @Test
    void getSource() throws Exception {
        assertEquals("FilesystemSource", instance.getSource());
    }

    @Test
    void getSourceReturningNil() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("bogus"));
        instance.setRequestContext(context);

        assertNull(instance.getSource());
    }

    /* authorizeBeforeAccess() */

    @Test
    void authorizeBeforeAccessReturningTrue() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("allowed.jpg"));
        instance.setRequestContext(context);

        assertTrue((boolean) instance.authorizeBeforeAccess());
    }

    @Test
    void authorizeBeforeAccessReturningFalse() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("forbidden-boolean.jpg"));
        instance.setRequestContext(context);

        assertFalse((boolean) instance.authorizeBeforeAccess());
    }

    @Test
    void authorizeBeforeAccessReturningMap() throws Exception {
        RequestContext context = new RequestContext();
        context.setIdentifier(new Identifier("redirect"));
        instance.setRequestContext(context);

        @SuppressWarnings("unchecked")
        Map<String,Object> result = (Map<String,Object>) instance.authorizeBeforeAccess();
        assertEquals("http://example.org/", result.get("location"));
        assertEquals(303, (long) result.get("status_code"));
    }

    /* serializeMetaIdentifier() */

    @Test
    void serializeMetaIdentifier() throws Exception {
        Map<String,Object> map = Map.of(
                "identifier", "whatever",
                "page_number", 3,
                "scale_constraint", List.of(2, 3));
        String result = instance.serializeMetaIdentifier(map);
        assertEquals("whatever;3;2:3", result);
    }

    /* setRequestContext() */

    @Test
    void setRequestContext() throws Exception {
        RequestContext context = new RequestContext();
        instance.setRequestContext(context);
        assertEquals(context, instance.getRequestContext());
    }

}
