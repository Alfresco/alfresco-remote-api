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
package org.alfresco.repo.web.scripts.dictionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.util.Iterator;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit test for Dictionary REST API
 * @author Saravanan Sellathurai
 */

public class DictionaryRestApiTest extends BaseWebScriptTest
{
	private static final String URL_SITES = "/api/classes";
	private static final String URL_PROPERTIES = "/api/properties";
	
	@Override
	protected void setUp() throws Exception 
	{ 
		super.setUp();
		getServer();
		AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
	}
	
	@Override
	protected void tearDown() throws Exception
    {
		super.tearDown();
		AuthenticationUtil.clearCurrentSecurityContext();
    }
	
	private void validatePropertyDef(JsonNode result) throws Exception
	{
		assertEquals("cm:created", result.get("name").textValue());
		assertEquals("Created Date", result.get("title").textValue());
		assertEquals("Created Date", result.get("description").textValue());
		assertEquals("d:datetime", result.get("dataType").textValue());
        assertFalse(result.get("multiValued").booleanValue());
        assertTrue(result.get("mandatory").booleanValue());
        assertTrue(result.get("enforced").booleanValue());
        assertTrue(result.get("protected").booleanValue());
        assertTrue(result.get("indexed").booleanValue());
        assertTrue(result.get("indexedAtomically").booleanValue());
		assertEquals("/api/property/cm_created", result.get("url").textValue());
		
	}
	
	private void validateChildAssociation(JsonNode result) throws Exception
	{
        assertEquals("cm:member", result.get("name").textValue());
        assertTrue(result.get("isChildAssociation").booleanValue());
        assertFalse(result.get("protected").booleanValue());
		
		assertEquals("cm:authorityContainer", result.get("source").get("class").textValue());
        assertFalse(result.get("source").get("mandatory").booleanValue());
        assertTrue(result.get("source").get("many").booleanValue());
		
		assertEquals("cm:authority", result.get("target").get("class").textValue());
        assertFalse(result.get("target").get("mandatory").booleanValue());
        assertTrue(result.get("target").get("many").booleanValue());
		
		assertTrue(result.get("url").toString().startsWith("/api/classes/"));
		assertTrue(result.get("url").toString().indexOf("/association/cm_member") > 0);;
	}
	
	private void validateAssociation(JsonNode result) throws Exception
	{
		assertEquals("cm:workingcopylink", result.get("name").textValue());
        assertFalse(result.get("isChildAssociation").booleanValue());
        assertFalse(result.get("protected").booleanValue());
		
		assertEquals("cm:checkedOut", result.get("source").get("class").textValue());
        assertTrue(result.get("source").get("mandatory").booleanValue());
        assertFalse(result.get("source").get("many").booleanValue());
		
		assertEquals("cm:workingcopy", result.get("target").get("class").textValue());
        assertTrue(result.get("target").get("mandatory").booleanValue());
        assertFalse(result.get("target").get("many").booleanValue());
		
		assertEquals("/api/classes/cm_checkedOut/association/cm_workingcopylink", result.get("url").textValue());
	}
	private void validateAssociationDef(JsonNode result) throws Exception
	{
		assertEquals("cm:avatar", result.get("name").textValue());
		assertEquals("Avatar", result.get("title").textValue());
		assertEquals("The person's avatar image", result.get("description").textValue());
        assertFalse(result.get("isChildAssociation").booleanValue());
        assertFalse(result.get("protected").booleanValue());
		
		assertEquals("cm:person", result.get("source").get("class").textValue());
		assertEquals("cm:avatarOf", result.get("source").get("role").textValue());
        assertFalse(result.get("source").get("mandatory").booleanValue());
        assertFalse(result.get("source").get("many").booleanValue());
		
		assertEquals("cm:content", result.get("target").get("class").textValue());
		assertEquals("cm:hasAvatar", result.get("target").get("role").textValue());
        assertFalse(result.get("target").get("mandatory").booleanValue());
        assertFalse(result.get("target").get("many").booleanValue());
		
		assertEquals("/api/classes/cm_person/association/cm_avatar", result.get("url").textValue());
	}
	
