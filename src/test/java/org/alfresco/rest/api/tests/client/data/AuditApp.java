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
import java.util.List;

import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

/**
 * A representation of a Audit App in JUnit Test
 * 
 * @author Andreea Nechifor
 *
 */
public class AuditApp extends org.alfresco.rest.api.model.AuditApp implements Serializable, ExpectedComparison
{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void expected(Object o)
    {
        assertTrue("o is an instance of " + o.getClass(), o instanceof AuditApp);

        AuditApp other = (AuditApp) o;

        AssertUtil.assertEquals("id", getId(), other.getId());
        AssertUtil.assertEquals("name", getName(), other.getName());
        AssertUtil.assertEquals("isEnabled", getIsEnabled(), other.getIsEnabled());
    }

    public ObjectNode toJSON()
    {
        ObjectNode auditAppJson = AlfrescoDefaultObjectMapper.createObjectNode();
        if (getId() != null)
        {
            auditAppJson.put("id", getId());
        }

        auditAppJson.put("name", getName());
        auditAppJson.put("isEnabled", getIsEnabled());

        return auditAppJson;
    }

    public static AuditApp parseAuditApp(JsonNode jsonObject)
    {
        String id = jsonObject.get("id").textValue();
        String name = jsonObject.get("name").textValue();
        Boolean isEnabled = jsonObject.get("isEnabled").booleanValue();

        AuditApp auditApp = new AuditApp();
        auditApp.setId(id);
        auditApp.setName(name);
        auditApp.setIsEnabled(isEnabled);

        return auditApp;
    }

    public static ListResponse<AuditApp> parseAuditApps(JsonNode jsonObject)
    {
        List<AuditApp> groups = new ArrayList<>();

        JsonNode jsonList = jsonObject.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        for (int i = 0; i < jsonEntries.size(); i++)
        {
            JsonNode jsonEntry = jsonEntries.get(i);
            JsonNode entry = jsonEntry.get("entry");
            groups.add(parseAuditApp(entry));
        }

        ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);
        ListResponse<AuditApp> resp = new ListResponse<AuditApp>(paging, groups);
        return resp;
    }

}
