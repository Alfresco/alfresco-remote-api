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
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import java.util.stream.Collectors;
import org.alfresco.rest.api.tests.client.PublicApiClient.ExpectedPaging;
import org.alfresco.rest.api.tests.client.PublicApiClient.ListResponse;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

/**
 * Represents a group.
 *
 * @author cturlica
 *
 */
public class Group extends org.alfresco.rest.api.model.Group implements Serializable, ExpectedComparison
{

    private static final long serialVersionUID = -3580248429177260831L;

    @Override
    public void expected(Object o)
    {
        assertTrue("o is an instance of " + o.getClass(), o instanceof Group);

        Group other = (Group) o;

        AssertUtil.assertEquals("id", getId(), other.getId());
        AssertUtil.assertEquals("displayName", getDisplayName(), other.getDisplayName());
        AssertUtil.assertEquals("isRoot", getIsRoot(), other.getIsRoot());
        AssertUtil.assertEquals("parentIds", getParentIds(), other.getParentIds());
        AssertUtil.assertEquals("zones", getZones(), other.getZones());
    }

    public ObjectNode toJSON()
    {
        ObjectNode groupJson = AlfrescoDefaultObjectMapper.createObjectNode();
        if (getId() != null)
        {
            groupJson.put("id", getId());
        }

        groupJson.put("displayName", getDisplayName());

        if (getIsRoot() != null)
        {
            groupJson.put("isRoot", getIsRoot());
        }

        if (getParentIds() != null)
        {
            groupJson.set("parentIds",
                    AlfrescoDefaultObjectMapper.convertValue(new ArrayList(getParentIds()), ArrayNode.class));
        }

        if (getZones() != null)
        {
            groupJson.set("zones",
                    AlfrescoDefaultObjectMapper.convertValue(new ArrayList(getZones()), ArrayNode.class));
        }

        return groupJson;
    }

    public static Group parseGroup(JsonNode jsonObject) throws IOException
    {
        String id = jsonObject.get("id").textValue();
        String displayName =  jsonObject.get("displayName").textValue();
        Boolean isRoot =  jsonObject.get("isRoot").booleanValue();
        List<String> parentIds = JsonUtil
                .convertJSONArrayToList((ArrayNode) jsonObject.get("parentIds"))
                .stream().map(parentId -> ((String) parentId))
                .collect(Collectors.toList());
        List<String> zones = JsonUtil
                .convertJSONArrayToList((ArrayNode) jsonObject.get("zones"))
                .stream().map(zone -> ((String) zone))
                .collect(Collectors.toList());

        Group group = new Group();
        group.setId(id);
        group.setDisplayName(displayName);
        group.setIsRoot(isRoot);
        group.setParentIds(parentIds != null ? new HashSet<String>(parentIds) : null);
        group.setZones(zones != null ? new HashSet<String>(zones) : null);

        return group;
    }

    public static ListResponse<Group> parseGroups(JsonNode jsonObject) throws IOException
    {
        List<Group> groups = new ArrayList<>();

        JsonNode jsonList = jsonObject.get("list");
        assertNotNull(jsonList);

        ArrayNode jsonEntries = (ArrayNode) jsonList.get("entries");
        assertNotNull(jsonEntries);

        for (int i = 0; i < jsonEntries.size(); i++)
        {
            JsonNode jsonEntry = jsonEntries.get(i);
            JsonNode entry = jsonEntry.get("entry");
            groups.add(parseGroup(entry));
        }

        ExpectedPaging paging = ExpectedPaging.parsePagination(jsonList);
        ListResponse<Group> resp = new ListResponse<>(paging, groups);
        return resp;
    }

}