	private void validateTypeClass(JsonNode result) throws Exception
	{
		//cm:cmobject is of type =>type
		assertEquals("cm:cmobject", result.get("name").textValue());
        assertFalse(result.get("isAspect").booleanValue());
		assertEquals("Object", result.get("title").textValue());
		assertEquals("", result.get("description").textValue());
		
		assertEquals("sys:base", result.get("parent").get("name").textValue());
		assertEquals("base", result.get("parent").get("title").textValue());
		assertEquals("/api/classes/sys_base", result.get("parent").get("url").textValue());
		
		assertEquals("sys:referenceable", result.get("defaultAspects").get("sys:referenceable").get("name").textValue());
		assertEquals("Referenceable", result.get("defaultAspects").get("sys:referenceable").get("title").textValue());
		assertEquals("/api/classes/cm_cmobject/property/sys_referenceable", result.get("defaultAspects").get("sys:referenceable").get("url").textValue());

		assertEquals("cm:auditable", result.get("defaultAspects").get("cm:auditable").get("name").textValue());
		assertEquals("Auditable", result.get("defaultAspects").get("cm:auditable").get("title").textValue());
		assertEquals("/api/classes/cm_cmobject/property/cm_auditable", result.get("defaultAspects").get("cm:auditable").get("url").textValue());
		
		//assertEquals("cm:name", result.get("properties").get("cm:name").get("name"));
		//assertEquals("Name", result.get("properties").get("cm:name").get("title"));
		//assertEquals("/api/classes/cm_cmobject/property/cm_name", result.get("properties").get("cm:name").get("url"));
		
		//assertEquals(, result.get("associations").size());
		//assertEquals(0, result.get("childassociations").size());
		
		assertEquals("/api/classes/cm_cmobject", result.get("url").textValue());
		
	}
	
	private void validateAspectClass(JsonNode result) throws Exception
	{
		//cm:thumbnailed is of type =>aspect
		assertEquals("cm:thumbnailed", result.get("name").textValue());
        assertTrue(result.get("isAspect").booleanValue());
		assertEquals("Thumbnailed", result.get("title").textValue());
		assertEquals("", result.get("description").textValue());
		assertEquals(0, result.get("defaultAspects").size());
		
		if (result.get("properties").has("cm:automaticUpdate"))
		{
    		assertEquals("cm:automaticUpdate", result.get("properties").get("cm:automaticUpdate").get("name").textValue());
    		assertEquals("Automatic Update", result.get("properties").get("cm:automaticUpdate").get("title").textValue());
    		assertEquals("/api/classes/cm_thumbnailed/property/cm_automaticUpdate", result.get("properties").get("cm:automaticUpdate").get("url").textValue());
		}
		
		//assertEquals(2, result.get("associations").size());
	}

