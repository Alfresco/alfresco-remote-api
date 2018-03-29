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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.api.tests.client.HttpResponse;
import org.alfresco.rest.api.tests.client.PublicApiClient;
import org.alfresco.rest.api.tests.client.PublicApiException;
import org.alfresco.rest.api.tests.client.PublicApiHttpClient;
import org.alfresco.rest.api.tests.client.UserDataService;
import org.alfresco.rest.workflow.api.model.Deployment;
import org.alfresco.rest.workflow.api.model.ProcessDefinition;
import org.alfresco.rest.workflow.api.model.ProcessInfo;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.lang.StringUtils;

public class WorkflowApiClient extends PublicApiClient
{
    public static final DateFormat DATE_FORMAT_ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    private DeploymentsClient deploymentsClient;
    private ProcessDefinitionsClient processDefinitionsClient;
    private ProcessesClient processesClient;
    private TasksClient tasksClient;
    
    public WorkflowApiClient(PublicApiHttpClient client, UserDataService userDataService) {
        super(client, userDataService);
        initWorkflowClients();
    }
    
    public void initWorkflowClients()
    {
        deploymentsClient = new DeploymentsClient();
        processDefinitionsClient = new ProcessDefinitionsClient();
        processesClient = new ProcessesClient();
        tasksClient = new TasksClient();
    }

    public DeploymentsClient deploymentsClient()
    {
        return deploymentsClient;
    }
    

    public ProcessDefinitionsClient processDefinitionsClient()
    {
        return processDefinitionsClient;
    }
    
    public ProcessesClient processesClient()
    {
        return processesClient;
    }
    
    public TasksClient tasksClient()
    {
        return tasksClient;
    }
    
    public class DeploymentsClient extends AbstractProxy
    {
        public ListResponse<Deployment> getDeployments(Map<String, String> params) throws PublicApiException, IOException
        {
            HttpResponse response = getAll("deployments", null, null, null, params, "Failed to get deploymentsClient");
            return DeploymentParser.INSTANCE.parseList(response.getJsonResponse());
        }
        
        public JsonNode getDeploymentsWithRawResponse(Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("deployments", null, null, null, params, "Failed to get deploymentsClient");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }

        public ListResponse<Deployment> getDeployments() throws PublicApiException, IOException
        {
            return getDeployments(null);
        }

        /*public ListResponse<Deployment> createNewDeployment(String name, byte[] bytes) throws PublicApiException
        {
            Part[] parts = new Part[]{
                    new StringPart("name", name),
                    new FilePart("file", new ByteArrayPartSource("bytes", bytes))
            };
            RequestEntity requestEntity = new MultipartRequestEntity(parts, null);
            HttpResponse response = create("deployments", null, null, null, requestEntity, "Failed to create new deployment");
            return DeploymentParser.INSTANCE.parseList(response.getJsonResponse());
        }*/

        public Deployment findDeploymentById(String deploymentId) throws PublicApiException
        {
            HttpResponse response = getSingle("deployments", deploymentId, null, null, "Failed to get deployment");
            JsonNode entry = response.getJsonResponse().get("entry");
            return DeploymentParser.INSTANCE.parseEntry(entry);
        }

        public void deleteDeployment(String deploymentId) throws PublicApiException
        {
            remove("deployments", deploymentId, null, null, "Failed to delete deployment");
        }
    }
    
    public class ProcessDefinitionsClient extends AbstractProxy
    {
        public ListResponse<ProcessDefinition> getProcessDefinitions(Map<String, String> params) throws PublicApiException, IOException
        {
            HttpResponse response = getAll("process-definitions", null, null, null, params, "Failed to get process definitions");
            return ProcessDefinitionParser.INSTANCE.parseList(response.getJsonResponse());
        }
        
        public JsonNode getProcessDefinitionsWithRawResponse(Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("process-definitions", null, null, null, params, "Failed to get process definitions");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }

