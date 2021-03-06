/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.forgerock.api.models.ApiDescription;
import org.forgerock.http.ApiProducer;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.LocaleContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.i18n.LocalizableString;
import org.forgerock.util.i18n.PreferredLocales;
import org.forgerock.util.test.assertj.Conditions;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchema;


public class SmsResourceProviderTest {

    private static final String RESOLVED_REALM = "resolvedRealm";

    /* This class currently only tests a small number of methods in the SmsResourceProvider Class
     * Do not presume that the current set of mock objects are suitably configured to test additional methods
     */
    @Mock
    private ServiceSchema serviceSchema;
    @Mock
    private SchemaType schemaType;
    @Mock
    private List<ServiceSchema> subSchemaPath;
    private String uriPath = "";
    @Mock
    private SmsJsonConverter jsonConverter;
    @Mock
    private Debug debug;
    @Mock
    private Context mockContext;
    @Mock
    private RealmContext mockRealmContext;
    @Mock
    private LocaleContext localeContext;

    @Mock
    private ServiceConfigManager mockServiceConfigManager;

    @Mock
    private ServiceConfig mockServiceConfig;
    @Mock
    private ServiceConfig mockServiceSubConfig;
    private RealmTestHelper realmTestHelper;

    private Locale local = new Locale("Test");

    private LocaleContext context = new LocaleContext(new HttpContext(
            json(object(field("headers", object()), field("parameters", object()))),
            this.getClass().getClassLoader())) {
        @Override
        public Locale getLocale() {
            return Locale.UK;
        }
    };

    @BeforeMethod
    public void initMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(localeContext.getLocale()).thenReturn(local);
        when(mockContext.asContext(LocaleContext.class)).thenReturn(localeContext);

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        Realm realm = realmTestHelper.mockRealm(RESOLVED_REALM);

        when(mockContext.getParent()).thenReturn(new RootContext());
        when(mockContext.containsContext(RealmContext.class)).thenReturn(true);
        when(mockContext.asContext(RealmContext.class)).thenReturn(mockRealmContext);
        when(mockRealmContext.getRealm()).thenReturn(realm);
        when(mockServiceConfigManager.getOrganizationConfig("/" + RESOLVED_REALM, null)).thenReturn(mockServiceConfig);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void verifyResourceProviderIsNotNull() {
        SmsResourceProvider resourceProvider = new MySmsResourceProvider(serviceSchema, schemaType, subSchemaPath,
                uriPath, true, jsonConverter, debug);

        assertThat(resourceProvider).isNotNull();
    }

    @Test
    public void verifyRelmForReturnsRealmWhenItIsContinaedWithinTheContext() {

        // Given
        SmsResourceProvider resourceProvider = new MySmsResourceProvider(serviceSchema, schemaType, subSchemaPath,
                uriPath, true, jsonConverter, debug);

        // When
        String returnedRealm = resourceProvider.realmFor(mockContext);

        // Then
        assertThat(returnedRealm).isEqualTo("/" + RESOLVED_REALM);
    }

    @Test
    public void verifyTypeAction() throws Exception {

        // Given
        when(serviceSchema.getI18NFileName()).thenReturn("org/forgerock/openam/core/rest/sms/SmsResourceProviderTest");
        when(serviceSchema.getResourceName()).thenReturn("one");
        when(serviceSchema.getI18NKey()).thenReturn("subOne");
        when(serviceSchema.supportsMultipleConfigurations()).thenReturn(false);

        SmsResourceProvider resourceProvider = new MySmsResourceProvider(serviceSchema, schemaType, subSchemaPath,
                uriPath, true, jsonConverter, debug);

        // When
        JsonValue returnedJV = resourceProvider.getType(mock(Context.class)).getOrThrow().getJsonContent();

        // Then
        AssertJJsonValueAssert.assertThat(returnedJV)
                .isObject()
                .stringIs("_id", Conditions.equalTo("one"))
                .booleanAt("collection").isFalse();
        Object name = returnedJV.get("name").getObject();
        assertThat(name).isInstanceOf(LocalizableString.class);
        assertThat(((LocalizableString) name).toTranslatedString(new PreferredLocales())).isEqualTo("Sub Schema One");
    }

