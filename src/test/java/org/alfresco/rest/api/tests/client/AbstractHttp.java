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
package org.alfresco.rest.api.tests.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.httpclient.HttpMethod;

public class AbstractHttp
{
    public static final String JSON_DATA = "data";

    /**
     * Extract the "data" JSON-object from the method's response.
     * @param method the method containing the response
     * @return the "data" object. Returns null if response is not JSON or no data-object
     *         is present.
     */
    public JsonNode getDataFromResponse(HttpMethod method)
    {
        JsonNode result = null;
        JsonNode object = getObjectFromResponse(method);
        
        // Extract object for "data" property
        object = object.get(JSON_DATA);
        if(object instanceof ObjectNode)
        {
            result = object;
        }
        return result;
    }
    
    /**
     * Extract the "data" JSON-array from the method's response.
     * @param method the method containing the response
     * @return the "data" object. Returns null if response is not JSON or no data-object
     *         is present.
     */
    public ArrayNode getDataArrayFromResponse(HttpMethod method)
    {
        ArrayNode result = null;
        JsonNode object = getObjectFromResponse(method);
        if(object != null)
        {
            // Extract object for "data" property
            object = object.get(JSON_DATA);
            if(object instanceof ArrayNode)
            {
                result = (ArrayNode) object;
            }
        }
        return result;
    }
    
    /**
     * Extract JSON-object from the method's response.
     * @param method the method containing the response
     * @return the json object. Returns null if response is not JSON or no data-object
     *         is present.
     */
    public JsonNode getObjectFromResponse(HttpMethod method)
    {
        JsonNode result = null;

        try
        {
            InputStream response = method.getResponseBodyAsStream();
            if(response != null)
            {
                JsonNode object = AlfrescoDefaultObjectMapper.getReader().readTree(response);
                if (object instanceof ObjectNode)
                {
                    return object;
                }
            }
        }
        catch (IOException error)
        {
            // Ignore errors, returning null
        }
       
        return result;
    }
    
    /**
     * Gets a string-value from the given JSON-object for the given key.
     * @param json the json object
     * @param key key pointing to the value
     * @param defaultValue if value is not set or if value is not of type "String", this value is returned
     */
    public String getString(JsonNode json, String key, String defaultValue)
    {
        String result = defaultValue;
        
        if(json != null)
        {
            JsonNode value = json.get(key);
            if(value instanceof TextNode)
            {
                result = value.textValue();
            }
        }
        return result;
    }

    /**
     * @param json JSON to extract array from
     * @param key key under which array is present on JSON
     * @return the {@link ArrayNode}. Returns null, if the value is null or not an array.
     */
    public ArrayNode getArray(JsonNode json, String key)
    {
        Object object = json.get(key);
        if(object instanceof ArrayNode)
        {
            return (ArrayNode) object;
        }
        return null;
    }

    /**
     * @param json JSON to extract object from
     * @param key key under which object is present on JSON
     * @return the {@link ObjectNode}. Returns null, if the value is null or not an object.
     */
    public ObjectNode getObject(JsonNode json, String key)
    {
        Object object = json.get(key);
        if(object instanceof ObjectNode)
        {
            return (ObjectNode) object;
        }
        return null;
    }
}
