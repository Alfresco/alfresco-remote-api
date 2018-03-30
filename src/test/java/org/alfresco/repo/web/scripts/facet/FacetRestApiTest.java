/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.web.scripts.facet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetService;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetServiceImpl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.collections.CollectionUtils;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * This class tests the ReST API of the {@link SolrFacetService}.
 * 
 * @author Neil Mc Erlean
 * @author Jamal Kaabi-Mofrad
 * @since 5.0
 */
public class FacetRestApiTest extends BaseWebScriptTest
{
    private static final String SEARCH_ADMIN_USER     = "searchAdmin";
    private static final String NON_SEARCH_ADMIN_USER = "nonSearchAdmin";

    private static final String FACETS = "facets";
    
    private final static String GET_ALL_FACETABLE_PROPERTIES_URL      = "/api/facet/facetable-properties";
    private final static String GET_SPECIFIC_FACETABLE_PROPERTIES_URL = "/api/facet/classes/{classname}/facetable-properties";
    private final static String GET_FACETS_URL       = "/api/facet/facet-config";
    private final static String PUT_FACET_URL_FORMAT = "/api/facet/facet-config/{0}?relativePos={1}";
    private final static String POST_FACETS_URL      = GET_FACETS_URL;
    private final static String PUT_FACETS_URL       = GET_FACETS_URL;

    private MutableAuthenticationService authenticationService;
    private AuthorityService             authorityService;
    private PersonService                personService;
    private RetryingTransactionHelper    transactionHelper;
    private List<String> filters = new ArrayList<String>();

