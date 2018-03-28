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
package org.alfresco.rest.api.tests.client.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

/**
 * A representation of an Audit Application Entry in JUnit Test
 * 
 * @author Andrei Forascu
 *
 */
public class AuditEntry extends org.alfresco.rest.api.model.AuditEntry implements Serializable, ExpectedComparison
{

    private static final long serialVersionUID = 1L;

    public AuditEntry(Long id, String auditApplicationId, org.alfresco.rest.api.model.UserInfo createdByUser, Date createdAt, Map<String, Serializable> values)
    {
        super(id, auditApplicationId, createdByUser, createdAt, values);
    }

    @Override
    public void expected(Object o)
    {
        assertTrue("o is an instance of " + o.getClass(), o instanceof AuditEntry);

        AuditEntry other = (AuditEntry) o;

        AssertUtil.assertEquals("id", getId(), other.getId());
        AssertUtil.assertEquals("auditApplicationId", getAuditApplicationId(), other.getAuditApplicationId());
        AssertUtil.assertEquals("values", getValues(), other.getValues());
        AssertUtil.assertEquals("createdByUser", getCreatedByUser().getId(), other.getCreatedByUser().getId());
        AssertUtil.assertEquals("createdAt", getCreatedAt(), other.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    public ObjectNode toJSON()
    {
        ObjectNode auditEntryJson = AlfrescoDefaultObjectMapper.createObjectNode();
        if (getId() != null)
        {
            auditEntryJson.put("id", getId());
        }
        auditEntryJson.put("auditApplicationId", getAuditApplicationId());
        if (createdByUser != null)
        {
            auditEntryJson.put("createdByUser", new UserInfo(createdByUser.getId(), createdByUser.getDisplayName()).toJSON());
        }
        auditEntryJson.put("values", AlfrescoDefaultObjectMapper.convertValue(getValues(), ObjectNode.class));
        auditEntryJson.put("createdAt", getCreatedAt().toString());

        return auditEntryJson;
    }

    @SuppressWarnings("unchecked")
    public static AuditEntry parseAuditEntry(JsonNode jsonObject)
    {
        Long id = jsonObject.get("id").longValue();
        String auditApplicationId = jsonObject.get("auditApplicationId").textValue();
        Map<String, Serializable> values = (Map<String, Serializable>) jsonObject.get("values");
        org.alfresco.rest.api.model.UserInfo createdByUser = null;
        JsonNode createdByUserJson = jsonObject.get("createdByUser");
        if (createdByUserJson != null)
        {
            String userId = createdByUserJson.get("id").textValue();
            String displayName = createdByUserJson.get("displayName").textValue();
            createdByUser = new  org.alfresco.rest.api.model.UserInfo(userId,displayName,displayName);   
        }
        Date createdAt = ISO8601DateFormat.parse(jsonObject.get("createdAt").textValue());

        AuditEntry auditEntry = new AuditEntry(id, auditApplicationId, createdByUser, createdAt, values);
        return auditEntry;
    }

    public static ListResponse<AuditEntry> parseAuditEntries(JsonNode jsonObject)
    {
        List<AuditEntry> entries = new ArrayList<>();

        JsonNode jsonList = jsonObject.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        for (int i = 0; i < jsonEntries.size(); i++)
        {
            JsonNode jsonEntry =  jsonEntries.get(i);
            JsonNode entry =  jsonEntry.get("entry");
            entries.add(parseAuditEntry(entry));
        }

        ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);
        ListResponse<AuditEntry> resp = new ListResponse<AuditEntry>(paging, entries);
        return resp;
    }

}
