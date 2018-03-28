/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;
import org.alfresco.util.json.JsonUtil;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;

public class Action extends org.alfresco.rest.api.model.Action implements Serializable, ExpectedComparison
{

    @Override
    public void expected(Object o)
    {
        assertTrue("o is an instance of " + o.getClass(), o instanceof Action);

        Action other = (Action) o;

        AssertUtil.assertEquals("id", getId(), other.getId());
        AssertUtil.assertEquals("actionDefinitionId", getActionDefinitionId(), other.getActionDefinitionId());
        AssertUtil.assertEquals("targetId", getTargetId(), other.getTargetId());
        AssertUtil.assertEquals("params", getParams(), other.getParams());
    }

    @SuppressWarnings("unchecked")
    public ObjectNode toJSON()
    {
        ObjectNode jsonObject = AlfrescoDefaultObjectMapper.createObjectNode();
        if (getId() != null)
        {
            jsonObject.put("id", getId());
        }

        jsonObject.put("actionDefinitionId", getActionDefinitionId());

        if (getTargetId() != null)
        {
            jsonObject.put("targetId", getTargetId());
        }

        if (getParams() != null)
        {
            jsonObject.set("params", AlfrescoDefaultObjectMapper.convertValue(getParams(), ObjectNode.class));
        }

        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    public static Action parseAction(JsonNode jsonObject) throws IOException
    {
        String id = jsonObject.get("id").textValue();
        String actionDefinitionId = jsonObject.get("actionDefinitionId").textValue();
        String targetId = jsonObject.get("targetId").textValue();
        Map<String, String> params = JsonUtil
                .convertJSONObjectToMap((ObjectNode) jsonObject.get("params"))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));

        Action action = new Action();
        action.setId(id);
        action.setActionDefinitionId(actionDefinitionId);
        action.setTargetId(targetId);
        action.setParams(params);

        return action;
    }

}