    @Override protected void setUp() throws Exception
    {
        super.setUp();
        authenticationService = getServer().getApplicationContext().getBean("AuthenticationService", MutableAuthenticationService.class);
        authorityService      = getServer().getApplicationContext().getBean("AuthorityService", AuthorityService.class);
        personService         = getServer().getApplicationContext().getBean("PersonService", PersonService.class);
        transactionHelper     = getServer().getApplicationContext().getBean("retryingTransactionHelper", RetryingTransactionHelper.class);

        AuthenticationUtil.clearCurrentSecurityContext();
        // Create test users. TODO Create these users @BeforeClass or at a testsuite scope.
        AuthenticationUtil.runAsSystem(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                createUser(SEARCH_ADMIN_USER);
                createUser(NON_SEARCH_ADMIN_USER);

                if ( !authorityService.getContainingAuthorities(AuthorityType.GROUP,
                                                                SEARCH_ADMIN_USER,
                                                                true)
                                    .contains(SolrFacetServiceImpl.GROUP_ALFRESCO_SEARCH_ADMINISTRATORS_AUTHORITY))
                {
                    authorityService.addAuthority(SolrFacetServiceImpl.GROUP_ALFRESCO_SEARCH_ADMINISTRATORS_AUTHORITY,
                                                  SEARCH_ADMIN_USER);
                }
                return null;
            }
        });
    }

    @Override public void tearDown() throws Exception
    {
        super.tearDown();

        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                deleteFilters();
                return null;
            }
        }, SEARCH_ADMIN_USER);

        AuthenticationUtil.runAsSystem(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                transactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>()
                {
                    public Void execute() throws Throwable
                    {
                        deleteUser(SEARCH_ADMIN_USER);
                        deleteUser(NON_SEARCH_ADMIN_USER);
                        return null;
                    }
                });
                return null;
            }
        });
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    public void testNonSearchAdminUserCannotCreateUpdateSolrFacets() throws Exception
    {
        // Create a filter
        final ObjectNode filter = AlfrescoDefaultObjectMapper.createObjectNode();
        final String filterName = "filter" + System.currentTimeMillis();
        filters.add(filterName);
        filter.put("filterID", filterName);
        filter.put("facetQName", "cm:test1");
        filter.put("displayName", "facet-menu.facet.test1");
        filter.put("displayControl", "alfresco/search/FacetFilters/test1");
        filter.put("maxFilters", 5);
        filter.put("hitThreshold", 1);
        filter.put("minFilterValueLength", 4);
        filter.put("sortBy", "ALPHABETICALLY");

        // Non-Search-Admin tries to create a filter
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 403);
                return null;
            }
        }, NON_SEARCH_ADMIN_USER);

        // Search-Admin creates a filter
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 200);
                return null;
            }
        }, SEARCH_ADMIN_USER);

        // Non-Search-Admin tries to modify the filter
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                Response response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                ObjectNode jsonRsp = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                assertEquals(filterName, jsonRsp.get("filterID").textValue());
                assertEquals(5, jsonRsp.get("maxFilters").intValue());
                // Now change the maxFilters value and try to update
                jsonRsp.put("maxFilters", 10);
                sendRequest(new PutRequest(PUT_FACETS_URL, jsonRsp.toString(), "application/json"), 403);

                return null;
            }
        }, NON_SEARCH_ADMIN_USER);
    }

    public void testNonSearchAdminUserCanGetFacets() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                Response response = sendRequest(new GetRequest(GET_FACETS_URL), 200);
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                List<String> filters = getListFromJsonArray((ArrayNode) jsonRsp.get(FACETS));
                assertTrue(filters.size() > 0);
                return null;
            }
        }, NON_SEARCH_ADMIN_USER);
    }

    public void testSearchAdminCanGetFacets() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                Response rsp = sendRequest(new GetRequest(GET_FACETS_URL), 200);

                String contentAsString = rsp.getContentAsString();
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(contentAsString);

                // FIXME The JSON payload should be contained within a 'data' object.
                ArrayNode facetsArray = (ArrayNode)jsonRsp.get(FACETS);
                assertNotNull("JSON 'facets' array was null", facetsArray);

                // We'll not add any further assertions on the JSON content. If we've
                // got valid JSON at this point, then that's good enough.
                return null;
            }
        }, SEARCH_ADMIN_USER);
    }

    public void testSearchAdminReordersFacets() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                // get the existing facets.
                Response rsp = sendRequest(new GetRequest(GET_FACETS_URL), 200);

                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());

                final ArrayNode facetsArray = (ArrayNode)jsonRsp.get(FACETS);
                assertNotNull("JSON 'facets' array was null", facetsArray);

                System.out.println("Received " + facetsArray.size() + " facets");

                final List<String> idsIndexes = getListFromJsonArray(facetsArray);

                System.out.println(" IDs, indexes = " + idsIndexes);

                // Reorder them such that the last facet is moved left one place.
                assertTrue("There should be more than 1 built-in facet", facetsArray.size() > 1);

                final String lastIndexId = idsIndexes.get(idsIndexes.size() - 1);
                final String url = PUT_FACET_URL_FORMAT.replace("{0}", lastIndexId)
                                                       .replace("{1}", "-1");
                rsp = sendRequest(new PutRequest(url, "", "application/json"), 200);


                // Now get the facets back and we should see that one has moved.
                rsp = sendRequest(new GetRequest(GET_FACETS_URL), 200);

                jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());

                ArrayNode newfacetsArray = (ArrayNode)jsonRsp.get(FACETS);
                assertNotNull("JSON 'facets' array was null", newfacetsArray);

                System.out.println("Received " + newfacetsArray.size() + " facets");

                final List<String> newIdsIndexes = getListFromJsonArray(newfacetsArray);

                System.out.println(" IDs, indexes = " + newIdsIndexes);

                // Note here that the last Facet JSON object *is* moved one place up the list.
                assertEquals(CollectionUtils.moveLeft(1, lastIndexId, idsIndexes), newIdsIndexes);
                return null;
            }
        }, SEARCH_ADMIN_USER);
    }

    public void testDefaultValues() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Build the Filter object - ignore the optional values
                ObjectNode filter_one = AlfrescoDefaultObjectMapper.createObjectNode();
                String filterNameOne = "filterOne" + System.currentTimeMillis();
                filters.add(filterNameOne);
                filter_one.put("filterID", filterNameOne);
                filter_one.put("facetQName", "cm:test1");
                filter_one.put("displayName", "facet-menu.facet.test1");
                filter_one.put("displayControl", "alfresco/search/FacetFilters/test1");
                filter_one.put("maxFilters", 5);
                filter_one.put("hitThreshold", 1);
                filter_one.put("minFilterValueLength", 4);
                filter_one.put("sortBy", "ALPHABETICALLY");

                // Post the filter
                Response response = sendRequest(new PostRequest(POST_FACETS_URL, filter_one.toString(),"application/json"), 200);

                // Retrieve the created filter
                response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterNameOne), 200);
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                assertEquals(filterNameOne, jsonRsp.get("filterID").textValue());
                assertEquals("{http://www.alfresco.org/model/content/1.0}test1", jsonRsp.get("facetQName").textValue());
                assertEquals("facet-menu.facet.test1", jsonRsp.get("displayName").textValue());
                assertEquals("alfresco/search/FacetFilters/test1", jsonRsp.get("displayControl").textValue());
                assertEquals(5, jsonRsp.get("maxFilters").intValue());
                assertEquals(1, jsonRsp.get("hitThreshold").intValue());
                assertEquals(4, jsonRsp.get("minFilterValueLength").intValue());
                assertEquals("ALPHABETICALLY", jsonRsp.get("sortBy").textValue());
                // Check the Default values
                assertEquals("ALL", jsonRsp.get("scope").textValue());
                assertFalse(jsonRsp.get("isEnabled").booleanValue());
                assertFalse(jsonRsp.get("isDefault").booleanValue());

                // Build the Filter object with all the values
                ObjectNode filter_two = AlfrescoDefaultObjectMapper.createObjectNode();
                String filterNameTwo = "filterTwo" + System.currentTimeMillis();
                filters.add(filterNameTwo);
                filter_two.put("filterID", filterNameTwo);
                filter_two.put("facetQName", "cm:test2");
                filter_two.put("displayName", "facet-menu.facet.test2");
                filter_two.put("displayControl", "alfresco/search/FacetFilters/test2");
                filter_two.put("maxFilters", 5);
                filter_two.put("hitThreshold", 1);
                filter_two.put("minFilterValueLength", 4);
                filter_two.put("sortBy", "ALPHABETICALLY");
                filter_two.put("scope", "SCOPED_SITES");
                List<String> expectedValues = Arrays.asList(new String[] { "sit1", "site2", "site3" });
                filter_two.put("scopedSites", AlfrescoDefaultObjectMapper.convertValue(expectedValues, ArrayNode.class));
                filter_two.put("isEnabled", true);

                // Post the filter
                response = sendRequest(new PostRequest(POST_FACETS_URL, filter_two.toString(), "application/json"), 200);

                // Retrieve the created filter
                response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterNameTwo), 200);
                jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

                assertEquals(filterNameTwo, jsonRsp.get("filterID").textValue());
                assertEquals("SCOPED_SITES", jsonRsp.get("scope").textValue());
                assertTrue(jsonRsp.get("isEnabled").booleanValue());
                ArrayNode jsonArray = (ArrayNode) jsonRsp.get("scopedSites");
                List<String> retrievedValues = getListFromJsonArray(jsonArray);
                // Sort the list
                Collections.sort(retrievedValues);
                assertEquals(expectedValues, retrievedValues);

                return null;
            }
        }, SEARCH_ADMIN_USER);
    }

    public void testFacetCustomProperties() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Build the Filter object
                ObjectNode filter = AlfrescoDefaultObjectMapper.createObjectNode();
                String filterName = "filter" + System.currentTimeMillis();
                filters.add(filterName);
                filter.put("filterID", filterName);
                filter.put("facetQName", "cm:content.size.test");
                filter.put("displayName", "facet-menu.facet.size.test");
                filter.put("displayControl", "alfresco/search/FacetFilters/test");
                filter.put("maxFilters", 5);
                filter.put("hitThreshold", 1);
                filter.put("minFilterValueLength", 4);
                filter.put("sortBy", "ALPHABETICALLY");

                ObjectNode customProp = AlfrescoDefaultObjectMapper.createObjectNode();
                // 1st custom prop
                ObjectNode blockIncludeRequest = AlfrescoDefaultObjectMapper.createObjectNode();
                blockIncludeRequest.put("name", "blockIncludeFacetRequest");
                blockIncludeRequest.put("value", "true");
                customProp.put("blockIncludeFacetRequest", blockIncludeRequest);

                // 2nd custom prop
                ObjectNode multipleValue = AlfrescoDefaultObjectMapper.createObjectNode();
                multipleValue.put("name", "multipleValueTest");
                List<String> expectedValues = Arrays.asList(new String[] { "sit1", "site2", "site3" });
                multipleValue.put("value", AlfrescoDefaultObjectMapper.convertValue(expectedValues, ArrayNode.class));
                customProp.put("multipleValueTest", multipleValue);

                filter.put("customProperties", customProp);

                // Post the filter
                Response response = sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(),"application/json"), 200);
                // Retrieve the created filter
                response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                customProp = (ObjectNode) jsonRsp.get("customProperties");

                blockIncludeRequest = (ObjectNode) customProp.get("blockIncludeFacetRequest");
                assertEquals("{http://www.alfresco.org/model/solrfacetcustomproperty/1.0}blockIncludeFacetRequest", blockIncludeRequest.get("name").textValue());
                assertEquals("true", blockIncludeRequest.get("value").textValue());

                multipleValue = (ObjectNode) customProp.get("multipleValueTest");
                assertEquals("{http://www.alfresco.org/model/solrfacetcustomproperty/1.0}multipleValueTest", multipleValue.get("name").textValue());

                ArrayNode jsonArray = (ArrayNode) multipleValue.get("value");
                List<String> retrievedValues = getListFromJsonArray(jsonArray);
                // Sort the list
                Collections.sort(retrievedValues);
                assertEquals(expectedValues, retrievedValues);

                return null;
            }
        }, SEARCH_ADMIN_USER);
    }

    public void testCreateUpdateFacetWithInvalidFilterId() throws Exception
    {
        // Build the Filter object
        final ObjectNode filter = AlfrescoDefaultObjectMapper.createObjectNode();
        final String filterName = "filter" + System.currentTimeMillis();
        filters.add(filterName);
        filter.put("filterID", filterName);
        filter.put("facetQName", "cm:test1");
        filter.put("displayName", "facet-menu.facet.test1");
        filter.put("displayControl", "alfresco/search/FacetFilters/test1");
        filter.put("maxFilters", 5);
        filter.put("hitThreshold", 1);
        filter.put("minFilterValueLength", 4);
        filter.put("sortBy", "ALPHABETICALLY");

        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 200);
                return null;
            }
        }, SEARCH_ADMIN_USER);

        // Admin tries to change the FilterID value
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Retrieve the created filter
                Response response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                ObjectNode jsonRsp = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                assertEquals(filterName, jsonRsp.get("filterID").textValue());
                // Now change the filterID value and try to update
                jsonRsp.set("filterID", TextNode.valueOf(filterName + "Modified"));
                sendRequest(new PutRequest(PUT_FACETS_URL, jsonRsp.toString(), "application/json"), 400);

                return null;
            }
        }, SEARCH_ADMIN_USER);

        // Admin tries to create a filter with a duplicate FilterID
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 400);

                return null;
            }
        }, SEARCH_ADMIN_USER);

        // Admin tries to create a filter with a malicious FilterID
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                Response response = sendRequest(new GetRequest(GET_FACETS_URL), 200);
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                ArrayNode facetsArray = (ArrayNode) jsonRsp.get(FACETS);
                assertNotNull("JSON 'facets' array was null", facetsArray);
                final List<String> facets = getListFromJsonArray(facetsArray);

                filter.put("filterID", "<script>alert('Maliciouse-FilterID')</script>");
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 400);

                // Retrieve all filters
                response = sendRequest(new GetRequest(GET_FACETS_URL), 200);
                jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
                facetsArray = (ArrayNode) jsonRsp.get(FACETS);

                assertNotNull("JSON 'facets' array was null", facetsArray);
                final List<String> newFacets = getListFromJsonArray(facetsArray);
                assertEquals(facets, newFacets);

                return null;
            }
        }, SEARCH_ADMIN_USER);

    }
    
    /** The REST API should accept both 'cm:name' and '{http://www.alfresco.org/model/content/1.0}name' forms of filter IDs. */
    public void testCreateFacetWithLongFormQnameFilterId() throws Exception
    {
        final ObjectNode filter = AlfrescoDefaultObjectMapper.createObjectNode();
        final String filterName = "filter" + GUID.generate();
        filters.add(filterName);
        filter.put("filterID", filterName);
        // This is the long-form qname that needs to be acceptable.
        filter.put("facetQName", "{http://www.alfresco.org/model/content/1.0}testLongQname");
        filter.put("displayName", "facet-menu.facet.testLongQname");
        filter.put("displayControl", "alfresco/search/FacetFilters/testLongQname");
        filter.put("maxFilters", 5);
        filter.put("hitThreshold", 1);
        filter.put("minFilterValueLength", 4);
        filter.put("sortBy", "ALPHABETICALLY");
        
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 200);
                return null;
            }
        }, SEARCH_ADMIN_USER);
    }
    
    public void testUpdateSingleValue() throws Exception
    {
        // Build the Filter object
        final ObjectNode filter = AlfrescoDefaultObjectMapper.createObjectNode();
        final String filterName = "filter" + System.currentTimeMillis();
        filters.add(filterName);
        filter.put("filterID", filterName);
        filter.put("facetQName", "cm:test");
        filter.put("displayName", "facet-menu.facet.test1");
        filter.put("displayControl", "alfresco/search/FacetFilters/test");
        filter.put("maxFilters", 5);
        filter.put("hitThreshold", 1);
        filter.put("minFilterValueLength", 4);
        filter.put("sortBy", "ALPHABETICALLY");
        filter.put("isEnabled", true);

        ObjectNode customProp = AlfrescoDefaultObjectMapper.createObjectNode();
        // 1st custom prop
        ObjectNode blockIncludeRequest = AlfrescoDefaultObjectMapper.createObjectNode();
        blockIncludeRequest.put("name", "blockIncludeFacetRequest");
        blockIncludeRequest.put("value", "true");
        customProp.put("blockIncludeFacetRequest", blockIncludeRequest);
        filter.put("customProperties", customProp);

        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Post the filter
                sendRequest(new PostRequest(POST_FACETS_URL, filter.toString(), "application/json"), 200);
                return null;
            }
        }, SEARCH_ADMIN_USER);

        // Admin updates displayName and facetQName in 2 put requests
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override
            public Void doWork() throws Exception
            {
                // Retrieve the created filter
                Response response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

                assertEquals(filterName, jsonRsp.get("filterID").textValue());
                assertEquals("facet-menu.facet.test1", jsonRsp.get("displayName").textValue());
                assertEquals("{http://www.alfresco.org/model/content/1.0}test", jsonRsp.get("facetQName").textValue());
                assertTrue(jsonRsp.get("isEnabled").booleanValue());

                // Just supply the filterID and the required value
                ObjectNode singleValueJson = AlfrescoDefaultObjectMapper.createObjectNode();
                singleValueJson.put("filterID", filterName);
                // Change the displayName value and update
                singleValueJson.put("displayName", "facet-menu.facet.modifiedValue");
                sendRequest(new PutRequest(PUT_FACETS_URL, singleValueJson.toString(), "application/json"), 200);

                // Change the isEnabled value and update
                // We simulate two PUT requests without refreshing the page in
                // between updates
                singleValueJson = AlfrescoDefaultObjectMapper.createObjectNode();
                singleValueJson.put("filterID", filterName);
                singleValueJson.put("isEnabled", false);
                sendRequest(new PutRequest(PUT_FACETS_URL, singleValueJson.toString(), "application/json"), 200);

                response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

                // Now see if the two changes have been persisted
                assertEquals("facet-menu.facet.modifiedValue", jsonRsp.get("displayName").textValue());
                assertFalse(jsonRsp.get("isEnabled").booleanValue());
                // Make sure the rest of values haven't been changed
                assertEquals(filterName, jsonRsp.get("filterID").textValue());
                assertEquals("{http://www.alfresco.org/model/content/1.0}test", jsonRsp.get("facetQName").textValue());
                assertEquals("alfresco/search/FacetFilters/test", jsonRsp.get("displayControl").textValue());
                assertEquals(5, jsonRsp.get("maxFilters").intValue());
                assertEquals(1, jsonRsp.get("hitThreshold").intValue());
                assertEquals(4, jsonRsp.get("minFilterValueLength").intValue());
                assertEquals("ALPHABETICALLY", jsonRsp.get("sortBy").textValue());
                assertEquals("ALL", jsonRsp.get("scope").textValue());
                assertFalse(jsonRsp.get("isDefault").booleanValue());
                // Make sure custom properties haven't been deleted
                JsonNode retrievedCustomProp = jsonRsp.get("customProperties");
                JsonNode retrievedBlockIncludeRequest = retrievedCustomProp.get("blockIncludeFacetRequest");
                assertEquals("{http://www.alfresco.org/model/solrfacetcustomproperty/1.0}blockIncludeFacetRequest", retrievedBlockIncludeRequest.get("name").textValue());
                assertEquals("true", retrievedBlockIncludeRequest.get("value").textValue());

                // Change the facetQName value and update
                singleValueJson = AlfrescoDefaultObjectMapper.createObjectNode();
                singleValueJson.put("filterID", filterName);
                singleValueJson.put("facetQName", "cm:testModifiedValue");
                // We simulate that 'testModifiedValue' QName doesn't have custom properties
                singleValueJson.put("customProperties", AlfrescoDefaultObjectMapper.createObjectNode());
                sendRequest(new PutRequest(PUT_FACETS_URL, singleValueJson.toString(), "application/json"), 200);

                response = sendRequest(new GetRequest(GET_FACETS_URL + "/" + filterName), 200);
                jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

                // Now see if the facetQName and its side-effect have been persisted
                assertEquals("{http://www.alfresco.org/model/content/1.0}testModifiedValue",jsonRsp.get("facetQName").textValue());
                assertNull("Custom properties should have been deleted.", jsonRsp.get("customProperties"));
                // Make sure the rest of values haven't been changed
                assertEquals(filterName, jsonRsp.get("filterID").textValue());
                assertEquals("facet-menu.facet.modifiedValue", jsonRsp.get("displayName").textValue());
                assertEquals("alfresco/search/FacetFilters/test", jsonRsp.get("displayControl").textValue());
                assertEquals(5, jsonRsp.get("maxFilters").intValue());
                assertEquals(1, jsonRsp.get("hitThreshold").intValue());
                assertEquals(4, jsonRsp.get("minFilterValueLength").intValue());
                assertEquals("ALPHABETICALLY", jsonRsp.get("sortBy").textValue());
                assertFalse(jsonRsp.get("isDefault").booleanValue());
                assertEquals("ALL", jsonRsp.get("scope").textValue());
                assertFalse(jsonRsp.get("isEnabled").booleanValue());

                return null;
            }
        }, SEARCH_ADMIN_USER);
    }
    
    public void testGetAllFacetableProperties() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                final Response rsp = sendRequest(new GetRequest(GET_ALL_FACETABLE_PROPERTIES_URL), 200);
                
                // For now, we'll only perform limited testing of the response as we primarily
                // want to know that the GET call succeeded and that it correctly identified
                // *some* facetable properties.
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());
                
                JsonNode data = jsonRsp.get("data");
                ArrayNode properties = (ArrayNode) data.get(FacetablePropertiesGet.PROPERTIES_KEY);
                
                final int arbitraryLimit = 25;
                assertTrue("Expected 'many' properties, but found 'not very many'", properties.size() > arbitraryLimit);
                
                return null;
            }
        }, SEARCH_ADMIN_USER);
    }
    
    public void testGetFacetablePropertiesForSpecificContentClasses() throws Exception
    {
        AuthenticationUtil.runAs(new RunAsWork<Void>()
        {
            @Override public Void doWork() throws Exception
            {
                final Response rsp = sendRequest(new GetRequest(GET_SPECIFIC_FACETABLE_PROPERTIES_URL.replace("{classname}", "cm:content")), 200);
                
                // For now, we'll only perform limited testing of the response as we primarily
                // want to know that the GET call succeeded and that it correctly identified
                // *some* facetable properties.
                JsonNode jsonRsp = AlfrescoDefaultObjectMapper.getReader().readTree(rsp.getContentAsString());
                
                JsonNode data = jsonRsp.get("data");
                ArrayNode properties = (ArrayNode) data.get(FacetablePropertiesGet.PROPERTIES_KEY);
                
                final int arbitraryLimit = 100;
                assertTrue("Expected 'not very many' properties, but found 'many'", properties.size() < arbitraryLimit);
                
                return null;
            }
        }, SEARCH_ADMIN_USER);
    }
    
    private List<String> getListFromJsonArray(ArrayNode facetsArray)
    {
        List<String> result = new ArrayList<>();

        for (int i = 0; i < facetsArray.size(); i++)
        {
            Object object = facetsArray.get(i);
            if (object instanceof ObjectNode)
            {
                final JsonNode filterIdJson = ((JsonNode) object).get("filterID");
                final String nextId = filterIdJson == null ? null : filterIdJson.textValue();
                result.add(nextId);
            }
            else
            {
                result.add(((JsonNode) object).textValue());
            }
        }
        return result;
    }

    private void createUser(String userName)
    {
        if (! authenticationService.authenticationExists(userName))
        {
            authenticationService.createAuthentication(userName, "PWD".toCharArray());
        }

        if (! personService.personExists(userName))
        {
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");

            personService.createPerson(ppOne);
        }
    }

    private void deleteUser(String userName)
    {
        if (personService.personExists(userName))
        {
            personService.deletePerson(userName);
        }
    }

    private void deleteFilters() throws IOException
    {
        for (String filter : filters)
        {
            sendRequest(new DeleteRequest(GET_FACETS_URL + "/" + filter), 200);
        }
    }
}