    @Test
    public void fetchingParentSubConfigWhichExistsInRealmReturnsSubConfig() throws Exception {

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(true);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager)).isEqualTo(mockServiceConfig);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void fetchingParentSubConfigWhichDoesntExistInRealmThrowsException() throws Exception {

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(false);

        rp.parentSubConfigFor(mockContext, mockServiceConfigManager);
    }

    @Test
    public void fetchingParentSubConfigWhichExistsFromSubSchemaWithResourceNameReturnsConfig() throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn("resourceName");
        when(mockServiceConfig.getSubConfig("resourceName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(true);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager))
                .isEqualTo(mockServiceSubConfig);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void fetchingParentSubConfigWhichDoesntExistFromSubSchemaWithResourceNameThrowsException() throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn("resourceName");
        when(mockServiceConfig.getSubConfig("resourceName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(false);

        rp.parentSubConfigFor(mockContext, mockServiceConfigManager);
    }

    @Test
    public void fetchingParentSubConfigWhichExistsFromSubSchemaWithoutResourceNameReturnsConfig() throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn(SmsRequestHandler.USE_PARENT_PATH);
        when(branch.getName()).thenReturn("schemaName");
        when(mockServiceConfig.getSubConfig("schemaName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(true);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager))
                .isEqualTo(mockServiceSubConfig);
    }

    @Test
    public void fetchingParentSubConfigWhichDoesntExistFromSubSchemaWithoutResourceReturnsSubConfig()
            throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                uriPath, false, jsonConverter, debug);

        UriRouterContext urc = new UriRouterContext(mockContext, "/uri", "", Collections.<String, String>emptyMap());

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn(SmsRequestHandler.USE_PARENT_PATH);
        when(branch.getName()).thenReturn("schemaName");
        when(mockServiceConfig.getSubConfig("schemaName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(false);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager))
                .isEqualTo(mockServiceSubConfig);
    }

    @Test
    public void fetchingParentSubConfigWhichExistsFromSubSchemaWithPlaceholderResourceNameReturnsSubConfig()
            throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                "/{placeholder}", false, jsonConverter, debug);

        Map<String, String> uriTemplaceVariables = new HashMap<String, String>();
        uriTemplaceVariables.put("placeholder", "subConfigName");
        UriRouterContext urc = new UriRouterContext(mockContext, "/{placeholder}", "", uriTemplaceVariables);

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockContext.containsContext(UriRouterContext.class)).thenReturn(true);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn("placeholder");
        when(mockServiceConfig.getSubConfig("subConfigName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(true);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager))
                .isEqualTo(mockServiceSubConfig);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void fetchingParentSubConfigWhichDoenstExistFromSubSchemaWithPlaceholderResourceNameThrowsException()
            throws Exception {

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                "/{placeholder}", false, jsonConverter, debug);

        Map<String, String> uriTemplaceVariables = new HashMap<String, String>();
        uriTemplaceVariables.put("placeholder", "subConfigName");
        UriRouterContext urc = new UriRouterContext(mockContext, "/{placeholder}", "", uriTemplaceVariables);

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockContext.containsContext(UriRouterContext.class)).thenReturn(true);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn("placeholder");
        when(mockServiceConfig.getSubConfig("subConfigName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(false);

        rp.parentSubConfigFor(mockContext, mockServiceConfigManager);
    }

    @Test
    public void fetchingParentSubConfigWhichExistsFromSubSchemaWithPlaceholderSchemaNameReturnsSubConfig()
            throws Exception {
        String resolvedRealm = "resolvedRealm";

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                "/{placeholder}", false, jsonConverter, debug);

        Map<String, String> uriTemplaceVariables = new HashMap<String, String>();
        uriTemplaceVariables.put("placeholder", "subConfigName");
        UriRouterContext urc = new UriRouterContext(mockContext, "/{placeholder}", "", uriTemplaceVariables);

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockContext.containsContext(UriRouterContext.class)).thenReturn(true);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn(SmsRequestHandler.USE_PARENT_PATH);
        when(branch.getName()).thenReturn("placeholder");
        when(mockServiceConfig.getSubConfig("subConfigName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(true);

        assertThat(rp.parentSubConfigFor(mockContext, mockServiceConfigManager))
                .isEqualTo(mockServiceSubConfig);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void fetchingParentSubConfigWhichDoenstExistFromSubSchemaWithPlaceholderSchemaNameThrowsException()
            throws Exception {
        String resolvedRealm = "resolvedRealm";

        ServiceSchema branch = Mockito.mock(ServiceSchema.class);
        ServiceSchema leaf = Mockito.mock(ServiceSchema.class);

        List<ServiceSchema> subSchemaPath = Arrays.asList(branch, leaf);

        SmsResourceProvider rp = new MySmsResourceProvider(serviceSchema, SchemaType.ORGANIZATION, subSchemaPath,
                "/{placeholder}", false, jsonConverter, debug);

        Map<String, String> uriTemplaceVariables = new HashMap<String, String>();
        uriTemplaceVariables.put("placeholder", "subConfigName");
        UriRouterContext urc = new UriRouterContext(mockContext, "/{placeholder}", "", uriTemplaceVariables);

        when(mockContext.asContext(UriRouterContext.class)).thenReturn(urc);
        when(mockContext.containsContext(UriRouterContext.class)).thenReturn(true);
        when(mockServiceConfig.exists()).thenReturn(true);

        when(branch.getResourceName()).thenReturn(SmsRequestHandler.USE_PARENT_PATH);
        when(branch.getName()).thenReturn("placeholder");
        when(mockServiceConfig.getSubConfig("subConfigName")).thenReturn(mockServiceSubConfig);
        when(mockServiceSubConfig.exists()).thenReturn(false);

        rp.parentSubConfigFor(mockContext, mockServiceConfigManager);
    }

    /**
     * A Local implementation of the abstract class will allow us to test the protected methods.
     */
    private class MySmsResourceProvider extends SmsResourceProvider {

        MySmsResourceProvider(ServiceSchema schema, SchemaType type, List<ServiceSchema> subSchemaPath, String uriPath,
                              boolean serviceHasInstanceName, SmsJsonConverter converter, Debug debug) {
            super(schema, type, subSchemaPath, uriPath, serviceHasInstanceName, converter, debug,
                    AMResourceBundleCache.getInstance(), Locale.UK);
        }

        @Override
        public ApiDescription api(ApiProducer<ApiDescription> apiProducer) {
            return null;
        }

        @Override
        public ApiDescription handleApiRequest(Context context, Request request) {
            return null;
        }

        @Override
        public void addDescriptorListener(Listener listener) {

        }

        @Override
        public void removeDescriptorListener(Listener listener) {

        }
    }
}