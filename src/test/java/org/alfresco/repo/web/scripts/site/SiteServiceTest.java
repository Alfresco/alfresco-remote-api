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
package org.alfresco.repo.web.scripts.site;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.AssertionFailedError;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.alfresco.util.testing.category.LuceneTests;
import org.junit.experimental.categories.Category;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Unit test to test site Web Script API of the Site Object.
 * 
 * @author Roy Wetherall
 */
@Category(LuceneTests.class)
public class SiteServiceTest extends AbstractSiteServiceTest
{    
    private SiteService siteService;
    private NodeService nodeService;
    private PermissionService permissionService;
    private AuthorityService authorityService;
    private FileFolderService fileFolderService;
    
    private static final String USER_ONE = "SiteTestOne";
    private static final String USER_TWO = "SiteTestTwo";
    private static final String USER_THREE = "SiteTestThree";
    private static final String USER_NUMERIC = "1234567890";
    private static final String USER_FOUR_AS_SITE_ADMIN = "SiteAdmin";
    
    private static final String URL_SITES = "/api/sites";
    private static final String URL_SITES_QUERY = URL_SITES + "/query";
    private static final String URL_MEMBERSHIPS = "/memberships";
    private static final String URL_SITES_ADMIN = "/api/admin-sites";
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.siteService = (SiteService)getServer().getApplicationContext().getBean("SiteService");
        this.nodeService = (NodeService)getServer().getApplicationContext().getBean("NodeService");
        this.permissionService = (PermissionService)getServer().getApplicationContext().getBean("PermissionService");
        this.authorityService = (AuthorityService)getServer().getApplicationContext().getBean("AuthorityService");
        this.fileFolderService = (FileFolderService)getServer().getApplicationContext().getBean("FileFolderService");

        
        // Create users
        createUser(USER_ONE);
        createUser(USER_TWO);
        createUser(USER_THREE);
        createUser(USER_NUMERIC);
        createUser(USER_FOUR_AS_SITE_ADMIN);
        
        // Add user four as a member of the site admins group
        authorityService.addAuthority("GROUP_SITE_ADMINISTRATORS", USER_FOUR_AS_SITE_ADMIN);
        
        // Do tests as user one
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        
        // Clear the users
        deleteUser(USER_ONE);
        deleteUser(USER_TWO);
        deleteUser(USER_THREE);
        deleteUser(USER_NUMERIC);
        deleteUser(USER_FOUR_AS_SITE_ADMIN);
        
