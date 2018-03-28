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
package org.alfresco.rest.workflow.api.tests;

import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;

public abstract class ListParser<T>
{

    public ListResponse<T> parseList(JsonNode jsonResponse) throws IOException
    {
        List<T> entries = new ArrayList<T>();

        JsonNode jsonList = jsonResponse.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        for(int i = 0; i < jsonEntries.size(); i++)
        {
            JsonNode jsonEntry = jsonEntries.get(i);
            JsonNode entry = jsonEntry.get("entry");
            entries.add(parseEntry(entry));
        }

        ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);

        ListResponse<T> resp = new ListResponse<T>(paging, entries);
        return resp;
    }

    public abstract T parseEntry(JsonNode entry) throws IOException;
}
