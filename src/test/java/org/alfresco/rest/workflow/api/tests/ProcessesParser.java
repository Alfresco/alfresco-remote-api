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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;
import org.alfresco.rest.workflow.api.model.ProcessInfo;
import org.alfresco.rest.workflow.api.model.Variable;
import org.alfresco.util.json.JsonUtil;

public class ProcessesParser extends ListParser<ProcessInfo>
{
    public static ProcessesParser INSTANCE = new ProcessesParser();

    @SuppressWarnings("unchecked")
    @Override
    public ProcessInfo parseEntry(JsonNode entry) throws IOException
    {
        ProcessInfo processesRest = new ProcessInfo();
        processesRest.setId(entry.get("id").textValue());
        processesRest.setProcessDefinitionId(entry.get("processDefinitionId").textValue());
        processesRest.setProcessDefinitionKey(entry.get("processDefinitionKey").textValue());
        processesRest.setStartedAt(WorkflowApiClient.parseDate(entry, "startedAt"));
        processesRest.setEndedAt(WorkflowApiClient.parseDate(entry, "endedAt"));
        JsonNode duration = entry.get("durationInMs");
        processesRest.setDurationInMs(duration == null ? null : duration.longValue());
        processesRest.setDeleteReason(entry.path("deleteReason").textValue());
        processesRest.setBusinessKey(entry.path("businessKey").textValue());
        processesRest.setSuperProcessInstanceId(entry.path("superProcessInstanceId").textValue());
        processesRest.setStartActivityId(entry.path("startActivityId").textValue());
        processesRest.setStartUserId(entry.path("startUserId").textValue());
        processesRest.setEndActivityId(entry.path("endActivityId").textValue());
        processesRest.setCompleted(entry.path("completed").booleanValue());
        JsonNode variablesJson = entry.get("variables");
        processesRest.setVariables(variablesJson == null ? null : JsonUtil.convertJSONObjectToMap((ObjectNode) variablesJson));
        JsonNode itemsJson = entry.get("item");
        processesRest.setItems(itemsJson == null ? null :
                JsonUtil.convertJSONArrayToList((ArrayNode) itemsJson)
                        .stream().map(item -> ((String) item))
                        .collect(Collectors.toSet()));
        
        if (entry.get("processVariables") != null) {
            List<Variable> processVariables = new ArrayList<>();
            ArrayNode variables = (ArrayNode) entry.get("processVariables");
            for (int i = 0; i < variables.size(); i++) 
            {
                JsonNode variableJSON = variables.get(i);
                Variable variable = new Variable();
                variable.setName( variableJSON.get("name").textValue());
                variable.setType( variableJSON.get("type").textValue());
                JsonNode valueJson = variableJSON.get("value");
                Object value = null;
                if (valueJson instanceof ObjectNode)
                {
                    value = JsonUtil.convertJSONObjectToMap((ObjectNode) valueJson);
                }
                else if (valueJson instanceof ArrayNode)
                {
                    value = JsonUtil.convertJSONArrayToList((ArrayNode) valueJson);
                }
                else if (valueJson instanceof ValueNode)
                {
                    value = JsonUtil.convertJSONValue((ValueNode) valueJson);
                }
                variable.setValue(value);
                processVariables.add(variable);
            }
            processesRest.setProcessVariables(processVariables);
        }
        
        return processesRest;
    }
}