        //Delete the sites
        deleteSites();
    }

    public void testCreateSite() throws Exception
    {
        String shortName  = GUID.generate();
        
        // Create a new site
        JsonNode result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);        
        assertEquals("myPreset", result.get("sitePreset"));
        assertEquals(shortName, result.get("shortName"));
        assertEquals("myTitle", result.get("title"));
        assertEquals("myDescription", result.get("description"));
        assertNotNull(result.get("node"));
        assertNotNull(result.get("tagScope"));
        assertEquals(SiteVisibility.PUBLIC.toString(), result.get("visibility"));
        assertTrue(result.get("isPublic").booleanValue());
        
        // Check for duplicate names
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 400); 
    }
    
    public void testGetSites() throws Exception
    {
    	int preexistingSiteCount = 0;
        Response response;
		ArrayNode result;
		try {
			response = sendRequest(new GetRequest(URL_SITES), 200);        
			result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());        
			assertNotNull(result);
			preexistingSiteCount = result.size();
		} catch (AssertionFailedError e) {
			//We don't mind if the first call fails, it's the rest of the calls that are important
			assertEquals(0,preexistingSiteCount);
		}
        
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", GUID.generate(), "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        response = sendRequest(new GetRequest(URL_SITES), 200);        
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());        
        assertNotNull(result);
        assertEquals("Wrong site count", preexistingSiteCount + 5, result.size());
        
        response = sendRequest(new GetRequest(URL_SITES + "?size=3"), 200);        
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());        
        assertNotNull(result);
        assertEquals("Wrong site count (?size=3)", 3, result.size());        

        response = sendRequest(new GetRequest(URL_SITES + "?size=13"), 200);        
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());        
        assertNotNull(result);
        assertEquals("Wrong site count (?size=13)", Math.min(13, preexistingSiteCount + 5), result.size());
    }
    
    /**
     * https://issues.alfresco.com/jira/browse/JAWS-456
     */
    public void testQuerySites() throws Exception
    {
        // Generate the short names of the sites
        String[] shortNames = new String[]{GUID.generate(), GUID.generate(), GUID.generate(), GUID.generate(), GUID.generate()};
        
        // Create the sites
        createSite("myPreset", shortNames[0], "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", shortNames[1], "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", shortNames[2], "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", shortNames[3], "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", shortNames[4], "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // build query json
        ObjectNode shortNameQuery = AlfrescoDefaultObjectMapper.createObjectNode();
        shortNameQuery.put("match", "exact");
        ArrayNode valuesArray = AlfrescoDefaultObjectMapper.createArrayNode();
        valuesArray.add(shortNames[0]);
        valuesArray.add(shortNames[2]);
        valuesArray.add(shortNames[4]);
        valuesArray.add("bobbins");
        shortNameQuery.set("values", valuesArray);
        ObjectNode query = AlfrescoDefaultObjectMapper.createObjectNode();
        query.set("shortName", shortNameQuery);
        
        // execute site query
        Response response = sendRequest(new PostRequest(URL_SITES_QUERY, query.toString(), "application/json"), 200);
        ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // check we have the results we expect
        assertEquals(3, result.size());
        Set<String> resultSet = new HashSet<String>();
        for (int i=0; i<result.size(); i++)
        {
           resultSet.add(result.get(i).get("shortName").textValue());
        }
        assertTrue(resultSet.contains(shortNames[0]));
        assertFalse(resultSet.contains(shortNames[1]));
        assertTrue(resultSet.contains(shortNames[2]));
        assertFalse(resultSet.contains(shortNames[3]));
        assertTrue(resultSet.contains(shortNames[4]));
        assertFalse(resultSet.contains("bobbins"));
        
        // sample one of the returned sites and check it's what we expect
        JsonNode site = result.get(0);
        assertNotNull(site);
        assertEquals("myPreset", site.get("sitePreset"));
        assertEquals("myTitle", site.get("title"));
        assertEquals("myDescription", site.get("description"));
        assertNotNull(site.get("node"));
        assertNotNull(site.get("tagScope"));
        assertTrue(site.get("isPublic").booleanValue());
    }
    
    public void testGetSite() throws Exception
    {
        // Get a site that doesn't exist
        sendRequest(new GetRequest(URL_SITES + "/" + "somerandomshortname"), 404);
        
        // Create a site and get it
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
       
    }
    
    public void testUpdateSite() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        ObjectNode result = (ObjectNode) createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Update the site
        result.put("title", "abs123abc");
        result.put("description", "123abc123");
        result.put("visibility", SiteVisibility.PRIVATE.toString());
        Response response = sendRequest(new PutRequest(URL_SITES + "/" + shortName, result.toString(), "application/json"), 200);
        result = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals("abs123abc", result.get("title"));
        assertEquals("123abc123", result.get("description"));
        assertFalse(result.get("isPublic").booleanValue());
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));
        
        // Try and get the site and double check it's changed
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
        result = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals("abs123abc", result.get("title"));
        assertEquals("123abc123", result.get("description"));
        assertFalse(result.get("isPublic").booleanValue());
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));
    }
    
    public void testDeleteSite() throws Exception
    {
        // Delete non-existent site
        sendRequest(new DeleteRequest(URL_SITES + "/" + "somerandomshortname"), 404);
        
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Get the site
        sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
        
        // Delete the site
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 200);
        
        // Get the site
        sendRequest(new GetRequest(URL_SITES + "/" + shortName), 404);
    }
    
    public void testGetMemberships() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Check the memberships
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);
        ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());        
        assertNotNull(result);
        assertEquals(1, result.size());
        JsonNode membership = result.get(0);
        assertEquals(SiteModel.SITE_MANAGER, membership.get("role"));
        assertEquals(USER_ONE, membership.get("authority").get("userName"));        
    }
    
    public void testPostMemberships() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.put("person", person);
        
        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        
        // Check the result
        assertEquals(SiteModel.SITE_CONSUMER, membership.get("role"));
        assertEquals(USER_TWO, membership.get("person").get("userName")); 
        
        // Get the membership list
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);   
        ArrayNode result2 = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertNotNull(result2);
        assertEquals(2, result2.size());
        
        
        // Add another user, with a fully numeric username
        membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONTRIBUTOR);
        person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_NUMERIC);
        membership.set("person", person);
        response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        
        // Check the details are correct for one user
        membership = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(SiteModel.SITE_CONTRIBUTOR, membership.get("role"));
        assertEquals(USER_NUMERIC, membership.get("authority").get("userName"));
        
        
        // Check the membership list
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);
        String json = response.getContentAsString();
        ArrayNode result3 = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(json);
        assertNotNull(result3);
        assertEquals(3, result3.size());
        
        // Check the everyone has the correct membership
        // (The webscript returns the users in order to make testing easier)
        Map<String, JsonNode> membershipMap = new HashMap<String, JsonNode>();
        for (int i = 0 ; i < membership.size(); i++)
        {
            membershipMap.put(result3.get(i).get("role").textValue(), result3.get(i));
        }

        membership = (ObjectNode) membershipMap.get(SiteModel.SITE_MANAGER);
        assertNotNull("The response did not contain " + SiteModel.SITE_MANAGER, membership);
        assertEquals(USER_ONE, membership.get("authority").get("userName"));
        
        membership = (ObjectNode) membershipMap.get(SiteModel.SITE_CONSUMER);
        assertNotNull("The response did not contain " + SiteModel.SITE_CONSUMER, membership);
        assertEquals(USER_TWO, membership.get("authority").get("userName"));
        
        membership = (ObjectNode) membershipMap.get(SiteModel.SITE_CONTRIBUTOR);
        assertNotNull("The response did not contain " + SiteModel.SITE_CONTRIBUTOR, membership);
        assertEquals(USER_NUMERIC, membership.get("authority").get("userName"));
    }
    
    public void testGetMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Test error conditions
        sendRequest(new GetRequest(URL_SITES + "/badsite" + URL_MEMBERSHIPS + "/" + USER_ONE), 404);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/baduser"), 404);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 404);
        
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_ONE), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // Check the result
        assertEquals(SiteModel.SITE_MANAGER, result.get("role"));
        assertEquals(USER_ONE, result.get("authority").get("userName")); 
    }
    
    public void testPutMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
                
        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);
        
        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        ObjectNode newMember = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // Update the role by returning the data.
        newMember.put("role", SiteModel.SITE_COLLABORATOR);
        response = sendRequest(new PutRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, newMember.toString(), "application/json"), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        // Check the result
        assertEquals(SiteModel.SITE_COLLABORATOR, result.get("role"));
        assertEquals(USER_TWO, result.get("authority").get("userName"));
        
        // Double check and get the membership for user two
        response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 200);
        result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(SiteModel.SITE_COLLABORATOR, result.get("role"));
        assertEquals(USER_TWO, result.get("authority").get("userName"));
        
    }
    
    public void testGroupMembership() throws Exception
    {
        String testGroup = "SiteServiceTestGroupA";
        String testGroupName = "GROUP_" + testGroup;
        
        if(!authorityService.authorityExists(testGroupName))
        {
            this.authenticationComponent.setSystemUserAsCurrentUser();
             
            testGroupName = authorityService.createAuthority(AuthorityType.GROUP, testGroup, testGroup, authorityService.getDefaultZones());
        }       
         
        this.authenticationComponent.setCurrentUser(USER_ONE);

        // CRUD a membership group for a web site
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
                
        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode group = AlfrescoDefaultObjectMapper.createObjectNode();
        group.put("fullName", testGroupName);
        membership.set("group", group);
        
        // Create a new group membership
        {
            Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
            ObjectNode newMember = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
            // Validate the return value
            assertEquals("role not correct", SiteModel.SITE_CONSUMER, newMember.get("role"));
            JsonNode newGroup = newMember.get("authority");
            assertNotNull("newGroup");
            assertEquals("full name not correct", testGroupName, newGroup.get("fullName"));
            assertEquals("authorityType not correct", "GROUP", newGroup.get("authorityType"));
            

            // Now send the returned value back with a new role (COLLABORATOR)
            newMember.put("role", SiteModel.SITE_COLLABORATOR);
            response = sendRequest(new PutRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, newMember.toString(), "application/json"), 200);
            JsonNode updateResult = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            assertEquals("role not correct", SiteModel.SITE_COLLABORATOR, updateResult.get("role"));
            
        }
        
        // Now List membership to show the group from above.
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS), 200);   
            ArrayNode listResult = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            
            /**
             * The result should have at least 2 elements, 1 for the user who created and 1 for the group added above
             */
            assertTrue("result too small", listResult.size() >= 2);
            for(int i = 0; i < listResult.size(); i++)
            {
                JsonNode obj = listResult.get(i);
                JsonNode authority = obj.get("authority");
                if(authority.get("authorityType").equals("GROUP"))
                {
                    assertEquals("full name not correct", testGroupName, authority.get("fullName"));
                    
                }
                if(authority.get("authorityType").equals("USER"))
                {
                    assertEquals("full name not correct", USER_ONE, authority.get("fullName"));
                }
            }
        }
        
        // Now get the group membership from above
        // Now List membership to show the group from above.
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + '/' + testGroupName), 200);   
            JsonNode getResult = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            System.out.println(response.getContentAsString());
            JsonNode grp = getResult.get("authority");
            assertEquals("full name not correct", testGroupName, grp.get("fullName"));
        }
        
        // cleanup
        if(authorityService.authorityExists(testGroupName))
        {
            this.authenticationComponent.setSystemUserAsCurrentUser();
            authorityService.deleteAuthority(testGroupName);
        }    
    }
    
    public void testDeleteMembership() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
     
        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);
        
        // Post the membership
        sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        
        // Delete the membership
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 200);
        
        // Check that the membership has been deleted
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 404);
        
    }
    
    public void testGetPersonSites() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        String shortName2  = GUID.generate();
        createSite("myPreset", shortName2, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        Response response = sendRequest(new GetRequest("/api/people/" + USER_TWO + "/sites"), 200);
        ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(0, result.size());
        
        // Add some memberships
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);
        sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);
        sendRequest(new PostRequest(URL_SITES + "/" + shortName2 + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);        
        
        response = sendRequest(new GetRequest("/api/people/" + USER_TWO + "/sites"), 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites"), 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.size());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_THREE + "/sites"), 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(0, result.size());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites?size=1"), 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(1, result.size());
        
        response = sendRequest(new GetRequest("/api/people/" + USER_ONE + "/sites?size=5"), 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }   
    
    /**
     * @since 4.0
     */
    public void testGetPotentialMemberships() throws Exception
    {
        // Create a site
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        // Check the memberships
        String filter = "";
        String authorityType = "GROUP";
        String url = "/api/sites/" + shortName + "/potentialmembers?filter=" + filter + "&amp;maxResults=10&amp;authorityType=" + authorityType;
        Response response = sendRequest(new GetRequest(url), 200);
        final String contentAsString = response.getContentAsString();
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(contentAsString);
        assertNotNull(result);
        ArrayNode people = (ArrayNode) result.get("people");
        assertNotNull("people array was null", people);
        
        ArrayNode data = (ArrayNode) result.get("data");
        assertNotNull("data array was null", data);
        
        // Delete the site
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 200);
    }
    
    public void testSiteCustomProperties()
        throws Exception
    {
        QName custPropSingle = QName.createQName(SiteModel.SITE_CUSTOM_PROPERTY_URL, "additionalInformation");
        QName custPropMuliple = QName.createQName(SiteModel.SITE_CUSTOM_PROPERTY_URL, "siteTags");
        
        // Create a site with custom properties, one single and one multiple
        SiteInfo siteInfo = this.siteService.createSite("testPreset", "mySiteWithCustomProperty2", "testTitle", "testDescription", SiteVisibility.PUBLIC);
        NodeRef siteNodeRef = siteInfo.getNodeRef();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(1);
        properties.put(custPropSingle, "information");
        properties.put(custPropMuliple, (Serializable)Arrays.asList(new String[]{ "tag1", "tag2", "tag333" }));
        this.nodeService.addAspect(siteNodeRef, QName.createQName(SiteModel.SITE_MODEL_URL, "customSiteProperties"), properties);
        this.createdSites.add("mySiteWithCustomProperty2");
        
        // Get the detail so of the site
        Response response = sendRequest(new GetRequest(URL_SITES + "/mySiteWithCustomProperty2"), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertNotNull(result);
        JsonNode customProperties = result.get("customProperties");
        assertNotNull(customProperties);
        JsonNode addInfo = customProperties.get("{http://www.alfresco.org/model/sitecustomproperty/1.0}additionalInformation");
        assertNotNull(addInfo);
        assertEquals("{http://www.alfresco.org/model/sitecustomproperty/1.0}additionalInformation", addInfo.get("name"));
        assertEquals("information", addInfo.get("value"));
        assertEquals("{http://www.alfresco.org/model/dictionary/1.0}text", addInfo.get("type"));
        assertEquals("Additional Site Information", addInfo.get("title"));
        
        JsonNode tags = customProperties.get("{http://www.alfresco.org/model/sitecustomproperty/1.0}siteTags");
        assertNotNull(tags);
        assertEquals("{http://www.alfresco.org/model/sitecustomproperty/1.0}siteTags", tags.get("name"));
        assertEquals(NullNode.getInstance(), tags.get("type"));
        assertEquals(NullNode.getInstance(), tags.get("title"));
        // TODO Fix ALF-2707 so this works
        System.err.println(response.getContentAsString());
//        ArrayNode tagsA = tags.get("value");
//        assertEquals(3, tagsA.size());
//        assertEquals("tag1", tagsA.get(0));
//        assertEquals("tag2", tagsA.get(1));
//        assertEquals("tag333", tagsA.get(2));
    }
    
    /*
     * MNT-10917
     * Check permissions of node after move/copy action from "Repository" to any site.
     * Note: permissions should be remain
     */
    public void testCheckPermissionsAfterCopy()
            throws Exception
    {
        AuthenticationUtil.pushAuthentication();
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
        String groupName = AuthorityType.GROUP.getPrefixString() + "_" + GUID.generate().substring(0, 5).trim();
        String createdAuth = authorityService.createAuthority(AuthorityType.GROUP, groupName);
        NodeRef fileNode = null, 
                siteDocLib = null, 
                copiedNode = null, 
                movedNode = null;
        try
        {
            fileNode = createRepoFile();
            siteDocLib = createTestSite();
            addPermissionsToFile(fileNode, createdAuth, SiteModel.SITE_CONTRIBUTOR, true);
            checkPermissions(fileNode, createdAuth, SiteModel.SITE_CONTRIBUTOR, "before copy");
            
            copiedNode = copyToSite(fileNode, siteDocLib);
            checkPermissions(copiedNode, createdAuth, SiteModel.SITE_CONTRIBUTOR, "after copy");
            
            nodeService.deleteNode(copiedNode);
            copiedNode = null;
            checkPermissions(fileNode, createdAuth, SiteModel.SITE_CONTRIBUTOR, "before move");
            movedNode = moveToSite(fileNode, siteDocLib);
            checkPermissions(movedNode, createdAuth, SiteModel.SITE_CONTRIBUTOR, "after move");
        }
        finally
        {
            if (fileNode != null)
            {
                nodeService.deleteNode(fileNode);
            }
            if (siteDocLib != null)
            {
                nodeService.deleteNode(siteDocLib);
            }
            AuthenticationUtil.popAuthentication();
        }
    }
    
    private NodeRef copyToSite(NodeRef fileRef, NodeRef destRef) throws Exception
    {
        FileInfo copiedFileInfo = fileFolderService.copy(fileRef, destRef, null);
        return copiedFileInfo.getNodeRef();
    }
    
    private NodeRef moveToSite(NodeRef fileRef, NodeRef destRef) throws Exception
    {
        FileInfo movedFileInfo = fileFolderService.move(fileRef, destRef, null);
        return movedFileInfo.getNodeRef();
    }

    private void checkPermissions(NodeRef nodeRef, String necessatyAuth, String expectedPermission, String actionInfo)
    {
        Set<AccessPermission> allSetPermissions = permissionService.getAllSetPermissions(nodeRef);
        for (AccessPermission perm : allSetPermissions)
        {
            String authority = perm.getAuthority();
            if (necessatyAuth.equals(authority))
            {
                if (expectedPermission.equals(perm.getPermission()))
                {
                    return;
                }
                fail("Expected permissions for authority \"" + necessatyAuth + "\" are incorrect. Expected: " + expectedPermission + ", but actual permission: "
                        + perm.getPermission() + ". Check position: " + actionInfo);
            }
        }
        fail("Expected authority \"" + necessatyAuth + "\" wasn't found. Check position: " + actionInfo);
    }
        
    private void addPermissionsToFile(NodeRef nodeRef, String user, String permission, boolean isAllowed)
    {
        permissionService.setPermission(nodeRef, user, permission, isAllowed);
    }

    private NodeRef createTestSite()
    {
        String sName = GUID.generate();
        // Create a public site
        this.siteService.createSite("testSitePreset", sName, sName, sName, SiteVisibility.PUBLIC);
        NodeRef siteContainer = this.siteService.createContainer(sName, "testContainer", ContentModel.TYPE_FOLDER, null);
        return siteContainer;
    }

    private NodeRef createRepoFile()
    {
        NodeRef rootNodeRef = this.nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        // create temporary folder
        NodeRef workingRootNodeRef = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName(NamespaceService.ALFRESCO_URI, "working root"),
                ContentModel.TYPE_FOLDER).getChildRef();

        String fName = GUID.generate();
        Map<QName, Serializable> properties = new HashMap<QName, Serializable>(11);
        properties.put(ContentModel.PROP_NAME, (Serializable) fName);
        QName assocQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(fName));
        // create empy file
        ChildAssociationRef assocRef = nodeService.createNode(workingRootNodeRef, ContentModel.ASSOC_CONTAINS, assocQName, ContentModel.TYPE_CONTENT, properties);

        return assocRef.getChildRef();
    }

    /**
     * End to end sanity check of web site invitation.
     * 
     * Nominated and Moderated invitations.
     * 
     * @throws Exception
     */
    public void testInvitationSanityCheck() throws Exception
    {
        String shortName  = GUID.generate();
        String secondShortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        createSite("myPreset", secondShortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        String inviteComments = "Please sir, let me in";
        String userName = USER_TWO;
        String roleName = SiteModel.SITE_CONSUMER;
        
        String inviteeFirstName = "Buffy";
        String inviteeLastName = "Summers";
        String inviteeEmail = "buffy@sunnydale";
        String inviteeUserName = userName;
        String serverPath = "http://localhost:8081/share/";
        String acceptURL = "page/accept-invite";
        String rejectURL = "page/reject-invite";
        
        //Create a new moderated invitation
        String moderatedId = createModeratedInvitation(secondShortName, inviteComments, userName, roleName);
        
        // Get the moderated invitation
        sendRequest(new GetRequest(URL_SITES + "/" + secondShortName + "/invitations/" + moderatedId), 200);
        
        // search for the moderated invitation 
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?inviteeUserName=" + userName), 200);
        
        // Create a nominated invitation
        String nominatedId = createNominatedInvitation(shortName, inviteeFirstName, inviteeLastName, inviteeEmail, inviteeUserName, roleName, serverPath, acceptURL, rejectURL, 201);
        
        // Search for all invitations on this site
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations"), 200);
        
        // cancel the moderated invitation
        sendRequest(new DeleteRequest(URL_SITES + "/" + secondShortName + "/invitations/" + moderatedId), 200);   
    }
    
    /**
     * Detailed Test of Get Invitation Web Script
     * @throws Exception
     */
    public void testGetInvitation() throws Exception
    {
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        /*
         * Create a new moderated invitation
         */
        String inviteeComments = "Please sir, let $* me in";
        String userName = USER_TWO;
        String roleName = SiteModel.SITE_CONSUMER;
        String inviteId = createModeratedInvitation(shortName, inviteeComments, userName, roleName);
              
        /*
         * Negative test - site does not exist
         */
        sendRequest(new GetRequest(URL_SITES + "/rubbish/invitations/" + inviteId), 404);
        
        /*
         * Negative test - site does exist but invitation doesn't
         */
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/activiti$8787487"), 404);
        
        /*
         * Negative test - site does exist but invitation engine is wrong
         */
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/trash$123"), 404);
        
        /*
         * Negative test - site does exist but invitation doesn't  no $ in inviteId
         */
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/trash123"), 404);
        
        /*
         * Positive test - get the invitation and validate that it is correct
         */
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/" + inviteId), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            //System.out.println(response.getContentAsString());
            JsonNode data = top.get("data");
            assertNotNull("data is null", data);
            assertEquals("inviteId is not set", data.get("inviteId"), inviteId);
            assertEquals("invitationType", "MODERATED", data.get("invitationType"));
            assertEquals("inviteeUserName is not set", userName, data.get("inviteeUserName"));
            assertEquals("resourceName is not correct", shortName, data.get("resourceName"));
            assertEquals("resourceType is not correct", "WEB_SITE", data.get("resourceType"));
            // Moderated specific properties
            assertEquals("inviteeComments", inviteeComments, data.get("inviteeComments"));
            assertEquals("roleName is not set", roleName, data.get("roleName"));

        }
        
        /*
         * Cancel the invitation
         */
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + "/invitations/" + inviteId), 200);
        
        /*
         * Verify that the invitation is no longer open
         */
        sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/" + inviteId), 404);
        
        /**
         * Create a nominated invitation
         */
        String inviteeFirstName = "Buffy";
        String inviteeLastName = "Summers";
        String inviteeEmail = "FirstName123.LastName123@email.com";
        String inviteeUserName = null;
        String serverPath = "http://localhost:8081/share/";
        String acceptURL = "page/accept-invite";
        String rejectURL = "page/reject-invite";
        inviteId = createNominatedInvitation(shortName, inviteeFirstName, inviteeLastName, inviteeEmail, inviteeUserName, roleName, serverPath, acceptURL, rejectURL, 201);
        
        /*
         * Positive test - get the invitation and validate that it is correct
         * inviteId and inviteeUserName will be generated.
         */
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations/" + inviteId), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            //System.out.println(response.getContentAsString());
            JsonNode data = top.get("data");
            assertNotNull("data is null", data);
            assertEquals("inviteId is not set", data.get("inviteId"), inviteId);
            assertEquals("invitationType", "NOMINATED", data.get("invitationType"));
            assertEquals("resourceName is not correct", shortName, data.get("resourceName"));
            assertEquals("resourceType is not correct", "WEB_SITE", data.get("resourceType"));
            
            // Nominated specific attributes
            assertEquals("roleName is not set", roleName, data.get("roleName"));
            // Generated user name
            assertNotNull("inviteeUserName is not set", data.get("inviteeUserName"));
            
        }
        
        /*
         * Cancel the nominated invitation
         */
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + "/invitations/" + inviteId), 200);
                       
    }
    
    public void testInviteDisabledUser() throws Exception
    {
        AuthenticationUtil.pushAuthentication();
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        String username = "testUser" + System.nanoTime();
        String siteShortName = GUID.generate();
        try
        {
            createUser(username);
            createSite("myPreset", siteShortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);

            NodeRef personNodeRef = personService.getPerson(username);
            String firstName = (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_FIRSTNAME);
            String lastName = (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_LASTNAME);
            String email = (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_EMAIL);
            String serverPath = "http://localhost:8081/share/";
            String acceptURL = "page/accept-invite";
            String rejectURL = "page/reject-invite";

            authenticationService.setAuthenticationEnabled(username, false);
            createNominatedInvitation(siteShortName, firstName, lastName, email, username, SiteModel.SITE_CONSUMER, serverPath, acceptURL, rejectURL, 409);
            fail("The user " + username + " is disabled and cannot be invited");
        }
        catch (IOException e)
        {
            // expected
        }
        finally
        {
            siteService.deleteSite(siteShortName);
            deleteUser(username);
            AuthenticationUtil.popAuthentication();
        }
    }
    
    /**
     * Detailed Test of List Invitation Web Script.
     * @throws Exception
     */
    public void testListInvitation() throws Exception
    {
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        
        String inviteeComments = "Please sir, let $* me in";
        String userName = USER_TWO;
        String roleName = SiteModel.SITE_CONSUMER;
        String moderatedIdA = createModeratedInvitation(shortName, inviteeComments, userName, roleName);
        
        String inviteeCommentsB = "Please sir, let $* me in";
        String userNameB = USER_THREE;
        String roleNameB = SiteModel.SITE_CONSUMER;
        String moderatedIdB = createModeratedInvitation(shortName, inviteeCommentsB, userNameB, roleNameB);
        
        String inviteeFirstName = "Buffy";
        String inviteeLastName = "Summers";
        String inviteeEmail = "buffy@sunnydale";
        String inviteeUserName = userName;
        String serverPath = "http://localhost:8081/share/";
        String acceptURL = "page/accept-invite";
        String rejectURL = "page/reject-invite";
        
        // Create a nominated invitation
        String nominatedId = createNominatedInvitation(shortName, inviteeFirstName, inviteeLastName, inviteeEmail, inviteeUserName, roleName, serverPath, acceptURL, rejectURL, 201);
        
        /**
         * search by user - negative test wombat does not have an invitation 
         */
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?inviteeUserName=wombat"), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            ArrayNode data = (ArrayNode) top.get("data");
            assertEquals("user wombat", data.size(), 0);
            
        }
        /**
         * search by user - find USER_TWO's invitation
         */
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?inviteeUserName=" + USER_TWO), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            //System.out.println(response.getContentAsString());
            ArrayNode data = (ArrayNode) top.get("data");
            assertEquals("user two invitation not found", 1, data.size());
            JsonNode first = data.get(0);
            assertEquals("userid is wrong", first.get("inviteeUserName"), USER_TWO);
            
        }
        
        /**
         * search by type - should find two moderated invitations
         */

        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?invitationType=MODERATED"), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            //System.out.println(response.getContentAsString());
            ArrayNode data = (ArrayNode) top.get("data");
            assertEquals("two moderated invitations not found", data.size(), 2);
        }
        
        // negative test - unknown invitationType
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?invitationType=Crap"), 500);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        }
        
        /**
         * search by user and type
         */
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName + "/invitations?inviteeUserName=" + USER_TWO + "&invitationType=MODERATED"), 200);
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            //System.out.println(response.getContentAsString());
            ArrayNode data = (ArrayNode) top.get("data");
            assertEquals("user two invitation not found", data.size(), 1);
            JsonNode first = data.get(0);
            assertEquals("first userid is wrong", first.get("inviteeUserName"), USER_TWO);
            assertEquals("type is wrong", first.get("invitationType"), "MODERATED"); 
        }      
    }
    
    /**
     * Detailed test of Create Invitation Web Script
     * 
     * Create Nominated Invitation
     * 
     * Create Moderated Invitation
     * 
     * @throws Exception
     */
    public void testCreateInvitation() throws Exception
    {
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        

        String inviteComments = "Please sir, let me in";
        String userName = USER_TWO;
        String roleName = SiteModel.SITE_CONSUMER;
        String inviteId = null;

        /*
         * Negative test - wrong invitation type
         */
        {
            ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
            newInvitation.put("invitationType", "Grundge");
            newInvitation.put("inviteeRoleName", roleName);
            newInvitation.put("inviteeComments", inviteComments);
            newInvitation.put("inviteeUserName", userName);
            sendRequest(new PostRequest(URL_SITES + "/" + shortName + "/invitations",  newInvitation.toString(), "application/json"), Status.STATUS_BAD_REQUEST);   
        }
        
        /*
         * Negative test - missing Invitation type
         */
        {
            ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
            newInvitation.put("inviteeRoleName", roleName);
            newInvitation.put("inviteeComments", inviteComments);
            newInvitation.put("inviteeUserName", userName);
            sendRequest(new PostRequest(URL_SITES + "/" + shortName + "/invitations",  newInvitation.toString(), "application/json"), Status.STATUS_BAD_REQUEST);   
        }
        
        /*
         * Negative test - blank RoleName
         */
        {
            ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
            newInvitation.put("invitationType", "MODERATED");
            newInvitation.put("inviteeRoleName", "");
            newInvitation.put("inviteeComments", inviteComments);
            newInvitation.put("inviteeUserName", userName);
            sendRequest(new PostRequest(URL_SITES + "/" + shortName + "/invitations",  newInvitation.toString(), "application/json"), Status.STATUS_BAD_REQUEST);   
        }
        
        /*
         * Create a new moderated invitation
         */
        ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
        {
            newInvitation.put("invitationType", "MODERATED");
            newInvitation.put("inviteeRoleName", roleName);
            newInvitation.put("inviteeComments", inviteComments);
            newInvitation.put("inviteeUserName", userName);
            Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + "/invitations",  newInvitation.toString(), "application/json"), Status.STATUS_CREATED);   
            JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            JsonNode data = top.get("data");
            inviteId = data.get("inviteId").textValue();
            assertEquals("invitationType", "MODERATED", data.get("invitationType"));
            assertEquals("inviteeUserName is not set", userName, data.get("inviteeUserName"));
            assertEquals("resourceName is not correct", shortName, data.get("resourceName"));
            assertEquals("resourceType is not correct", "WEB_SITE", data.get("resourceType"));
            
        }
        assertNotNull("inviteId is null", inviteId);
        assertTrue("inviteId is too small", inviteId.length() > 0);
        
    }    
    
    private String createNominatedInvitation(String siteName, String inviteeFirstName, String inviteeLastName, String inviteeEmail, String inviteeUserName, String inviteeRoleName, String serverPath, String acceptURL, String rejectURL, int expectedStatus) throws Exception
    {
        /*
         * Create a new nominated invitation
         */
        ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
        
        newInvitation.put("invitationType", "NOMINATED");
        newInvitation.put("inviteeRoleName", inviteeRoleName);
        if(inviteeUserName != null)
        {
            // nominate an existing user
            newInvitation.put("inviteeUserName", inviteeUserName);
        }
        else
        {
            // nominate someone else
            newInvitation.put("inviteeFirstName", inviteeFirstName);
            newInvitation.put("inviteeLastName", inviteeLastName);
            newInvitation.put("inviteeEmail", inviteeEmail);
        }
        newInvitation.put("serverPath", serverPath);
        newInvitation.put("acceptURL", acceptURL);
        newInvitation.put("rejectURL", rejectURL);    
        
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + siteName + "/invitations",  newInvitation.toString(), "application/json"), expectedStatus);
        JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode data = top.get("data");
        String inviteId = data.get("inviteId").textValue();
        
        return inviteId;
    }
    
    private String createModeratedInvitation(String siteName, String inviteeComments, String inviteeUserName, String inviteeRoleName) throws Exception
    {
        /*
         * Create a new moderated invitation
         */
        ObjectNode newInvitation = AlfrescoDefaultObjectMapper.createObjectNode();
        
        newInvitation.put("invitationType", "MODERATED");
        newInvitation.put("inviteeRoleName", inviteeRoleName);
        newInvitation.put("inviteeComments", inviteeComments);
        newInvitation.put("inviteeUserName", inviteeUserName);
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + siteName + "/invitations",  newInvitation.toString(), "application/json"), 201);   
        JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode data = top.get("data");
        String inviteId = data.get("inviteId").textValue();
        
        return inviteId;
    }
    
    public void testGetMemberInfo() throws Exception
    {
        String testGroup = "SiteServiceTestGroupA";
        String testGroupName = "GROUP_" + testGroup;

        if (!authorityService.authorityExists(testGroupName))
        {
            this.authenticationComponent.setSystemUserAsCurrentUser();

            testGroupName = authorityService.createAuthority(AuthorityType.GROUP, testGroup,
                        testGroup, authorityService.getDefaultZones());
        }
        
        if(!authorityService.getContainedAuthorities(AuthorityType.USER, testGroupName, true).contains(USER_TWO))
        {
            this.authenticationComponent.setSystemUserAsCurrentUser();
            this.authorityService.addAuthority(testGroupName, USER_TWO);
        }

        this.authenticationComponent.setCurrentUser(USER_ONE);

        // CRUD a membership group for a web site
        // Create a site
        String shortName = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);

        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode group = AlfrescoDefaultObjectMapper.createObjectNode();
        group.put("fullName", testGroupName);
        membership.set("group", group);

        // Create a new group membership
        {
            Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName
                        + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
            JsonNode newMember = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

            // Validate the return value
            assertEquals("role not correct", SiteModel.SITE_CONSUMER, newMember.get("role"));
            JsonNode newGroup = newMember.get("authority");
            assertNotNull(newGroup);
            assertEquals("full name not correct", testGroupName, newGroup.get("fullName"));
            assertEquals("authorityType not correct", "GROUP", newGroup.get("authorityType"));
        }

        // Now List memberships
        {
            Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName
                        + URL_MEMBERSHIPS + "?authorityType=USER"), 200);
            ArrayNode listResult = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
            assertNotNull(listResult);
            assertEquals(2, listResult.size());

            for (int i = 0; i < listResult.size(); i++)
            {
                JsonNode json = listResult.get(i);

                if (USER_ONE.equals(json.get("authority").get("fullName")))
                {
                    assertEquals("user one is Not member of any group", false, json.get("isMemberOfGroup"));
                }
                else
                {
                    assertEquals("full name not correct", USER_TWO, json.get("authority").get("fullName"));
                    assertEquals("user two is member of a SiteServiceTestGroupA group", true, json.get("isMemberOfGroup"));
                }
            }
        }
        
        // cleanup
        if (authorityService.authorityExists(testGroupName))
        {
            this.authenticationComponent.setSystemUserAsCurrentUser();
            this.authorityService.deleteAuthority(testGroupName);
        }
    }
    
    public void testChangeSiteVisibilityAsSiteAdmin() throws Exception
    {
        // Create a site
        String shortName = GUID.generate();

        // Create a new site
        JsonNode result = createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        assertEquals(SiteVisibility.PUBLIC.toString(), result.get("visibility"));

        // try to change the site visibility as user2
        this.authenticationComponent.setCurrentUser(USER_TWO);
        ObjectNode changeVisibility = AlfrescoDefaultObjectMapper.createObjectNode();
        changeVisibility.put("shortName", shortName);
        changeVisibility.put("visibility", "PRIVATE");

        // we should get AccessDeniedException
        sendRequest(new PutRequest(URL_SITES + "/" + shortName, changeVisibility.toString(), "application/json"), 500);
        SiteInfo siteInfo = siteService.getSite(shortName);
        assertEquals("Site visibility should not have been changed.", SiteVisibility.PUBLIC, siteInfo.getVisibility());

        // set the current user as site-admin
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        // Change the visibility to private
        Response response = sendRequest(new PutRequest(URL_SITES + "/" + shortName, changeVisibility.toString(), "application/json"), 200);
        JsonNode jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(SiteVisibility.PRIVATE.toString(), jsonObj.get("visibility"));

        // Change the visibility to moderated. We want to test if we can find
        // the private site before changing its visibility
        changeVisibility.put("visibility", "MODERATED");
        response = sendRequest(new PutRequest(URL_SITES + "/" + shortName, changeVisibility.toString(), "application/json"), 200);
        jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(SiteVisibility.MODERATED.toString(), jsonObj.get("visibility"));

        // Remove user4 from the site-admin group
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        authorityService.removeAuthority("GROUP_SITE_ADMINISTRATORS", USER_FOUR_AS_SITE_ADMIN);

        // set the current user as site-admin
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        // Now that we have removed user4 from the group, try to test if he can still modify the site
        changeVisibility.put("visibility", "PUBLIC");
        sendRequest(new PutRequest(URL_SITES + "/" + shortName, changeVisibility.toString(), "application/json"), 500);
        siteInfo = siteService.getSite(shortName);
        assertEquals("Site visibility should not have been changed.", SiteVisibility.MODERATED, siteInfo.getVisibility());
    }

    public void testChangeMembershipRoleAsSiteAdmin() throws Exception
    {
        // Create a site
        String shortName = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);

        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);

        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        JsonNode jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        // Check the result
        assertEquals(SiteModel.SITE_CONSUMER,  jsonObj.get("role"));
        assertEquals(USER_TWO,  jsonObj.get("authority").get("userName"));

        // try to change the user role as user3
        this.authenticationComponent.setCurrentUser(USER_THREE);
        membership.put("role", SiteModel.SITE_COLLABORATOR);
        sendRequest(new PutRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 500);
        assertEquals("User's role should not have been changed.", SiteModel.SITE_CONSUMER.toString(), siteService.getMembersRole(shortName, USER_TWO));

        // set the current user as site-admin
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        response = sendRequest(new PutRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        // Check the result
        assertEquals(SiteModel.SITE_COLLABORATOR,  jsonObj.get("role"));
        assertEquals(USER_TWO,  jsonObj.get("authority").get("userName"));
    }
    
    public void testDeleteMembershipAsSiteAdmin() throws Exception
    {
        // Create a site
        String shortName = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);

        // Build the JSON membership object
        ObjectNode membership = AlfrescoDefaultObjectMapper.createObjectNode();
        membership.put("role", SiteModel.SITE_CONSUMER);
        ObjectNode person = AlfrescoDefaultObjectMapper.createObjectNode();
        person.put("userName", USER_TWO);
        membership.set("person", person);

        // Post the membership
        Response response = sendRequest(new PostRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS, membership.toString(), "application/json"), 200);
        JsonNode jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        // Check the result
        assertEquals(SiteModel.SITE_CONSUMER, jsonObj.get("role"));
        assertEquals(USER_TWO, jsonObj.get("authority").get("userName"));

        // try to delete user2 from the site
        this.authenticationComponent.setCurrentUser(USER_THREE);
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 500);
        assertTrue(USER_THREE + " doesn’t have permission to delete users from the site", siteService.isMember(shortName, USER_TWO));

        // set the current user as site-admin
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName + URL_MEMBERSHIPS + "/" + USER_TWO), 200);
        assertFalse(siteService.isMember(shortName, USER_TWO));
    }

    public void testDeleteSiteAsSiteAdmin() throws Exception
    {
        // Create a site
        String shortName = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.PUBLIC, 200);
        // Get the site
        sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);

        // try to delete the site
        this.authenticationComponent.setCurrentUser(USER_THREE);
        // Delete the site
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 500);
        // Get the site
        Response response = sendRequest(new GetRequest(URL_SITES + "/" + shortName), 200);
        JsonNode jsonObj = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(shortName, jsonObj.get("shortName"));

        // set the current user as site-admin
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        // Delete the site
        sendRequest(new DeleteRequest(URL_SITES + "/" + shortName), 200);
        sendRequest(new GetRequest(URL_SITES + "/" + shortName), 404);
    }

    public void testGetAllSitesAsSiteAdmin() throws Exception
    {
        String user1PublicSiteName = GUID.generate();
        String user1ModeratedSiteName = GUID.generate();
        String user1PrivateSiteName = GUID.generate();

        String user2PrivateSiteName = GUID.generate();

        // USER_ONE public site
        JsonNode result = createSite("myPreset", user1PublicSiteName, "u1PublicSite", "myDescription",
                    SiteVisibility.PUBLIC, 200);
        assertEquals(SiteVisibility.PUBLIC.toString(), result.get("visibility"));

        // USER_ONE moderated site
        result = createSite("myPreset", user1ModeratedSiteName, "u1ModeratedSite", "myDescription",
                    SiteVisibility.MODERATED, 200);
        assertEquals(SiteVisibility.MODERATED.toString(), result.get("visibility"));

        // USER_ONE private site
        result = createSite("myPreset", user1PrivateSiteName, "u1PrivateSite", "myDescription", SiteVisibility.PRIVATE,
                    200);
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));

        this.authenticationComponent.setCurrentUser(USER_TWO);
        // USER_TWO private site
        result = createSite("myPreset", user2PrivateSiteName, "u2PrivateSite", "myDescription", SiteVisibility.PRIVATE, 200);
        assertEquals(SiteVisibility.PRIVATE.toString(), result.get("visibility"));

        this.authenticationComponent.setCurrentUser(USER_THREE);
        // Note: we'll get 404 rather than 403
        sendRequest(new GetRequest(URL_SITES_ADMIN), 404);
        
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        Response response = sendRequest(new GetRequest(URL_SITES_ADMIN), 200);
        JsonNode jsonObject = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        ArrayNode jsonArray = (ArrayNode) jsonObject.get("list").get("entries");
        
        int siteAdminGetSitesSize = jsonArray.size(); 
        // SiteAdmin can see the public, moderated and private sites
        assertTrue("result too small", siteAdminGetSitesSize >= 4);
        assertTrue("Site admin can access all the sites (PUBLIC | MODERATED | PRIVATE).", canSeePrivateSites(jsonArray));
        
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        response = sendRequest(new GetRequest(URL_SITES_ADMIN), 200);
        jsonObject = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        jsonArray = (ArrayNode) jsonObject.get("list").get("entries");;
        assertEquals("SiteAdmin must have access to the same sites as the super Admin.", siteAdminGetSitesSize,
                    jsonArray.size());
    }
    
    public void testGetAllSitesPagedAsSiteAdmin() throws Exception
    {
        // we use this as a name filter
        long siteNamePrefix = System.currentTimeMillis();
        String siteNameSuffix = GUID.generate();;
        String user1PublicSiteName = siteNamePrefix + siteNameSuffix.substring(siteNameSuffix.lastIndexOf('-'));
        
        createSite("myPreset", user1PublicSiteName, "u1PublicSite", "myDescription",
                    SiteVisibility.PUBLIC, 200);
        // Create 5 more sites
        for(int i =1; i < 6; i++)
        {
            createSite("myPreset", GUID.generate(), "u1PublicSite"+i, "myDescription"+i,
                        SiteVisibility.PUBLIC, 200);
        }
        
        this.authenticationComponent.setCurrentUser(USER_FOUR_AS_SITE_ADMIN);
        
        Response response = sendRequest(new GetRequest(URL_SITES_ADMIN+"?maxItems=5&skipCount=0"), 200);
        JsonNode jsonObject = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode paging = jsonObject.get("list").get("pagination");
        assertEquals("The skipCount must be 0", 0, paging.get("skipCount").intValue());
        assertEquals("The maxItems must be 5", 5, paging.get("maxItems").intValue());
        // There are only 7 sites in total (including the default alfresco site 'swsdp'),
        // but in case there are hanging sites that haven't been cleaned, 
        // or the default alfresco site has been deleted by previous tests, we check for what we have created in this test.
        assertTrue("The totalItems must be 6", paging.get("totalItems").intValue() >= 6 );
        assertTrue(paging.get("hasMoreItems").booleanValue());
        
        response = sendRequest(new GetRequest(URL_SITES_ADMIN+"?nf="+siteNamePrefix+"&maxItems=5&skipCount=0"), 200);
        jsonObject = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        paging = jsonObject.get("list").get("pagination");
        assertEquals("The count must be 1", 1, paging.get("count"));
        assertEquals("The maxItems must be 5", 5, paging.get("maxItems"));
        assertEquals("The totalItems must be 1", 1, paging.get("totalItems"));
        assertFalse(paging.get("hasMoreItems").booleanValue());

    }

    private boolean canSeePrivateSites(ArrayNode jsonArray) throws Exception
    {
        for (int i = 0; i < jsonArray.size(); i++)
        {
            JsonNode obj = jsonArray.get(i);
            String visibility = obj.get("entry").get("visibility").textValue();
            if (SiteVisibility.PRIVATE.equals(SiteVisibility.valueOf(visibility)))
            {
                return true;
            }
        }
        return false;
    }
    
    public void testMultipleInviteRequests() throws Exception
    {
        String shortName  = GUID.generate();
        createSite("myPreset", shortName, "myTitle", "myDescription", SiteVisibility.MODERATED, 200);
        String userName = USER_TWO;
        String roleName = SiteModel.SITE_CONSUMER;
        String inviteComments = "Request to join";
 
        try {
             //Create a new moderated invitation
             String moderatedInvitationId = createModeratedInvitation(shortName, inviteComments, userName, roleName);
             //Create another invitation
             String newModeratedInvitationId = createModeratedInvitation(shortName, inviteComments, userName, roleName);
             fail("A request to join this site is already in pending");
        }
        catch (AssertionFailedError e) {
              // Ignore since we where expecting this
        }
    }
    
}
