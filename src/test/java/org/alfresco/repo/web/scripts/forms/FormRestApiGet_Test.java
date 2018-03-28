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
package org.alfresco.repo.web.scripts.forms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;
import org.springframework.extensions.webscripts.json.JSONUtils;

public class FormRestApiGet_Test extends AbstractTestFormRestApi 
{
    protected ObjectNode createItemJSON(NodeRef nodeRef) throws Exception
    {
        ObjectNode jsonPostData = AlfrescoDefaultObjectMapper.createObjectNode();
        
        jsonPostData.put("itemKind", "node");
        
        StringBuilder builder = new StringBuilder();
        builder.append(nodeRef.getStoreRef().getProtocol()).append("/").append(
                    nodeRef.getStoreRef().getIdentifier()).append("/").append(nodeRef.getId());
        jsonPostData.put("itemId", builder.toString());
        
        return jsonPostData;
    }
    
    public void testResponseContentType() throws Exception
    {
        JsonNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 200);
        assertEquals("application/json;charset=UTF-8", rsp.getContentType());
    }

    public void testGetFormForNonExistentNode() throws Exception
    {
        // Create a NodeRef with all digits changed to an 'x' char - 
        // this should make for a non-existent node.
        String missingId = this.referencingDocNodeRef.getId().replaceAll("\\d", "x");
        NodeRef missingNodeRef = new NodeRef(this.referencingDocNodeRef.getStoreRef(), 
                    missingId);
        
        JsonNode jsonPostData = createItemJSON(missingNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 404);
        assertEquals("application/json;charset=UTF-8", rsp.getContentType());
    }

    public void testJsonContentParsesCorrectly() throws Exception
    {
        JsonNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 200);
        String jsonResponseString = rsp.getContentAsString();
        
        Object jsonObject = new JSONUtils().toObject(jsonResponseString);
        assertNotNull("JSON object was null.", jsonObject);
    }

    public void testJsonUpperStructure() throws Exception
    {
        JsonNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 200);
        String jsonResponseString = rsp.getContentAsString();
        
        JsonNode jsonParsedObject = AlfrescoDefaultObjectMapper.getReader().readTree(jsonResponseString);
        assertNotNull(jsonParsedObject);
        
        Object dataObj = jsonParsedObject.get("data");
        assertEquals(JsonNode.class, dataObj.getClass());
        JsonNode rootDataObject = (JsonNode) dataObj;

        assertEquals(5, rootDataObject.size());
        String item = rootDataObject.get("item").textValue();
        String submissionUrl = rootDataObject.get("submissionUrl").textValue();
        String type = rootDataObject.get("type").textValue();
        JsonNode definitionObject = rootDataObject.get("definition");
        JsonNode formDataObject = rootDataObject.get("formData");
        
        assertNotNull(item);
        assertNotNull(submissionUrl);
        assertNotNull(type);
        assertNotNull(definitionObject);
        assertNotNull(formDataObject);
    }

    @SuppressWarnings("unchecked")
    public void testJsonFormData() throws Exception
    {
        JsonNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 200);
        String jsonResponseString = rsp.getContentAsString();

        JsonNode jsonParsedObject = AlfrescoDefaultObjectMapper.getReader().readTree(jsonResponseString);
        assertNotNull(jsonParsedObject);
        
        JsonNode rootDataObject = jsonParsedObject.get("data");
        
        JsonNode formDataObject = rootDataObject.get("formData");
        List<String> keys = new ArrayList<String>();
        for (Iterator iter = formDataObject.fieldNames(); iter.hasNext(); )
        {
            String nextFieldName = (String)iter.next();
            assertEquals("Did not expect to find a colon char in " + nextFieldName,
                        -1, nextFieldName.indexOf(':'));
            keys.add(nextFieldName);
        }
        // Threshold is a rather arbitrary number. I simply want to ensure that there
        // are *some* entries in the formData hash.
        final int threshold = 5;
        int actualKeyCount = keys.size();
        assertTrue("Expected more than " + threshold +
                " entries in formData. Actual: " + actualKeyCount, actualKeyCount > threshold);
    }
    
    @SuppressWarnings("unchecked")
    public void testJsonDefinitionFields() throws Exception
    {
        JsonNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, 
                    jsonPostString, APPLICATION_JSON), 200);
        String jsonResponseString = rsp.getContentAsString();
        
        JsonNode jsonParsedObject = AlfrescoDefaultObjectMapper.getReader().readTree(jsonResponseString);
        assertNotNull(jsonParsedObject);
        
        JsonNode rootDataObject = jsonParsedObject.get("data");
        
        JsonNode definitionObject = rootDataObject.get("definition");
        
        ArrayNode fieldsArray = (ArrayNode) definitionObject.get("fields");
        
        for (int i = 0; i < fieldsArray.size(); i++)
        {
            JsonNode nextJsonObject = fieldsArray.get(i);
            List<String> fieldKeys = new ArrayList<String>();
            for (Iterator iter2 = nextJsonObject.fieldNames(); iter2.hasNext(); )
            {
                fieldKeys.add((String)iter2.next());
            }
            for (String s : fieldKeys)
            {
                if (s.equals("mandatory") || s.equals("protectedField"))
                {
                    assertEquals("JSON booleans should be actual booleans.", java.lang.Boolean.class, nextJsonObject.get(s).getClass());
                }
            }
        }
    }
    
    public void testJsonSelectedFields() throws Exception
    {
        ObjectNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        ArrayNode jsonFields = AlfrescoDefaultObjectMapper.createArrayNode();
        jsonFields.add("cm:name");
        jsonFields.add("cm:title");
        jsonFields.add("cm:publisher");
        jsonPostData.set("fields", jsonFields);
        
        // Submit the JSON request.
        String jsonPostString = jsonPostData.toString();        
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, jsonPostString,
                APPLICATION_JSON), 200);
        
        String jsonResponseString = rsp.getContentAsString();
        JsonNode jsonParsedObject = AlfrescoDefaultObjectMapper.getReader().readTree(jsonResponseString);
        assertNotNull(jsonParsedObject);
        
        JsonNode rootDataObject = jsonParsedObject.get("data");
        JsonNode definitionObject = rootDataObject.get("definition");
        ArrayNode fieldsArray = (ArrayNode)definitionObject.get("fields");
        assertEquals("Expected 2 fields", 2, fieldsArray.size());
        
        // get the name and title definitions
        JsonNode nameField = fieldsArray.get(0);
        JsonNode titleField = fieldsArray.get(1);
        String nameFieldDataKey = nameField.get("dataKeyName").textValue();
        String titleFieldDataKey = titleField.get("dataKeyName").textValue();
        
        // get the data and check it
        JsonNode formDataObject = rootDataObject.get("formData");
        assertNotNull("Expected to find cm:name data", formDataObject.get(nameFieldDataKey));
        assertNotNull("Expected to find cm:title data", formDataObject.get(titleFieldDataKey));
        assertEquals(TEST_FORM_TITLE, formDataObject.get("prop_cm_title"));
    }
    
    public void testJsonForcedFields() throws Exception
    {
        ObjectNode jsonPostData = createItemJSON(this.referencingDocNodeRef);
        
        ArrayNode jsonFields = AlfrescoDefaultObjectMapper.createArrayNode();
        jsonFields.add("cm:name");
        jsonFields.add("cm:title");
        jsonFields.add("cm:publisher");
        jsonFields.add("cm:wrong");
        jsonPostData.set("fields", jsonFields);
        
        ArrayNode jsonForcedFields = AlfrescoDefaultObjectMapper.createArrayNode();
        jsonForcedFields.add("cm:publisher");
        jsonForcedFields.add("cm:wrong");
        jsonPostData.set("force", jsonForcedFields);
        
        // Submit the JSON request.
        String jsonPostString = jsonPostData.toString();
        Response rsp = sendRequest(new PostRequest(FORM_DEF_URL, jsonPostString,
                APPLICATION_JSON), 200);
        
        String jsonResponseString = rsp.getContentAsString();
        JsonNode jsonParsedObject = AlfrescoDefaultObjectMapper.getReader().readTree(jsonResponseString);
        assertNotNull(jsonParsedObject);
        
        JsonNode rootDataObject = jsonParsedObject.get("data");
        JsonNode definitionObject = rootDataObject.get("definition");
        ArrayNode fieldsArray = (ArrayNode)definitionObject.get("fields");
        assertEquals("Expected 3 fields", 3, fieldsArray.size());
        
        // get the name and title definitions
        JsonNode nameField = fieldsArray.get(0);
        JsonNode titleField = fieldsArray.get(1);
        String nameFieldDataKey = nameField.get("dataKeyName").textValue();
        String titleFieldDataKey = titleField.get("dataKeyName").textValue();
        
        // get the data and check it
        JsonNode formDataObject = rootDataObject.get("formData");
        assertNotNull("Expected to find cm:name data", formDataObject.get(nameFieldDataKey));
        assertNotNull("Expected to find cm:title data", formDataObject.get(titleFieldDataKey));
        assertEquals(TEST_FORM_TITLE, formDataObject.get("prop_cm_title"));
    }
}
