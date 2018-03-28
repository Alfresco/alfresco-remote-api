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

package org.alfresco.rest.api.tests.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.alfresco.rest.framework.jacksonextensions.JacksonHelper;

import static org.junit.Assert.assertNotNull;

/**
 * @author Jamal Kaabi-Mofrad
 */
public class JacksonUtil
{
    private JacksonHelper jsonHelper;

    public JacksonUtil(JacksonHelper jsonHelper)
    {
        this.jsonHelper = jsonHelper;
    }

    public <T> List<T> parseEntries(JsonNode jsonObject, Class<T> clazz)
    {
        assertNotNull(jsonObject);
        assertNotNull(clazz);

        List<T> models = new ArrayList<>();

        JsonNode jsonList = jsonObject.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        Iterator<JsonNode> iterator = jsonEntries.elements();
        while (iterator.hasNext())
        {
            JsonNode jsonEntry = iterator.next();
            T pojoModel = parseEntry(jsonEntry, clazz);
            models.add(pojoModel);
        }

        return models;
    }

    public <T> T parseEntry(JsonNode jsonObject, Class<T> clazz)
    {
        assertNotNull(jsonObject);
        assertNotNull(clazz);

        JsonNode entry = jsonObject.get("entry");
        T pojoModel = jsonHelper.construct(new StringReader(entry.toString()), clazz);
        assertNotNull(pojoModel);

        return pojoModel;
    }
}
