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
package org.alfresco.repo.web.scripts.groups;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.util.PropertyMap;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Unit test of Groups REST APIs. 
 * 
 * /api/groups 
 * /api/rootgroups 
 *  
 * @author Mark Rogers
 */
public class GroupsTest extends BaseWebScriptTest
{    
	private static final Log logger = LogFactory.getLog(BaseWebScriptTest.class);
	
    private MutableAuthenticationService authenticationService;
    private AuthorityService authorityService;
    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    
    private String ADMIN_GROUP = "ALFRESCO_ADMINISTRATORS";
    private String EMAIL_GROUP = "EMAIL_CONTRIBUTORS";
    private String TEST_ROOTGROUP = "GroupsTest_ROOT";
    private String TEST_GROUPA = "TestA";
    private String TEST_GROUPB = "TESTB";
    private String TEST_GROUPC = "TesTC";
    private String TEST_GROUPD = "TESTD";
    private String TEST_GROUPE = "TestE";
    private String TEST_GROUPF = "TestF";
    private String TEST_GROUPG = "TestG";
    private String TEST_LINK = "TESTLINK";
    private String TEST_ROOTGROUP_DISPLAY_NAME = "GROUPS_TESTROOTDisplayName";
    
    private static final String USER_ONE = "GroupTestOne";
    private static final String USER_TWO = "GroupTestTwo";
    private static final String USER_THREE = "GroupTestThree";
    
    private static final String URL_GROUPS = "/api/groups";
    private static final String URL_ROOTGROUPS = "/api/rootgroups";
        