        public ProcessDefinition findProcessDefinitionById(String processDefinitionId) throws PublicApiException
        {
            HttpResponse response = getSingle("process-definitions", processDefinitionId, null, null, "Failed to get process definition");
            JsonNode entry = response.getJsonResponse().get("entry");
            return ProcessDefinitionParser.INSTANCE.parseEntry(entry);
        }

        public HttpResponse findImageById(String processDefinitionId) throws PublicApiException
        {
            return getSingle("process-definitions", processDefinitionId, "image", null, "Failed to get process definition");
        }

        public JsonNode findStartFormModel(String processDefinitionId) throws PublicApiException
        {
            HttpResponse response = getAll("process-definitions", processDefinitionId, "start-form-model", null, null,
                    "Failed to get the start form model of the process definition");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
    }

    public class ProcessesClient extends AbstractProxy
    {
        public ProcessInfo createProcess(String body) throws PublicApiException, IOException
        {
            HttpResponse response = create("processes", null, null, null, body, "Failed to start new process instance");
            return ProcessesParser.INSTANCE.parseEntry(response.getJsonResponse().get("entry"));
        }

        public ListResponse<ProcessInfo> getProcesses(Map<String, String> params) throws PublicApiException, IOException
        {
            HttpResponse response = getAll("processes", null, null, null, params, "Failed to get process instances");
            return ProcessesParser.INSTANCE.parseList(response.getJsonResponse());
        }
        
        public JsonNode getProcessesJSON(Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("processes", null, null, null, params, "Failed to get process instances");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }

        public ProcessInfo findProcessById(String processInstanceId) throws PublicApiException, IOException
        {
            HttpResponse response = getSingle("processes", processInstanceId, null, null, "Failed to find process instance by id");
            JsonNode entry = response.getJsonResponse().get("entry");
            return ProcessesParser.INSTANCE.parseEntry(entry);
        }
        