    private void validatePropertiesConformity(ArrayNode classDefs) throws Exception
    {
        final int itemsToTest = 10;
        for (int i = 0; (i < itemsToTest) && (i < classDefs.size()); ++i)
        {
            JsonNode classDef1 = classDefs.get(i);
            JsonNode propertyNames1 = classDef1.get("properties");
            Iterator<String> propNamesIterator1 = propertyNames1.fieldNames();
            // properties of class obtained by api/classes
            List<String> propertyValues1 = new ArrayList<>(propertyNames1.size());
            while (propNamesIterator1.hasNext())
            {
                String propName = propNamesIterator1.next();
                JsonNode valueJson = propertyNames1.get(propName).get("name");
                propertyValues1.add(valueJson.textValue());
            }

            String classUrl = classDef1.get("url").textValue();
            assertTrue(classUrl.contains(URL_SITES));
            Response responseFromGetClassDef = sendRequest(new GetRequest(classUrl), 200);
            JsonNode classDef2 = AlfrescoDefaultObjectMapper.getReader()
                    .readTree(responseFromGetClassDef.getContentAsString());
            assertTrue(classDef2.size() > 0);
            assertEquals(200, responseFromGetClassDef.getStatus());
            assertEquals(classDef1.get("name"), classDef2.get("name"));
            JsonNode propertyNames2 = classDef2.get("properties");
            Iterator<String> propNamesIterator2 = propertyNames2.fieldNames();
            // properties of class obtained by api/classes/class
            List<String> propertyValues2 = new ArrayList<>(propertyNames2.size());
            while (propNamesIterator2.hasNext())
            {
                String propName = propNamesIterator2.next();
                JsonNode valueJson = propertyNames2.get(propName).get("name");
                propertyValues2.add(valueJson.textValue());
            }

            Response responseFromGetPropertiesDef = sendRequest(new GetRequest(classUrl + "/properties"), 200);
            ArrayNode propertiesDefs = (ArrayNode) AlfrescoDefaultObjectMapper.getReader()
                    .readTree(responseFromGetPropertiesDef.getContentAsString());
            assertEquals(200, responseFromGetClassDef.getStatus());
            // properties of class obtained by api/classes/class/properties
            List<String> propertyValues3 = new ArrayList<>(propertiesDefs.size());
            for (int j = 0; j < propertiesDefs.size(); j++)
            {
                JsonNode valueJson = propertiesDefs.get(j).get("name");
                propertyValues3.add(valueJson.textValue());
            }

            assertEquivalenceProperties(propertyValues1, propertyValues2);
            assertEquivalenceProperties(propertyValues2, propertyValues3);
        }
    }

    private void assertEquivalenceProperties(List<Object> propertyValues1, List<Object> propertyValues2)
    {
        if ((propertyValues1.size() != propertyValues2.size()) || !propertyValues1.containsAll(propertyValues2))
        {
            fail("Wrong properties in classes");
        }
    }

	public void testGetPropertyDef() throws Exception
	{
		Response response = sendRequest(new GetRequest("/api/classes/cm_auditable/property/cm_created"), 200);
		assertEquals(200,response.getStatus());
		JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		validatePropertyDef(result);
		
		assertEquals(result.size()>0, true);
		response = sendRequest(new GetRequest("/api/classes/cm_hi/property/cm_welcome"), 404);
		assertEquals(404,response.getStatus());
		
		//invalid property name , returns a null JsonObject as such a property doesn't exist under cm_auditable
		response = sendRequest(new GetRequest("/api/classes/cm_auditable/property/cm_welcome"), 200);
		result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(0, result.size());
		assertEquals(200,response.getStatus());
	}
	
	public void testGetPropertyDefs() throws Exception
	{
		//validate for a particular property cm_created in the class cm_auditable
		GetRequest req = new GetRequest(URL_SITES + "/cm_auditable/properties");
		Map< String, String > arguments = new HashMap< String, String >();
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		Response response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
		
		JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(200,response.getStatus());
        assertEquals(5, result.size());
		
		//validate with no parameter => returns an array of property definitions
		arguments.clear();
		response = sendRequest(req, 200);
		result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(200,response.getStatus());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if(result.get(i).get("name").equals("cm:created")) 
			{
				validatePropertyDef(result.get(i));
			}
		}
		
		// test /api/properties
		req = new GetRequest(URL_PROPERTIES);
		response = sendRequest(req, 200);
		assertEquals(200, response.getStatus());		
		result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
        for (int i = 0; i < result.size(); i++)
        {
            if(result.get(i).get("name").equals("cm:created")) 
            {
                validatePropertyDef(result.get(i));
            }
            
            @SuppressWarnings("unused")
            String title = "";
            if (result.get(i).has("title") == true)
            {
                title = result.get(i).get("title").textValue();
            }
      }
        