    /**
     * Test Tree for all group tests
     *
     * TEST_ROOTGROUP
     *	GROUPA
     *	GROUPB
     *		GROUPD
     *		GROUPE (in Share Zone)
     *		USER_TWO
     *		USER_THREE
     *	GROUPC
     *		USER_TWO
     *	GROUPF
     *		GROUPD
     *	GROUPG
     *		GROUPD
     */	
    private synchronized String createTestTree()
    {
    	if(rootGroupName == null)
    	{
    		rootGroupName = authorityService.getName(AuthorityType.GROUP, TEST_ROOTGROUP);
    	}
    	
        Set<String> shareZones = new HashSet<String>(1, 1.0f);
        shareZones.add(AuthorityService.ZONE_APP_SHARE);
    	
        if(!authorityService.authorityExists(rootGroupName))
        {
            AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        	 
        	rootGroupName = authorityService.createAuthority(AuthorityType.GROUP, TEST_ROOTGROUP, TEST_ROOTGROUP_DISPLAY_NAME, authorityService.getDefaultZones());
        	String groupA = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPA, TEST_GROUPA, authorityService.getDefaultZones());
        	authorityService.addAuthority(rootGroupName, groupA);
        	String groupB = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPB, TEST_GROUPB,authorityService.getDefaultZones());
            authorityService.addAuthority(rootGroupName, groupB);
        	String groupD = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPD, TEST_GROUPD, authorityService.getDefaultZones());
         	String groupE = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPE, TEST_GROUPE, shareZones);
            authorityService.addAuthority(groupB, groupD);
            authorityService.addAuthority(groupB, groupE);
        	authorityService.addAuthority(groupB, USER_TWO);
        	authorityService.addAuthority(groupB, USER_THREE);
            String groupF = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPF, TEST_GROUPF, authorityService.getDefaultZones());
            authorityService.addAuthority(rootGroupName, groupF);
            authorityService.addAuthority(groupF, groupD);
            String groupG = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPG, TEST_GROUPG, authorityService.getDefaultZones());
            authorityService.addAuthority(rootGroupName, groupG);
            authorityService.addAuthority(groupG, groupD);
            
        	String groupC = authorityService.createAuthority(AuthorityType.GROUP, TEST_GROUPC, TEST_GROUPC,authorityService.getDefaultZones());
        	authorityService.addAuthority(rootGroupName, groupC);
        	authorityService.addAuthority(groupC, USER_TWO);
        
        	String link = authorityService.createAuthority(AuthorityType.GROUP, TEST_LINK, TEST_LINK, authorityService.getDefaultZones());
        	authorityService.addAuthority(rootGroupName, link);
        	
            this.authenticationComponent.setCurrentUser(USER_ONE);
        }
        
        return rootGroupName;

    }

    private static String rootGroupName = null;    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        this.authenticationService = (MutableAuthenticationService)getServer().getApplicationContext().getBean("AuthenticationService");
        this.authenticationComponent = (AuthenticationComponent)getServer().getApplicationContext().getBean("authenticationComponent");
        this.personService = (PersonService)getServer().getApplicationContext().getBean("PersonService");
        this.authorityService = (AuthorityService)getServer().getApplicationContext().getBean("AuthorityService");
        
        this.authenticationComponent.setSystemUserAsCurrentUser();
        
        // Create users
        createUser(USER_ONE);
        createUser(USER_TWO);
        createUser(USER_THREE);
        
        // Do tests as user one
        this.authenticationComponent.setCurrentUser(USER_ONE);
    }
    
    private void createUser(String userName)
    {
        if (this.authenticationService.authenticationExists(userName) == false)
        {
            this.authenticationService.createAuthentication(userName, "PWD".toCharArray());
            
            PropertyMap ppOne = new PropertyMap(4);
            ppOne.put(ContentModel.PROP_USERNAME, userName);
            ppOne.put(ContentModel.PROP_FIRSTNAME, "firstName");
            ppOne.put(ContentModel.PROP_LASTNAME, "lastName");
            ppOne.put(ContentModel.PROP_EMAIL, "email@email.com");
            ppOne.put(ContentModel.PROP_JOBTITLE, "jobTitle");
            
            this.personService.createPerson(ppOne);
        }        
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        this.authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
    }
    
    /**
     * Detailed test of get root groups
     */
    public void testGetRootGroup() throws Exception
    { 
        createTestTree();
        
    	/**
    	 * Get all root groups, regardless of zone, should be at least the ALFRESCO_ADMINISTRATORS, 
    	 * TEST_ROOTGROUP and EMAIL_CONTRIBUTORS groups
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_ROOTGROUPS), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		//System.out.println(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() >= 3);
    		boolean gotRootGroup = false;
    		boolean gotEmailGroup = false;
    		
    		
    		for(int i = 0; i < data.size(); i++)
    		{
    			JsonNode rootGroup = data.get(i);
    			if(rootGroup.get("shortName").textValue().equals(TEST_ROOTGROUP))
    			{
    				// This is our test rootgroup
    				assertEquals("shortName wrong", TEST_ROOTGROUP, rootGroup.get("shortName").textValue());
    				assertEquals("displayName wrong", TEST_ROOTGROUP_DISPLAY_NAME, rootGroup.get("displayName").textValue());
    				assertEquals("authorityType wrong", "GROUP", rootGroup.get("authorityType").textValue());
    				gotRootGroup = true;
    			}
    			if(rootGroup.get("shortName").textValue().equals(EMAIL_GROUP))
    			{
    				gotEmailGroup = true;
    			}
    		}
        	assertTrue("root group not found", gotRootGroup);
        	assertTrue("email group not found", gotEmailGroup);
    	}

    	
    	if(rootGroupName != null)
    	{
    		rootGroupName = authorityService.getName(AuthorityType.GROUP, TEST_ROOTGROUP);
    	}
    	
    	Set<String> zones = authorityService.getAuthorityZones(rootGroupName);
    	assertTrue("root group is in APP.DEFAULT zone", zones.contains("APP.DEFAULT") );
    	
    	/**
    	 * Get all root groups in the application zone "APP.DEFAULT"
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_ROOTGROUPS + "?zone=APP.DEFAULT"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		//System.out.println(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		
    		assertTrue(data.size() > 0);
    		
    		for(int i = 0; i < data.size(); i++)
    		{
    			JsonNode rootGroup = data.get(i);
    			if(rootGroup.get("shortName").textValue().equals(TEST_ROOTGROUP))
    			{
    				// This is our test rootgroup
    				assertEquals("shortName wrong", TEST_ROOTGROUP, rootGroup.get("shortName").textValue());
    				assertEquals("displayName wrong", TEST_ROOTGROUP_DISPLAY_NAME, rootGroup.get("displayName").textValue());
    				assertEquals("authorityType wrong", "GROUP", rootGroup.get("authorityType").textValue());
    			}
    		}	
    	}
    	
    	/**
    	 * Get all root groups in the admin zone
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_ROOTGROUPS + "?zone=AUTH.ALF"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() > 0);
    		
    		for(int i = 0; i < data.size(); i++)
    		{
    			JsonNode rootGroup = data.get(i);
    			if(rootGroup.get("shortName").textValue().equals(TEST_ROOTGROUP))
    			{
    				// This is our test rootgroup
    				assertEquals("shortName wrong", TEST_ROOTGROUP, rootGroup.get("shortName").textValue());
    				assertEquals("displayName wrong", TEST_ROOTGROUP_DISPLAY_NAME, rootGroup.get("displayName").textValue());
    				assertEquals("authorityType wrong", "GROUP", rootGroup.get("authorityType").textValue());
    			}
    		}	
    	}
    	
    	/**
    	 * Negative test Get all root groups in the a zone that does not exist
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_ROOTGROUPS + "?zone=WIBBLE"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() == 0);
    		// Should return no results
    	}
    	
    }
    
    /**
     * Detailed test of get group
     */
    public void testGetGroup() throws Exception
    {
        createTestTree();
        
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + ADMIN_GROUP), 200);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		JsonNode data = top.get("data");
    		assertTrue(data.size() > 0);
    	}
    	
    	{
    		sendRequest(new GetRequest(URL_GROUPS + "/" + "crap"), Status.STATUS_NOT_FOUND);
    	}
    	
    	/**
    	 * Get GROUP B
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		JsonNode data = top.get("data");
    		assertTrue(data.size() > 0);
    	}
    	
    	/**
    	 * Get GROUP E which is in a different zone
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPE), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		JsonNode data = top.get("data");
    		assertTrue(data.size() > 0);
    	}
    
    }
    
    /**
     * Detailed test of create root group
     * Detailed test of delete root group
     */
    public void testCreateRootGroup() throws Exception
    {
    	String myGroupName = "GT_CRG";
    	String myDisplayName = "GT_CRGDisplay";
    	
    	/**
    	 * Negative test - try to create a group without admin authority
    	 */
    	{
    		ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    		newGroupJSON.put("displayName", myDisplayName); 
    		sendRequest(new PostRequest(URL_ROOTGROUPS + "/" + myGroupName,  newGroupJSON.toString(), "application/json"), Status.STATUS_INTERNAL_SERVER_ERROR);   
    	}
    	
    	 
    	this.authenticationComponent.setSystemUserAsCurrentUser();
    	
    	try
    	{
    		/**
    		 * Create a root group
    		 */
    		{
    			ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			newGroupJSON.put("displayName", myDisplayName); 
    			Response response = sendRequest(new PostRequest(URL_ROOTGROUPS + "/" + myGroupName,  newGroupJSON.toString(), "application/json"), Status.STATUS_CREATED);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			JsonNode rootGroup = top.get("data");
    			assertEquals("shortName wrong", myGroupName, rootGroup.get("shortName").textValue());
    			assertEquals("displayName wrong", myDisplayName, rootGroup.get("displayName").textValue());
    		}
    	
    		/**
    		 * Negative test Create a root group that already exists
    		 */
    		{
    			ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			newGroupJSON.put("displayName", myDisplayName); 
    			sendRequest(new PostRequest(URL_ROOTGROUPS + "/" + myGroupName,  newGroupJSON.toString(), "application/json"), Status.STATUS_BAD_REQUEST);   
    		}
    		
    		/**
    		 * Delete the root group
    		 */
    		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myGroupName), Status.STATUS_OK);
    		
    		/**
    		 * Attempt to delete the root group again - should fail
    		 */
    		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myGroupName), Status.STATUS_NOT_FOUND);
    		
    		
    	} 
    	finally
    	{
    	
    		/**
    		 * Delete the root group
    		 */
    		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myGroupName), 0);  
    	}
    }
    
    /**
     * Detailed test of link group
     */
    public void testLinkChild() throws Exception
    {
    	String myRootGroup = "GT_LGROOT";
    	
    	try 
    	{
    		this.authenticationComponent.setSystemUserAsCurrentUser();
    		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myRootGroup), 0);
    		
    		String groupLinkFullName = "";
    		{
    			Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_LINK), Status.STATUS_OK);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			logger.debug(response.getContentAsString());
    			JsonNode data = top.get("data");
    			assertTrue(data.size() > 0);
    			groupLinkFullName = data.get("fullName").textValue();
    		}
    		
    		/**
    		 * Create a root group
    		 */
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			newGroupJSON.put("displayName", myRootGroup); 
    			sendRequest(new PostRequest(URL_ROOTGROUPS + "/" + myRootGroup,  newGroupJSON.toString(), "application/json"), Status.STATUS_CREATED);    
    		}
    		
    		/**
    		 * Link an existing group (GROUPB) to my root group.
    		 */
    		
    		/**
    		 * Negative test Link Group B without administrator access.
    		 */
    		this.authenticationComponent.setCurrentUser(USER_ONE);
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			sendRequest(new PostRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + groupLinkFullName, newGroupJSON.toString(), "application/json" ), Status.STATUS_INTERNAL_SERVER_ERROR);
    		}
    		
    		this.authenticationComponent.setSystemUserAsCurrentUser();
    		
    		/**
    		 * Link Group B
    		 */
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			Response response = sendRequest(new PostRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + groupLinkFullName, newGroupJSON.toString(), "application/json" ), Status.STATUS_OK);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			logger.debug(response.getContentAsString());
    			JsonNode data = top.get("data");
    		}
    		
    		/**
    		 * Link the group again - this fails
    		 * - duplicate groups (children with the same name) are not allowed 
    		 */
    		{
    			ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			Response response = sendRequest(new PostRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + groupLinkFullName, newGroupJSON.toString(), "application/json" ), Status.STATUS_INTERNAL_SERVER_ERROR);
    		}
    		
        	/**
        	 * Get All Children of myGroup which are GROUPS - should find GROUP B
        	 */
        	{
        		logger.debug("Get child GROUPS of myRootGroup");
        		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + myRootGroup + "/children?authorityType=GROUP"), Status.STATUS_OK);
        		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		ArrayNode data = (ArrayNode) top.get("data");
        		assertTrue("no child groups of myGroup", data.size() == 1);
        		
        		JsonNode subGroup = data.get(0);
        		assertEquals("shortName wrong", TEST_LINK, subGroup.get("shortName").textValue());
        		assertEquals("authorityType wrong", "GROUP", subGroup.get("authorityType").textValue());
        	}
        	
        	/**
        	 * Now link in an existing user
        	 */		 
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			String userOneFullName = USER_ONE;
    			Response response = sendRequest(new PostRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + userOneFullName, newGroupJSON.toString(), "application/json" ), Status.STATUS_OK);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			logger.debug(response.getContentAsString());
    			JsonNode data = top.get("data");
    		}
    		
        	/**
        	 * Get All Children of myGroup which are USERS - should find USER ONE
        	 */
        	{
        		logger.debug("Get child USERS of myRootGroup");
        		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + myRootGroup + "/children?authorityType=USER"), Status.STATUS_OK);
        		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		ArrayNode data = (ArrayNode) top.get("data");
        		assertTrue("no child groups of myGroup", data.size() == 1);
        		
        		JsonNode subGroup = data.get(0);
        		assertEquals("shortName wrong", USER_ONE, subGroup.get("shortName").textValue());
        		assertEquals("authorityType wrong", "USER", subGroup.get("authorityType").textValue());
        	}
    			
    		/**
    		 * Unlink Group B
    		 */
    		{
    			logger.debug("Unlink Test Link");
    			Response response = sendRequest(new DeleteRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + groupLinkFullName ), Status.STATUS_OK);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			logger.debug(response.getContentAsString());

    		}
    		
        	/**
        	 * Get All Children of myGroup which are GROUPS - should no longer find GROUP B
        	 */
        	{
        		logger.debug("Get child GROUPS of myRootGroup");
        		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + myRootGroup + "/children?authorityType=GROUP"), Status.STATUS_OK);
        		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		ArrayNode data = (ArrayNode) top.get("data");
        		//TODO TEST failing
        		
        		//assertTrue("group B not removed", data.size() == 0);
        	}
        	
    		/**
    		 * Create a new group (BUFFY)
    		 */
        	String myNewGroup = "GROUP_BUFFY";
    		{
    			// Delete incase it already exists from a previous test run
        		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/BUFFY"), 0);

                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			Response response = sendRequest(new PostRequest(URL_GROUPS + "/" + myRootGroup +"/children/" + myNewGroup, newGroupJSON.toString(), "application/json" ), Status.STATUS_CREATED);
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    			logger.debug(response.getContentAsString());
    			JsonNode data = top.get("data");
        		assertEquals("shortName wrong", "BUFFY", data.get("shortName").textValue());
        		assertEquals("fullName wrong", myNewGroup, data.get("fullName").textValue());
        		assertEquals("authorityType wrong", "GROUP", data.get("authorityType").textValue());
    		}
        	
    		/**
        	 * Get All Children of myGroup which are GROUPS - should find GROUP(BUFFY)
        	 */
        	{
        		logger.debug("Get child GROUPS of myRootGroup");
        		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + myRootGroup + "/children?authorityType=GROUP"), Status.STATUS_OK);
        		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		ArrayNode data = (ArrayNode) top.get("data");
           		for(int i = 0; i < data.size(); i++)
        		{
        			JsonNode rootGroup = data.get(i);
        			if(rootGroup.get("fullName").textValue().equals(myNewGroup))
        			{
        			
        			}
        		}

        	}
    		
    		/**
    		 * Negative tests
    		 */
    	
    	}
    	finally
    	{
    		sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myRootGroup), 0);
    	}
    }
    
    /**
     * Detailed test of update group
     * @throws Exception
     */
    public void testUpdateGroup() throws Exception
    {
    	String myGroupName = "GT_UG";
    	String myDisplayName = "GT_UGDisplay";
    	String myNewDisplayName = "GT_UGDisplayNew";
    
    	this.authenticationComponent.setSystemUserAsCurrentUser();
    	
    	try
    	{
    		/**
    		 * Create a root group
    		 */
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			newGroupJSON.put("displayName", myDisplayName); 
    			sendRequest(new PostRequest(URL_ROOTGROUPS + "/" + myGroupName,  newGroupJSON.toString(), "application/json"), Status.STATUS_CREATED);
    		}
    		
    		/**
    		 * Now change its display name
    		 */
    		{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
    			newGroupJSON.put("displayName", myNewDisplayName); 
    			Response response = sendRequest(new PutRequest(URL_GROUPS + "/" + myGroupName,  newGroupJSON.toString(), "application/json"), Status.STATUS_OK);    
    			JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		JsonNode data = top.get("data");
        		assertTrue(data.size() > 0);
        		assertEquals("displayName wrong", myNewDisplayName, data.get("displayName").textValue());

    		}
    		
        	/**
        	 * Now get it and verify that the name has changed
        	 */
        	{
        		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" +  myGroupName), Status.STATUS_OK);
        		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        		logger.debug(response.getContentAsString());
        		JsonNode data = top.get("data");
        		assertTrue(data.size() > 0);
        		assertEquals("displayName wrong", myNewDisplayName, data.get("displayName").textValue());

        	}   
        	
    		/**
    		 * Negative test
    		 */
        	{
                ObjectNode newGroupJSON = AlfrescoDefaultObjectMapper.createObjectNode();
        		newGroupJSON.put("displayName", myNewDisplayName); 
        		sendRequest(new PutRequest(URL_GROUPS + "/" + "rubbish",  newGroupJSON.toString(), "application/json"), Status.STATUS_NOT_FOUND);    
        	}
    	}
    	finally
        {
        	sendRequest(new DeleteRequest(URL_ROOTGROUPS + "/" + myGroupName), 0);
        }
    }
    
    
    /**
     * Detailed test of search groups
     *<li>if the optional includeInternal parameter is true then will include internal groups, otherwise internalGroups are not returned.</li>
      <li>If the optional shortNameFilter parameter is set then returns those root groups with a partial match on shortName.</li>
     */
    public void testSearchGroups() throws Exception
    {
    	 createTestTree();
    	 
    	// Search on partial short name
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + "ALFRESCO_ADMIN*"), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", ADMIN_GROUP, authority.get("shortName").textValue());
    	}
    	
    	// Search on partial short name with a ?
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + "ALFRE?CO_ADMIN*"), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", ADMIN_GROUP, authority.get("shortName").textValue());
    	}
    	
    	// Negative test.
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + "XX?X"), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 0", 0, data.size());
    	}
    	
    	// Search on full shortName
		{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + ADMIN_GROUP), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", ADMIN_GROUP, authority.get("shortName").textValue());
		}
		
    	// Search on partial short name of a non root group
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + TEST_GROUPD ), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    //System.out.println(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", TEST_GROUPD, authority.get("shortName").textValue());

    	}
    	
    	// Search on partial short name of a non root group in default zone
    	{
    		String url = URL_GROUPS + "?shortNameFilter=" + TEST_GROUPD + "& zone=" + AuthorityService.ZONE_APP_DEFAULT;
		    Response response = sendRequest(new GetRequest(url ), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    //System.out.println(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", TEST_GROUPD, authority.get("shortName").textValue());
    	}
    	
    	// Search for a group (which is not in the default zone) in all zones
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + TEST_GROUPE ), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
		    //System.out.println(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 1", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("Group E not found", TEST_GROUPE, authority.get("shortName").textValue());
			
			// Double check group E is in the share zone
			Set<String> zones = authorityService.getAuthorityZones(authority.get("fullName").textValue());
			assertTrue(zones.contains(AuthorityService.ZONE_APP_SHARE));
    	}
    	

