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

import com.fasterxml.jackson.databind.JsonNode;
import org.alfresco.rest.workflow.api.model.ProcessDefinition;

public class ProcessDefinitionParser extends ListParser<ProcessDefinition>
{
    public static ProcessDefinitionParser INSTANCE = new ProcessDefinitionParser();

    @Override
    public ProcessDefinition parseEntry(JsonNode entry)
    {
        ProcessDefinition processDefinition = new ProcessDefinition();
        processDefinition.setId(entry.get("id").textValue());
        processDefinition.setKey(entry.get("key").textValue());
        processDefinition.setVersion(entry.get("version").intValue());
        processDefinition.setName(entry.get("name").textValue());
        processDefinition.setDeploymentId(entry.get("deploymentId").textValue());
        processDefinition.setTitle(entry.get("title").textValue());
        processDefinition.setDescription(entry.get("description").textValue());
        processDefinition.setCategory(entry.get("category").textValue());
        processDefinition.setStartFormResourceKey(entry.get("startFormResourceKey").textValue());
        processDefinition.setGraphicNotationDefined(entry.get("graphicNotationDefined").booleanValue());
        return processDefinition;
    }
}