        // test /api/properties?name=cm:name&name=cm:title&name=cm:description
        req = new GetRequest(URL_PROPERTIES + "?name=cm:name&name=cm:title&name=cm:description");
        response = sendRequest(req, 200);
        assertEquals(200, response.getStatus());        
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(3, result.size());
	}
	
	public void testGetClassDetail() throws Exception
	{
		GetRequest req = new GetRequest(URL_SITES + "/cm_thumbnailed");
		Response response = sendRequest(req, 200);
		JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		assertEquals(200,response.getStatus());
		validateAspectClass(result);
		
		req = new GetRequest(URL_SITES + "/cm_cmobject");
		response = sendRequest(req, 200);
		result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		assertEquals(200,response.getStatus());
		validateTypeClass(result);
		
		response = sendRequest(new GetRequest("/api/classes/cm_hi"), 404);
		assertEquals(404,response.getStatus());
	}
	

	public void testGetClassDetails() throws Exception
	{
		/**
		 *  There are eight scenarios with getting class details , all are optional fields
		 *  Classfilter   namespaceprefix   name   Returns  
		 *  1   yes				yes			 yes	single class
		 *  2   yes				yes			 no     Array of classes [returns array of classes of the particular namespaceprefix]
		 *  3   yes				no			 no     Array of classes [depends on classfilter, either type or aspect or all classes in the repo]
		 * 	4   no				no			 no		Array of classes [returns all classes of both type and aspects in the entire repository]
		 * 	5   no				yes			 yes	single class [returns a single class of a valid namespaceprefix:name combination]
		 * 	6   no				yes			 no		Array of classes [returns an array of all aspects and types under particular namespaceprefix]
		 * 	7   no				no			 yes    404 error [since name alone doesn't makes any meaning]
		 *  8   yes 		    no 			 yes	404 error [since name alone doesn't makes any meaning]
		 * 	Test cases are provided for all the above scenarios	
		 */
		
		
		//check for a aspect under cm with name thumbnailes [case-type:1]
		GetRequest req = new GetRequest(URL_SITES);
		Map< String, String > arguments = new HashMap< String, String >();
		arguments.put("cf", "aspect");
		arguments.put("nsp", "cm");
		arguments.put("n", "thumbnailed");
		req.setArgs(arguments);
		Response response = sendRequest(req, 200);
		ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed"))
			{
				validateAspectClass(result.get(i));
			}
		}
		//check array size
		assertEquals(200,response.getStatus());
		
		//check for a type under cm with name cmobject [case-type:1]
		arguments.clear();
		arguments.put("cf", "type");
		arguments.put("nsp", "cm");
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject")) 
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a type under cm with name cmobject [case-type:1]
		arguments.clear();
		arguments.put("cf", "all");
		arguments.put("nsp", "cm");
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject")) 
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a type under cm without options=>name, namespaceprefix [case-type:2]
		arguments.clear();
		arguments.put("cf", "type");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		// the above result has all the types under cm, so now check for the presence type cm:cmobject in the array of classes of all types
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a aspect under cm without options=>name [case-type:2]
		arguments.clear();
		arguments.put("cf", "aspect");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		// the above result has all the aspects under cm, so now check for the presence aspect cm:thumnailed in the array of classes of all aspects
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed")) 
			{
				validateAspectClass(result.get(i));
			}
		}
		
		//check for all aspects under cm without options=>name [case-type:2]
		arguments.clear();
		arguments.put("cf", "all");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed"))
			{
				validateAspectClass(result.get(i));
			}	
		}
		assertEquals(200,response.getStatus());
		
		//check for all type under cm without options=>name, namespaceprefix [case-type:3]
		arguments.clear();
		arguments.put("cf", "type");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
			    System.out.println(result.get(i).toString());
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for all aspect under cm without options=>name, namespaceprefix [case-type:3]
		arguments.clear();
		arguments.put("cf", "aspect");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed")) 
			{
				validateAspectClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for all aspect and type in the repository when nothing is given [case-type:4]
		arguments.clear();
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed"))
			{
				validateAspectClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for all aspect and type in the repository when nothing is given [case-type:4]
		arguments.clear();
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a classname [namespaceprefix:name => cm:cmobject] without classfilter option [case-type:5]
		arguments.clear();
		arguments.put("nsp", "cm");
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a classname [namespaceprefix:name => cm:thumbnailed] without classfilter option [case-type:5]
		arguments.clear();
		arguments.put("nsp", "cm");
		arguments.put("n", "thumbnailed");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed"))
			{
				validateAspectClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a namespaceprefix [namespaceprefix => cm] without classfilter and name option [case-type:6]
		arguments.clear();
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:thumbnailed"))
			{
				validateAspectClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a namespaceprefix [namespaceprefix => cm] without classfilter and name option [case-type:6]
		arguments.clear();
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject")) 
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a namespaceprefix [namespaceprefix => cm] without classfilter and name option [case-type:6]
		arguments.clear();
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject")) 
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//check for a name alone without classfilter and namespaceprefix option [case-type:7] => returns 404 error
		arguments.clear();
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		
		//check for a type under cm with name cmobject and no namespaceprefix [case-type:8] => returns 404 error
		arguments.clear();
		arguments.put("cf", "type");
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		// Test with wrong data
		//check for all aspects under cm without option=>name
		arguments.clear();
		arguments.put("cf", "aspects");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//check for all types under cm without option=>name
		arguments.clear();
		arguments.put("cf", "types");
		arguments.put("nsp", "cmd");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//check for all dictionary data  without option=>name and option=>namespaceprefix
		arguments.clear();
		arguments.put("cf", "a�&llsara");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//check for all aspect dictionary data  without option=>name and option=>namespaceprefix
		arguments.clear();
		arguments.put("cf", "aspectb");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//check for all types dictionary data  without option=>name and option=>namespaceprefix
		arguments.clear();
		arguments.put("cf", "typesa");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//check for all types dictionary data  without option=>name and option=>namespaceprefix and option=>classfilter
		arguments.clear();
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
	}

	public void testSubClassDetails() throws Exception
	{
		GetRequest req = new GetRequest(URL_SITES + "/sys_base/subclasses");
		Map< String, String > arguments = new HashMap< String, String >();
		arguments.put("r", "true");
		req.setArgs(arguments);
		Response response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
		response = sendRequest(req, 200);
		ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		arguments.clear();
		arguments.put("r", "false");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		assertEquals(200,response.getStatus());
		arguments.clear();
		arguments.put("r", "false");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		arguments.clear();
		arguments.put("r", "true");
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		assertEquals(200,response.getStatus());
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:cmobject"))
			{
				validateTypeClass(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		// data with only name along
		arguments.clear();
		arguments.put("r", "true");
		arguments.put("n", "cmobject");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		// invalid name and namespaceprefix
		arguments.clear();
		arguments.put("r", "true");
		arguments.put("n", "dublincore"); //name and namespaceprefix are valid one , but its not present in sys_base as a sub-class
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//invalid name and a valid namespaceprefix
		arguments.clear();
		arguments.put("r", "true");
		arguments.put("n", "dublincoresara"); //name and namespaceprefix are invalid one
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		
	}
	
	public void testGetAssociationDef() throws Exception
	{
		GetRequest req = new GetRequest(URL_SITES + "/cm_person/association/cm_avatar");
		Response response = sendRequest(req, 200);
		JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(200,response.getStatus());
		validateAssociationDef(result);
		
		//wrong data
		response = sendRequest(new GetRequest(URL_SITES +"/cm_personalbe/association/cms_avatarsara"), 404);
		assertEquals(404,response.getStatus());
		
		//ask for an invalid association, which returns a null array 
		response = sendRequest(new GetRequest(URL_SITES +"/cm_person/association/cm_atari"), 200);
		result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString()); // change to return 404
		assertEquals(0,result.size());
		assertEquals(200,response.getStatus());
	}
	
	public void testGetAssociationDefs() throws Exception
	{
	    // CHILD ASSOCS
	    
	    // All associations on cm:authorityContainer
		GetRequest req = new GetRequest(URL_SITES + "/cm_authorityContainer/associations");
		Map< String, String > arguments = new HashMap< String, String >();
		arguments.put("af", "all");
		req.setArgs(arguments);
		Response response = sendRequest(req, 200);
		ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:member"))
			{
				validateChildAssociation(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		// Child associations on cm:authorityContainer
		arguments.clear();
		arguments.put("af", "child");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
            if (result.get(i).get("name").equals("cm:member"))
            {
                validateChildAssociation(result.get(i));
            }
		}
		assertEquals(200,response.getStatus());
		
        // look for childassociation cm:member
        arguments.clear();
        arguments.put("af", "child");
        arguments.put("nsp", "cm");
        arguments.put("n", "member");
        req.setArgs(arguments);
        response = sendRequest(req, 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(result.size()>0, true);
        for(int i=0; i<result.size(); i++)
        {
            if (result.get(i).get("name").equals("cm:workingcopylink"))
            {
                validateChildAssociation(result.get(i));
            }
        }
        assertEquals(200,response.getStatus());
        
        // cm:authorityContainer has a child_assoc relation with cm:member , but ask for general association, this then returns a null array 
        arguments.clear();
        arguments.put("af", "general");
        arguments.put("nsp", "cm");
        arguments.put("n", "member");
        req.setArgs(arguments);
        response = sendRequest(req, 200);
        result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        assertEquals(0,result.size());
        assertEquals(200,response.getStatus());
        
        // PEER ASSOCS
        
		// Peer associations on cm:checkedOut
        req = new GetRequest(URL_SITES + "/cm_checkedOut/associations");
		arguments.clear();
		arguments.put("af", "general");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
			if (result.get(i).get("name").equals("cm:workingcopylink"))
			{
				validateAssociation(result.get(i));
			}
		}
		assertEquals(200,response.getStatus());
		
		//look for association cm:workingcopylink
		arguments.clear();
		arguments.put("af", "general");
		arguments.put("nsp", "cm");
		arguments.put("n", "workingcopylink");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
            if (result.get(i).get("name").equals("cm:workingcopylink"))
            {
                validateAssociation(result.get(i));
            }
		}
		assertEquals(200,response.getStatus());
		
		//look for details on cm:checkedOut
		arguments.clear();
		arguments.put("nsp", "cm");
		arguments.put("n", "workingcopylink");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(result.size()>0, true);
		for(int i=0; i<result.size(); i++)
		{
            if (result.get(i).get("name").equals("cm:workingcopylink"))
            {
                validateAssociation(result.get(i));
            }
		}
		assertEquals(200,response.getStatus());
		
		// cm:copiedFrom has a general association relation with cm:object , but ask for child association, this then returns a null array 
		arguments.clear();
		arguments.put("af", "child");
		arguments.put("nsp", "cm");
		arguments.put("n", "workingcopylink");
		req.setArgs(arguments);
		response = sendRequest(req, 200);
		result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
		assertEquals(0,result.size());
		assertEquals(200,response.getStatus());
		
		//wrong data
		response = sendRequest(new GetRequest(URL_SITES +"/cmsa_personalbe/associations"), 404);
		assertEquals(404,response.getStatus());
		
		//data without name parameter
		arguments.clear();
		arguments.put("nsp", "cm");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
		
		//data with invalid association in wca_form
		arguments.clear();
		arguments.put("nsp", "cm");
		arguments.put("n", "hiwelcome");
		req.setArgs(arguments);
		response = sendRequest(req, 404);
		assertEquals(404,response.getStatus());
	}
	
    public void testGetClasses() throws Exception
    {
        GetRequest req = new GetRequest(URL_SITES);
        Response response = sendRequest(req, 200);
        ArrayNode result = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertTrue(result.size() > 0);
        assertEquals(200, response.getStatus());
        validatePropertiesConformity(result);
    }
	
}