        public JsonNode getTasks(String processInstanceId, Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("processes", processInstanceId, "tasks", null, params, "Failed to get task instances of processInstanceId " + processInstanceId);
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode getActivities(String processInstanceId, Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("processes", processInstanceId, "activities", null, params, "Failed to get activity instances of processInstanceId " + processInstanceId);
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public HttpResponse getImage(String processInstanceId) throws PublicApiException
        {
            HttpResponse response = getSingle("processes", processInstanceId, "image", null, "Failed to get image of processInstanceId " + processInstanceId);
            return response;
        }
        
        public JsonNode findProcessItems(String processInstanceId) throws PublicApiException
        {
            HttpResponse response = getAll("processes", processInstanceId, "items", null, null,
                    "Failed to get the items of the process instance");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode getProcessvariables(String processInstanceId) throws PublicApiException
        {
            HttpResponse response = getAll("processes", processInstanceId, "variables", null, null,
                    "Failed to get the variables of the process instance");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode createVariables(String processId, ArrayNode variables) throws PublicApiException, JsonProcessingException
        {
            HttpResponse response = create("processes", processId, "variables",
                    null, AlfrescoDefaultObjectMapper.writeValueAsString(variables), "Failed to create variables");
            return response.getJsonResponse();
        }
        
        public JsonNode updateVariable(String processId, String variableName, JsonNode variable) throws PublicApiException, JsonProcessingException
        {
            HttpResponse response = update("processes", processId, "variables",
                    variableName, AlfrescoDefaultObjectMapper.writeValueAsString(variable), "Failed to update variable");
            return response.getJsonResponse();
        }
        
        public void deleteVariable(String processId, String variableName) throws PublicApiException
        {
            remove("processes", processId, "variables", variableName, "Failed to delete variable");
        }
        
        public void addProcessItem(String processId, String body) throws PublicApiException
        {
            create("processes", processId, "items", null, body, "Failed to add item");
        }
        
        public void deleteProcessItem(String processId, String itemId) throws PublicApiException
        {
            remove("processes", processId, "items", itemId, "Failed to delete item");
        }
        
        public JsonNode findProcessItem(String processInstanceId, String itemId) throws PublicApiException
        {
            HttpResponse response = getAll("processes", processInstanceId, "items", itemId, null,
                    "Failed to get the item of the process instance");
            JsonNode entry = response.getJsonResponse().get("entry");
            return entry;
        }

        public void deleteProcessById(String processId) throws PublicApiException
        {
            remove("processes", processId, null, null, "Failed to delete process");
        }
    }
    
    public class TasksClient extends AbstractProxy
    {
        public JsonNode findTaskById(String taskId) throws PublicApiException
        {
            HttpResponse response = getSingle("tasks", taskId, null, null, "Failed to get task");
            JsonNode entry = response.getJsonResponse().get("entry");
            return entry;
        }
        
        public JsonNode updateTask(String taskId, JsonNode task, List<String> selectedFields) throws PublicApiException, JsonProcessingException
        {
            String selectedFieldsValue = StringUtils.join(selectedFields, ",");
            Map<String, String> params = new HashMap<String, String>();
            params.put("select", selectedFieldsValue);
            
            HttpResponse response = update("tasks", taskId, null, null,
                    AlfrescoDefaultObjectMapper.writeValueAsString(task), params, "Failed to update task", 200);
            
            JsonNode entry = response.getJsonResponse().get("entry");
            return entry;
        }
        
        public JsonNode findTasks(Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", null, null, null, params, "Failed to get all tasks");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode findTaskCandidates(String taskId) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", taskId, "candidates", null, null, "Failed to get task candidates");
            return response.getJsonResponse();
        }
        
        public JsonNode findTaskVariables(String taskId, Map<String, String> params) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", taskId, "variables", null, params, "Failed to get task variables");
            return response.getJsonResponse();
        }
        
        public JsonNode findTaskVariables(String taskId) throws PublicApiException
        {
            return findTaskVariables(taskId, null);
        }
        
        public JsonNode createTaskVariables(String taskId, ArrayNode variables) throws PublicApiException, JsonProcessingException
        {
            HttpResponse response = create("tasks", taskId, "variables", null,
                    AlfrescoDefaultObjectMapper.writeValueAsString(variables), "Failed to create task variables");
            return response.getJsonResponse();
        }
        
        public JsonNode updateTaskVariable(String taskId, String variableName, JsonNode variable) throws PublicApiException, JsonProcessingException
        {
            HttpResponse response = update("tasks", taskId, "variables", variableName,
                    AlfrescoDefaultObjectMapper.writeValueAsString(variable), "Failed to update task variable");
            return response.getJsonResponse();
        }
        
        public void deleteTaskVariable(String taskId, String variableName) throws PublicApiException
        {
            remove("tasks", taskId, "variables", variableName, "Failed to delete task variable");
        }

        public JsonNode findTaskFormModel(String taskId) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", taskId, "task-form-model", null, null, "Failed to get task form model");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode findTaskItems(String taskId) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", taskId, "items", null, null,
                    "Failed to get the items of the task");
            JsonNode list = response.getJsonResponse().get("list");
            return list;
        }
        
        public JsonNode addTaskItem(String taskId, String body) throws PublicApiException
        {
            HttpResponse response = create("tasks", taskId, "items", null, body, "Failed to add item");
            JsonNode entry = response.getJsonResponse().get("entry");
            return entry;
        }
        
        public void deleteTaskItem(String taskId, String itemId) throws PublicApiException
        {
            remove("tasks", taskId, "items", itemId, "Failed to delete item");
        }
        
        public JsonNode findTaskItem(String taskId, String itemId) throws PublicApiException
        {
            HttpResponse response = getAll("tasks", taskId, "items", itemId, null,
                    "Failed to get the item of the task");
            JsonNode entry = response.getJsonResponse().get("entry");
            return entry;
        }
    }
    
    public static Date parseDate(JsonNode entry, String fieldName) {
        JsonNode dateJson = entry.get(fieldName);
        if (dateJson!=null) {
          try
          {
              return DATE_FORMAT_ISO8601.parse(dateJson.textValue());
          }
          catch (Exception e)
          {
              throw new RuntimeException("couldn't parse date "+dateJson.textValue()+": "+e.getMessage(), e);
          }
        }
        return null;
      }
}