//TODO TEST Case failing ?
//    	// Search for Group E in a specific zone (without name filter)
//    	{
//		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?zone=" + AuthorityService.ZONE_APP_SHARE), Status.STATUS_OK);
//		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
//		    logger.debug(response.getContentAsString());
//		    System.out.println(response.getContentAsString());
//		    ArrayNode data = top.get("data");
//		    assertEquals("Can't find any groups in Share zone", 1, data.size());
// 			JsonNode authority = data.get(0);
//			assertEquals("", TEST_GROUPE, authority.get("shortName"));
//    	}
 
    	// Search for a group in a specifc non default zone
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + TEST_GROUPE + "&zone=" + AuthorityService.ZONE_APP_SHARE), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
//		    System.out.println(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("Can't find Group E in Share zone", 1, data.size());
 			JsonNode authority = data.get(0);
			assertEquals("", TEST_GROUPE, authority.get("shortName").textValue());
    	}
    	
    	// Negative test Search for a group in a wrong zone
    	{
		    Response response = sendRequest(new GetRequest(URL_GROUPS + "?shortNameFilter=" + TEST_GROUPE + "&zone=" + "SOME.THING"), Status.STATUS_OK);
		    JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		    logger.debug(response.getContentAsString());
//		    System.out.println(response.getContentAsString());
		    ArrayNode data = (ArrayNode) top.get("data");
		    assertEquals("size not 0", 0, data.size());
    	}
    	
    
    }
    
    public void testSearchGroupsPaging() throws Exception
    {
        createTestTree();

        ArrayNode data = getDataArray(URL_GROUPS + "?shortNameFilter=*");
        int defaultSize = data.size();
        String firstGroup = data.get(0).toString();

        assertTrue("There should be at least 6 groups in default zone!", defaultSize > 5);

        // Test maxItems works
        data = getDataArray(URL_GROUPS + "?shortNameFilter=*" +"&maxItems=3");
        assertEquals("There should only be 3 groups!", 3, data.size());
        assertEquals("The first group should be the same!!", firstGroup, data.get(0).toString());
        
        // Test skipCount works
        data = getDataArray(URL_GROUPS + "?shortNameFilter=*" + "&skipCount=2");
        assertEquals("The number of groups returned is wrong!", defaultSize-2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount
        data = getDataArray(URL_GROUPS + "?shortNameFilter=*" +"&skipCount=2&maxItems=3");
        assertEquals("The number of groups returned is wrong!", 3, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount when maxItems is too big.
        // Shoudl return last 2 items.
        int skipCount = defaultSize-2;
        data = getDataArray(URL_GROUPS + "?shortNameFilter=*" + "&skipCount="+skipCount+"&maxItems=5");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
    }
    
    public void testGetRootGroupsPaging() throws Exception
    {
        createTestTree();
        
        ArrayNode data = getDataArray(URL_ROOTGROUPS);
        int defaultSize = data.size();
        String firstGroup = data.get(0).toString();
        
        assertTrue("There should be at least 3 groups in default zone!", defaultSize > 2);
        
        // Test maxItems works
        data = getDataArray(URL_ROOTGROUPS + "?maxItems=2");
        assertEquals("There should only be 2 groups!", 2, data.size());
        assertEquals("The first group should be the same!!", firstGroup, data.get(0).toString());
        
        // Test skipCount works
        data = getDataArray(URL_ROOTGROUPS + "?skipCount=1");
        assertEquals("The number of groups returned is wrong!", defaultSize-1, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount
        data = getDataArray(URL_ROOTGROUPS + "?skipCount=1&maxItems=2");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount when maxItems is too big.
        // Shoudl return last 2 items.
        int skipCount = defaultSize-1;
        data = getDataArray(URL_ROOTGROUPS + "?skipCount="+skipCount+"&maxItems=5");
        assertEquals("The number of groups returned is wrong!", 1, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
    }
    
    public void testGetParentsPaging() throws Exception
    {
        createTestTree();

        // Test for immediate parents
        String baseUrl = URL_GROUPS + "/" + TEST_GROUPD + "/parents";
        
        ArrayNode data = getDataArray(baseUrl);
        int defaultSize = data.size();
        String firstGroup = data.get(0).toString();

        assertEquals("There should be at least 3 groups in default zone!", 3, defaultSize);

        // Test maxItems works
        data = getDataArray(baseUrl +"?maxItems=2");
        assertEquals("There should only be 2 groups!", 2, data.size());
        assertEquals("The first group should be the same!!", firstGroup, data.get(0).toString());
        
        // Test skipCount works
        data = getDataArray(baseUrl + "?skipCount=1");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount
        data = getDataArray(baseUrl + "?skipCount=1&maxItems=1");
        assertEquals("The number of groups returned is wrong!", 1, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount when maxItems is too big.
        // Should return last 2 items.
        data = getDataArray(baseUrl + "?skipCount=1&maxItems=5");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));

        //Test for ALL parents
        baseUrl = URL_GROUPS + "/" + TEST_GROUPD + "/parents?level=ALL";
        
        data = getDataArray(baseUrl);
        defaultSize = data.size();
        firstGroup = data.get(0).toString();
        
        assertTrue("There should be at least 3 groups in default zone!", defaultSize > 2);
        
        // Test maxItems works
        data = getDataArray(baseUrl +"&maxItems=2");
        assertEquals("There should only be 2 groups!", 2, data.size());
        assertEquals("The first group should be the same!!", firstGroup, data.get(0).toString());
        
        // Test skipCount works
        data = getDataArray(baseUrl + "&skipCount=1");
        assertEquals("The number of groups returned is wrong!", defaultSize-1, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount
        data = getDataArray(baseUrl + "&skipCount=1&maxItems=2");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount when maxItems is too big.
        // Shoudl return last 2 items.
        int skipCount = defaultSize-2;
        data = getDataArray(baseUrl + "&skipCount="+skipCount+"&maxItems=5");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
    }

    public void testGetChildGroupsPaging() throws Exception
    {
        createTestTree();
        
        // Test for immediate parents
        String baseUrl = URL_GROUPS + "/" + TEST_ROOTGROUP + "/children?authorityType=GROUP";
        
        ArrayNode data = getDataArray(baseUrl);
        int defaultSize = data.size();
        String firstGroup = data.get(0).toString();
        
        assertEquals("There should be 6 groups in default zone!", 6, defaultSize);
        
        // Test maxItems works
        data = getDataArray(baseUrl +"&maxItems=2");
        assertEquals("There should only be 3 groups!", 2, data.size());
        assertEquals("The first group should be the same!!", firstGroup, data.get(0).toString());
        
        // Test skipCount works
        data = getDataArray(baseUrl + "&skipCount=2");
        assertEquals("The number of groups returned is wrong!", 4, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount
        data = getDataArray(baseUrl + "&skipCount=2&maxItems=2");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
        
        // Test maxItems and skipCount when maxItems is too big.
        // Shoudl return last 2 items.
        data = getDataArray(baseUrl + "&skipCount=4&maxItems=5");
        assertEquals("The number of groups returned is wrong!", 2, data.size());
        assertFalse("The first group should not be the same!!", firstGroup.equals(data.get(0).toString()));
    }

    private ArrayNode getDataArray(String url) throws IOException
    {
        Response response = sendRequest(new GetRequest(url), Status.STATUS_OK);
        JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        ArrayNode data = (ArrayNode) top.get("data");
        return data;
    }
    /**
     * Detailed test of get Parents
     */
    public void testGetParents() throws Exception
    {
        createTestTree();
        
    	/**
    	 * Get all parents for the root group ALFRESCO_ADMINISTRATORS groups which has no parents
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + ADMIN_GROUP + "/parents"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		// Top level group has no parents
    		assertTrue("top level group has no parents", data.size() == 0);
    	}
    	
    	/**
    	 * Get GROUP B   Which should be a child of TESTROOT
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB + "/parents"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() > 0);
    	}
    	
       	/**
    	 * Get GROUP D   Which should be a child of GROUPB child of TESTROOT
    	 */
    	{
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPD + "/parents?level=ALL"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() >= 2);
    	}

// TODO parents script does not have zone parameter    	
//      /**
//    	 * Get GROUP E   Which should be a child of GROUPB child of TESTROOT but in a different zone
//    	 */
//    	{
//    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPE + "/parents?level=ALL"), Status.STATUS_OK);
//    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
//    		logger.debug(response.getContentAsString());
//    		ArrayNode data = top.get("data");
//    		assertTrue(data.size() >= 2);
//    	}
    	
      	/**
    	 * Negative test Get GROUP D level="rubbish"
    	 */
    	{
    		sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPD + "/parents?level=rubbish"), Status.STATUS_BAD_REQUEST);
    	}
    	
    	/**
    	 * Negative test GROUP(Rubbish) does not exist
    	 */
    	{ 
    		sendRequest(new GetRequest(URL_GROUPS + "/" + "rubbish" + "/parents?level=all"), Status.STATUS_NOT_FOUND);
    	
    	}
    }
    
    /**
     * Detailed test of get Children
     */
    public void testGetChildren() throws Exception
    {
        createTestTree();
            	
    	/**
    	 * Get All Children of GROUP B
    	 */
    	{
    		logger.debug("Get all children of GROUP B");
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB + "/children"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() > 0);
    		boolean gotGroupD = false;
    		boolean gotGroupE = false;
    		boolean gotUserTwo = false;
    		boolean gotUserThree = false;
      		for(int i = 0; i < data.size(); i++)
    		{
    			JsonNode authority = data.get(i);
    			if(authority.get("shortName").textValue().equals(TEST_GROUPD))
    			{
    				gotGroupD = true;
    			}
    			if(authority.get("shortName").textValue().equals(TEST_GROUPE))
    			{
    				gotGroupE = true;
    			}
    			if(authority.get("shortName").textValue().equals(USER_TWO))
    			{
    				gotUserTwo = true;
    			}
    			if(authority.get("shortName").textValue().equals(USER_THREE))
    			{
    				gotUserThree = true;
    			}
    		}
      		assertEquals("4 groups not returned", 4, data.size());
      		assertTrue("not got group D", gotGroupD);
      		assertTrue("not got group E", gotGroupE);
      		assertTrue("not got user two", gotUserTwo);
      		assertTrue("not got user three", gotUserThree);

    	}
    	
    	/**
    	 * Get All Children of GROUP B which are GROUPS
    	 */
    	{
    		logger.debug("Get child GROUPS of GROUP B");
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB + "/children?authorityType=GROUP"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue("no child groups of group B", data.size() > 1 );
    		
    		boolean gotGroupD = false;
    		boolean gotGroupE = false;
    		JsonNode subGroup = data.get(0);
      		for(int i = 0; i < data.size(); i++)
    		{
      			JsonNode authority = data.get(i);
    			if(authority.get("shortName").textValue().equals(TEST_GROUPD))
    			{
    				gotGroupD = true;
    			}
    			else if(authority.get("shortName").textValue().equals(TEST_GROUPE))
    			{
    				gotGroupE = true;
    			}
    			else
    			{
    				fail("unexpected authority returned:" + authority.get("shortName"));
    			}
    		}
      		assertTrue("not got group D", gotGroupD);
      		assertTrue("not got group E", gotGroupE);
      		
    		assertEquals("authorityType wrong", "GROUP", subGroup.get("authorityType").textValue());
      		for(int i = 0; i < data.size(); i++)
    		{
      			JsonNode authority = data.get(i);
      			assertEquals("authorityType wrong", "GROUP", authority.get("authorityType").textValue());
    		}
    	}
    	
    	/**
    	 * Get All Children of GROUP B which are USERS
    	 */
    	{
    		logger.debug("Get Child Users of Group B");
    		Response response = sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB + "/children?authorityType=USER"), Status.STATUS_OK);
    		JsonNode top = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
    		logger.debug(response.getContentAsString());
    		ArrayNode data = (ArrayNode) top.get("data");
    		assertTrue(data.size() > 1);
      		for(int i = 0; i < data.size(); i++)
    		{
      			JsonNode authority = data.get(i);
      			assertEquals("authorityType wrong", "USER", authority.get("authorityType").textValue());
    		}
    	}
    	
    	/**
    	 * Negative test All Children of GROUP B, bad authorityType
    	 */
    	{
    		sendRequest(new GetRequest(URL_GROUPS + "/" + TEST_GROUPB + "/children?authorityType=XXX"), Status.STATUS_BAD_REQUEST);
    	}
    		
    	/**
    	 * Negative test GROUP(Rubbish) does not exist
    	 */
    	{ 
    		sendRequest(new GetRequest(URL_GROUPS + "/" + "rubbish" + "/children"), Status.STATUS_NOT_FOUND);
    	}
    }
}
