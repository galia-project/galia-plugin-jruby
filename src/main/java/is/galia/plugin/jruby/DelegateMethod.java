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

/**
 * Enumeration of all delegate methods recognized by the application. Note that
 * plugin modules may use their own delegate methods which aren't defined here.
 */
enum DelegateMethod {

    AUTHORIZE("authorize"),
    AUTHORIZE_BEFORE_ACCESS("authorize_before_access"),
    CUSTOMIZE_IIIF1_INFORMATION_RESPONSE("customize_iiif1_information_response"),
    CUSTOMIZE_IIIF2_INFORMATION_RESPONSE("customize_iiif2_information_response"),
    CUSTOMIZE_IIIF3_INFORMATION_RESPONSE("customize_iiif3_information_response"),
    DESERIALIZE_META_IDENTIFIER("deserialize_meta_identifier"),
    FILESYSTEMSOURCE_PATHMAME("filesystemsource_pathname"),
    HTTPSOURCE_RESOURCE_INFO("httpsource_resource_info"),
    METADATA("metadata"),
    OVERLAY("overlay"),
    REDACTIONS("redactions"),
    SERIALIZE_META_IDENTIFIER("serialize_meta_identifier"),
    SOURCE("source");

    private final String methodName;

    DelegateMethod(String methodName) {
        this.methodName = methodName;
    }

    /**
     * @return Name of the delegate method.
     */
    String getMethodName() {
        return methodName;
    }

    /**
     * @return Name of the delegate method.
     */
    @Override
    public String toString() {
        return methodName;
    }

}
