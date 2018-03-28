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
        processesRest.setDurationInMs(entry.get("durationInMs").longValue());
        processesRest.setDeleteReason(entry.get("deleteReason").textValue());
        processesRest.setBusinessKey(entry.get("businessKey").textValue());
        processesRest.setSuperProcessInstanceId(entry.get("superProcessInstanceId").textValue());
        processesRest.setStartActivityId(entry.get("startActivityId").textValue());
        processesRest.setStartUserId(entry.get("startUserId").textValue());
        processesRest.setEndActivityId(entry.get("endActivityId").textValue());
        processesRest.setCompleted(entry.get("completed").booleanValue());
        processesRest.setVariables(JsonUtil.convertJSONObjectToMap((ObjectNode) entry.get("variables")));
        processesRest.setItems(JsonUtil
                .convertJSONArrayToList((ArrayNode) entry.get("item"))
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
                variable.setValue(variableJSON.get("value"));
                processVariables.add(variable);
            }
            processesRest.setProcessVariables(processVariables);
        }
        
        return processesRest;
    }
}
