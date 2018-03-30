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
package org.alfresco.repo.web.scripts.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.person.TestGroupManager;
import org.alfresco.repo.security.person.TestPersonManager;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.repo.workflow.WorkflowAdminServiceImpl;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.WorkflowTestHelper;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowNode;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.cmr.workflow.WorkflowTaskDefinition;
import org.alfresco.service.cmr.workflow.WorkflowTaskState;
import org.alfresco.service.cmr.workflow.WorkflowTransition;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.alfresco.util.testing.category.LuceneTests;
import org.junit.experimental.categories.Category;
import org.springframework.context.ApplicationContext;
import org.alfresco.util.ISO8601DateFormat;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;


/**
 * @author Nick Smith
 * @author Frederik Heremans
 * @since 3.4.e
 */
@Category(LuceneTests.class)
public abstract class AbstractWorkflowRestApiTest extends BaseWebScriptTest
{
    protected final static String USER1 = "Bob" + GUID.generate();
    protected final static String USER2 = "Jane" + GUID.generate();
    protected final static String USER3 = "Nick" + GUID.generate();
    protected final static String GROUP ="Group" + GUID.generate();
    protected static final String URL_TASKS = "api/task-instances";
    protected static final String URL_USER_TASKS = "api/task-instances?authority={0}";
    protected static final String URL_USER_TASKS_PROPERTIES = "api/task-instances?authority={0}&properties={1}";
    protected static final String URL_TASKS_DUE_BEFORE = "api/task-instances?dueBefore={0}";
    protected static final String URL_TASKS_DUE_AFTER = "api/task-instances?dueAfter={0}";
    protected static final String URL_TASKS_DUE_AFTER_AND_SKIP = "api/task-instances?dueAfter={0}&skipCount={1}";
    protected static final String URL_WORKFLOW_TASKS = "api/workflow-instances/{0}/task-instances";
    protected static final String URL_WORKFLOW_DEFINITIONS = "api/workflow-definitions";
    protected static final String URL_WORKFLOW_DEFINITION = "api/workflow-definitions/{0}";
    protected static final String URL_WORKFLOW_INSTANCES = "api/workflow-instances";
    protected static final String URL_WORKFLOW_INSTANCES_FOR_DEFINITION = "api/workflow-definitions/{0}/workflow-instances";
    protected static final String URL_WORKFLOW_INSTANCES_FOR_NODE = "api/node/{0}/{1}/{2}/workflow-instances";

    protected static final String COMPANY_HOME = "/app:company_home";
    protected static final String TEST_CONTENT = "TestContent";
    protected static final String ADHOC_START_TASK_TYPE = "wf:submitAdhocTask";
    protected static final String ADHOC_TASK_TYPE = "wf:adhocTask";
    protected static final String ADHOC_TASK_COMPLETED_TYPE = "wf:completedAdhocTask";


    private TestPersonManager personManager;
    private TestGroupManager groupManager;
    
    protected WorkflowService workflowService;
    private NodeService nodeService;
    private NamespaceService namespaceService;
    private NodeRef packageRef;
    private NodeRef contentNodeRef;
    private AuthenticationComponent authenticationComponent;
    private DictionaryService dictionaryService;

    private List<String> workflows = new LinkedList<String>(); 

    private WorkflowTestHelper wfTestHelper;

    public void testTaskInstancesGet() throws Exception
    {
        // Check USER2 starts with no tasks.
        personManager.setUser(USER2);
        Response response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertTrue(results.size() == 0);

        // Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Calendar dueDateCal = Calendar.getInstance();
        Date dueDate = dueDateCal.getTime();

        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String workflowId = adhocPath.getInstance().getId();
        workflows.add(workflowId);

        WorkflowTask startTask = workflowService.getStartTask(workflowId);
        workflowService.endTask(startTask.getId(), null);

        // Check USER2 now has one task.
        List<WorkflowTask> tasks = workflowService.getAssignedTasks(USER2, WorkflowTaskState.IN_PROGRESS);
        WorkflowTask task = tasks.get(0);

        Map<QName, Serializable> updateParams = new HashMap<QName, Serializable>(1);
        updateParams.put(WorkflowModel.PROP_DUE_DATE, new Date());
        workflowService.updateTask(task.getId(), updateParams, null, null);

        personManager.setUser(USER2);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertTrue(results.size() == tasks.size());
        JsonNode result = results.get(0);

        int totalItems = results.size();

        String expUrl = "api/task-instances/" + task.getId();
        assertEquals(expUrl, result.get("url").textValue());
        assertEquals(task.getName(), result.get("name").textValue());
        assertEquals(task.getTitle(), result.get("title").textValue());
        assertEquals(task.getDescription(), result.get("description").textValue());
        assertEquals(task.getState().name(), result.get("state").textValue());
        assertEquals("api/workflow-paths/" + adhocPath.getId(), result.get("path").textValue());
        assertFalse(result.get("isPooled").booleanValue());
        assertTrue(result.get("isEditable").booleanValue());
        assertTrue(result.get("isReassignable").booleanValue());
        assertFalse(result.get("isClaimable").booleanValue());
        assertFalse(result.get("isReleasable").booleanValue());

        JsonNode owner = result.get("owner");
        assertEquals(USER2, owner.get("userName").textValue());
        assertEquals(personManager.getFirstName(USER2), owner.get("firstName").textValue());
        assertEquals(personManager.getLastName(USER2), owner.get("lastName").textValue());

        JsonNode properties = result.get("properties");
        assertNotNull(properties);

        JsonNode instance = result.get("workflowInstance");
        assertNotNull(instance);

        // Check state filtering
        checkTasksState(URL_TASKS + "?state=completed", WorkflowTaskState.COMPLETED);
        checkTasksState(URL_TASKS + "?state=in_progress", WorkflowTaskState.IN_PROGRESS);

        // TODO: Add more tests to check pooled actors.

        // Check for priority filtering
        checkPriorityFiltering(URL_TASKS + "?priority=2");

        // Due after yesterday, started task should be in it
        dueDateCal.add(Calendar.DAY_OF_MONTH, -1);
        checkTasksPresent(MessageFormat.format(URL_TASKS_DUE_AFTER, ISO8601DateFormat.format(dueDateCal.getTime())),
                true, task.getId());

        // Due before yesterday, started task shouldn't be in it
        checkTasksPresent(MessageFormat.format(URL_TASKS_DUE_BEFORE, ISO8601DateFormat.format(dueDateCal.getTime())),
                false, task.getId());

        // Due before tomorrow, started task should be in it
        dueDateCal.add(Calendar.DAY_OF_MONTH, 2);
        checkTasksPresent(MessageFormat.format(URL_TASKS_DUE_BEFORE, ISO8601DateFormat.format(dueDateCal.getTime())),
                true, task.getId());

        // Due after tomorrow, started task shouldn't be in it
        checkTasksPresent(MessageFormat.format(URL_TASKS_DUE_AFTER, ISO8601DateFormat.format(dueDateCal.getTime())),
                false, task.getId());

        // checkFiltering(URL_TASKS + "?dueAfter=" +
        // ISO8601DateFormat.format(dueDate));

        // checkFiltering(URL_TASKS + "?dueBefore=" +
        // ISO8601DateFormat.format(new Date()));

        // Check property filtering on the task assigned to USER2
        String customProperties = "bpm_description,bpm_priority";
        checkTaskPropertyFiltering(customProperties, Arrays.asList("bpm_description", "bpm_priority"));

        // Properties that aren't explicitally present on task should be
        // returned as wel
        customProperties = "bpm_unexistingProperty,bpm_description,bpm_priority";
        checkTaskPropertyFiltering(customProperties,
                Arrays.asList("bpm_description", "bpm_priority", "bpm_unexistingProperty"));

        // Check paging
        int maxItems = 3;
        for (int skipCount = 0; skipCount < totalItems; skipCount += maxItems)
        {
            // one of this should test situation when skipCount + maxItems >
            // totalItems
            checkPaging(MessageFormat.format(URL_USER_TASKS, USER2) + "&maxItems=" + maxItems + "&skipCount="
                    + skipCount, totalItems, maxItems, skipCount);
        }

        // testing when skipCount > totalItems
        checkPaging(MessageFormat.format(URL_USER_TASKS, USER2) + "&maxItems=" + maxItems + "&skipCount="
                + (totalItems + 1), totalItems, maxItems, totalItems + 1);

        // check the exclude filtering
        String exclude = "wf:submitAdhocTask";
        response = sendRequest(new GetRequest(URL_TASKS + "?exclude=" + exclude), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);

        boolean adhocTasksPresent = false;
        for (int i = 0; i < results.size(); i++)
        {
            JsonNode taskJSON = results.get(i);

            String type = taskJSON.get("name").textValue();
            if (exclude.equals(type))
            {
                adhocTasksPresent = true;
                break;
            }
        }
        assertFalse("Found wf:submitAdhocTask when they were supposed to be excluded", adhocTasksPresent);
        
        // CLOUD-1928: Check skip-count works toghether with filter, start another process
        personManager.setUser(USER1);
        params.clear();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        dueDateCal.add(Calendar.DAY_OF_YEAR, 2);

        params.put(WorkflowModel.PROP_DUE_DATE, dueDateCal.getTime());
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, workflowService.createPackage(null));

        WorkflowPath adhocPath2 = workflowService.startWorkflow(adhocDef.getId(), params);
        String workflowId2 = adhocPath2.getInstance().getId();
        workflows.add(workflowId2);

        WorkflowTask startTask2 = workflowService.getStartTask(workflowId2);
        workflowService.endTask(startTask2.getId(), null);
        
        // Filter based on due-date and skip first result. Should return nothing instead of
        // the second task, since only one matches and one is skipped
        
        // Due after tomorrow, started task shouldn't be in it
       String url = MessageFormat.format(URL_TASKS_DUE_AFTER_AND_SKIP,  ISO8601DateFormat.format(dueDateCal.getTime()), 1);
       json = getDataFromRequest(url);
       ArrayNode resultArray = (ArrayNode) json.get("data");
       assertEquals(0, resultArray.size());
    }

    public void testTaskInstancesGetWithFiltering() throws Exception
    {
        // Check USER2 starts with no tasks.
        personManager.setUser(USER2);
        Response response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        getJsonArray(response, 0);

        // Start workflow as USER1 and assign the task to GROUP.
        personManager.setUser(USER1);
        WorkflowDefinition wfDefinition = workflowService.getDefinitionByName(getReviewPooledWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<>(3);
        params.put(WorkflowModel.ASSOC_GROUP_ASSIGNEE, groupManager.get(GROUP));
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_WORKFLOW_DESCRIPTION, "descTest1");

        WorkflowPath wfPath = workflowService.startWorkflow(wfDefinition.getId(), params);
        String workflowId = wfPath.getInstance().getId();
        workflows.add(workflowId);

        WorkflowTask startTask = workflowService.getStartTask(workflowId);
        workflowService.endTask(startTask.getId(), null);

        // Start another workflow as USER1 and assign the task to GROUP.
        wfDefinition = workflowService.getDefinitionByName(getReviewPooledWorkflowDefinitionName());
        params.put(WorkflowModel.ASSOC_GROUP_ASSIGNEE, groupManager.get(GROUP));
        params.put(WorkflowModel.ASSOC_PACKAGE, workflowService.createPackage(null));
        params.put(WorkflowModel.PROP_WORKFLOW_DESCRIPTION, "descTest2/withSlash");

        wfPath = workflowService.startWorkflow(wfDefinition.getId(), params);
        workflowId = wfPath.getInstance().getId();
        workflows.add(workflowId);

        startTask = workflowService.getStartTask(workflowId);
        workflowService.endTask(startTask.getId(), null);

        // Check USER2's tasks without filtering. It should return two tasks as USER2 is a member of the GROUP
        personManager.setUser(USER2);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        getJsonArray(response, 2);

        //Check USER2's tasks With filtering where property bpm:description should match "descTest1"
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=bpm:description/descTest1"), 200);
        ArrayNode results = getJsonArray(response, 1);
        JsonNode result = results.get(0);
        assertNotNull(result);
        JsonNode properties = result.get("properties");
        assertNotNull(properties);
        assertEquals("descTest1", properties.get("bpm_description").textValue());

        //Check USER2's tasks With filtering where property bpm:description should match "descTest2"
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=bpm:description/descTest2/withSlash"), 200);
        results = getJsonArray(response, 1);
        result = results.get(0);
        assertNotNull(result);
        properties = result.get("properties");
        assertNotNull(properties);
        assertEquals("descTest2/withSlash", properties.get("bpm_description").textValue());

        /*
         * -ve tests
         */
        // Mismatched property value - There is no task with the description "somePropValue"
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=bpm:description/somePropValue"), 200);
        getJsonArray(response, 0);

        //Unregistered namespace prefix (ignores "property" parameter)
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=unknownPrefix:description/test"), 200);
        getJsonArray(response, 2);

        // Nonexistent property (ignores "property" parameter)
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=bpm:nonexistentProp/test"), 200);
        getJsonArray(response, 2);

        // Not well-formed parameter
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=bpm:description/"), 200);
        getJsonArray(response, 2);

        // Not well-formed parameter
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=descTest1"), 200);
        getJsonArray(response, 2);

        // Not well-formed parameter
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2) + "&property=/descTest1"), 200);
        getJsonArray(response, 2);

        // Check USER3's tasks without filtering. It should return 0 task as USER3 is not a member of the GROUP
        personManager.setUser(USER3);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER3)), 200);
        getJsonArray(response, 0);
    }

    public void testWorkflowPermissions() throws Exception
    {
        // Start workflow as USER1 and assign task to USER1.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER1));
        Calendar dueDateCal = Calendar.getInstance();
        Date dueDate = dueDateCal.getTime();

        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String workflowId = adhocPath.getInstance().getId();
        workflows.add(workflowId);

        WorkflowTask startTask = workflowService.getStartTask(workflowId);
        workflowService.endTask(startTask.getId(), null);

        // Check tasks of USER1 from behalf of USER2
        personManager.setUser(USER2);
        Response response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER1)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertTrue("User2 should not see any tasks if he is not initiator or assignee", results.size() == 0);
    }
    
    public void testTaskInstancesForWorkflowGet() throws Exception
    {
        // Check starts with no workflow.
        personManager.setUser(USER2);
        sendRequest(new GetRequest(MessageFormat.format(URL_WORKFLOW_TASKS, "Foo")), Status.STATUS_INTERNAL_SERVER_ERROR);

        // Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Calendar dueDateCal = Calendar.getInstance();
        Date dueDate = dueDateCal.getTime();

        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String workflowId = adhocPath.getInstance().getId();
        workflows.add(workflowId);

        // End start task.
        WorkflowTask startTask = workflowService.getStartTask(workflowId);
        String startTaskId = startTask.getId();
        workflowService.endTask(startTaskId, null);

        // Check USER2 now has one task.
        List<WorkflowTask> tasks = workflowService.getAssignedTasks(USER2, WorkflowTaskState.IN_PROGRESS);
        assertEquals(1, tasks.size());
        WorkflowTask task = tasks.get(0);

        // Retrieve tasks using the workflow instance
        String baseUrl = MessageFormat.format(URL_WORKFLOW_TASKS, workflowId);

        // Check returns the completed start task.
        String adhocTaskId = task.getId();
        checkTasksMatch(baseUrl, startTaskId);

        String completedUrl = baseUrl + "?state=" + WorkflowTaskState.COMPLETED;
        checkTasksMatch(completedUrl, startTaskId);

        personManager.setUser(USER2);
        String inProgressUrl = baseUrl + "?state=" + WorkflowTaskState.IN_PROGRESS;
        checkTasksMatch(inProgressUrl, adhocTaskId);

        String user1Url = baseUrl + "?authority=" + USER1;
        checkTasksMatch(user1Url, startTaskId);

        String user2Url = baseUrl + "?authority=" + USER2;
        checkTasksMatch(user2Url, adhocTaskId);

        String user1CompletedURL = user1Url + "&state=" + WorkflowTaskState.COMPLETED;
        checkTasksMatch(user1CompletedURL, startTaskId);

        String user1InProgressURL = user1Url + "&state=" + WorkflowTaskState.IN_PROGRESS;
        checkTasksMatch(user1InProgressURL);

        String user2CompletedURL = user2Url + "&state=" + WorkflowTaskState.COMPLETED;
        checkTasksMatch(user2CompletedURL);

        String user2InProgressURL = user2Url + "&state=" + WorkflowTaskState.IN_PROGRESS;
        checkTasksMatch(user2InProgressURL, adhocTaskId);
    }

    public void testTaskInstanceGet() throws Exception
    {
        //Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        
        Calendar dueDateCal = Calendar.getInstance();
        dueDateCal.clear(Calendar.MILLISECOND);
        Date dueDate = dueDateCal.getTime();
        
        NodeRef assignee = personManager.get(USER2);
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, assignee);
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String workflowId = adhocPath.getInstance().getId();
        workflows.add(workflowId);
        
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);

        // Get the start-task
        Response response = sendRequest(new GetRequest(URL_TASKS + "/" + startTask.getId()), Status.STATUS_OK);
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        JsonNode result = json.get("data");
        assertNotNull(result);

        assertEquals(startTask.getId(), result.get("id").textValue());
        assertEquals(URL_TASKS + "/" + startTask.getId(), result.get("url").textValue());
        assertEquals(startTask.getName(), result.get("name").textValue());
        assertEquals(startTask.getTitle(), result.get("title").textValue());
        assertEquals(startTask.getDescription(), result.get("description").textValue());

        assertEquals(startTask.getState().name(), result.get("state").textValue());
        assertEquals("api/workflow-paths/" + adhocPath.getId(), result.get("path").textValue());
        
        checkWorkflowTaskEditable(result);
        checkWorkflowTaskOwner(result, USER1);
        checkWorkflowTaskPropertiesPresent(result);

        JsonNode properties = result.get("properties");
        assertEquals(1, properties.get("bpm_priority").intValue());
        String dueDateStr = ISO8601DateFormat.format(dueDate);
        assertEquals(dueDateStr, properties.get("bpm_dueDate").textValue());
        assertEquals(assignee.toString(), properties.get("bpm_assignee").textValue());
        assertEquals(packageRef.toString(), properties.get("bpm_package").textValue());
        
        checkWorkflowInstance(startTask.getPath().getInstance(), result.get("workflowInstance"));
        checkWorkflowTaskDefinition(startTask.getDefinition(), result.get("definition"));
        
        // Finish the start-task, and fetch it again
        personManager.setUser(USER2);
        workflowService.endTask(startTask.getId(), null);
        startTask = workflowService.getTaskById(startTask.getId()); 

        response = sendRequest(new GetRequest(URL_TASKS + "/" + startTask.getId()), Status.STATUS_OK);
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        result = json.get("data");
        assertNotNull(result);
        
        assertEquals(startTask.getId(), result.get("id").textValue());
        assertEquals(URL_TASKS + "/" + startTask.getId(), result.get("url").textValue());
        assertEquals(startTask.getName(), result.get("name").textValue());
        assertEquals(startTask.getTitle(), result.get("title").textValue());
        assertEquals(startTask.getDescription(), result.get("description").textValue());
        
        assertEquals(startTask.getState().name(), result.get("state").textValue());
        assertEquals("api/workflow-paths/" + adhocPath.getId(), result.get("path").textValue());

        checkWorkflowTaskReadOnly(result);
        checkWorkflowTaskOwner(result, USER1);
        checkWorkflowTaskPropertiesPresent(result);

        checkWorkflowInstance(startTask.getPath().getInstance(), result.get("workflowInstance"));
        checkWorkflowTaskDefinition(startTask.getDefinition(), result.get("definition"));
        
        // Get the next active task
        WorkflowTask firstTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        
        response = sendRequest(new GetRequest(URL_TASKS + "/" + firstTask.getId()), 200);
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        result = json.get("data");
        assertNotNull(result);
        
        assertEquals(firstTask.getId(), result.get("id").textValue());
        assertEquals(URL_TASKS + "/" + firstTask.getId(), result.get("url").textValue());
        assertEquals(firstTask.getName(), result.get("name").textValue());
        assertEquals(firstTask.getTitle(), result.get("title").textValue());
        assertEquals(firstTask.getDescription(), result.get("description").textValue());
       
        // Task should be in progress
        assertEquals(firstTask.getState().name(), result.get("state").textValue());
        assertEquals(WorkflowTaskState.IN_PROGRESS.toString(), result.get("state").textValue());
        assertEquals("api/workflow-paths/" + adhocPath.getId(), result.get("path").textValue());
        
        checkWorkflowTaskEditable(result);
        checkWorkflowTaskOwner(result, USER2);
        checkWorkflowTaskPropertiesPresent(result);
        
        checkWorkflowInstance(firstTask.getPath().getInstance(), result.get("workflowInstance"));
        checkWorkflowTaskDefinition(firstTask.getDefinition(), result.get("definition"));
        
        // Finish the task, and fetch it again
        workflowService.endTask(firstTask.getId(), null);
        firstTask = workflowService.getTaskById(firstTask.getId()); 
        
        response = sendRequest(new GetRequest(URL_TASKS + "/" + firstTask.getId()), 200);
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        result = json.get("data");
        assertNotNull(result);
        
        assertEquals(firstTask.getId(), result.get("id").textValue());
        assertEquals(URL_TASKS + "/" + firstTask.getId(), result.get("url").textValue());
        assertEquals(firstTask.getName(), result.get("name").textValue());
        assertEquals(firstTask.getTitle(), result.get("title").textValue());
        assertEquals(firstTask.getDescription(), result.get("description").textValue());
        
        // The task should be completed
        assertEquals(firstTask.getState().name(), result.get("state").textValue());
        assertEquals(WorkflowTaskState.COMPLETED.toString(), result.get("state").textValue());
        assertEquals("api/workflow-paths/" + adhocPath.getId(), result.get("path").textValue());

        checkWorkflowTaskReadOnly(result);
        checkWorkflowTaskOwner(result, USER2);
        checkWorkflowTaskPropertiesPresent(result);
        
        checkWorkflowInstance(firstTask.getPath().getInstance(), result.get("workflowInstance"));
        checkWorkflowTaskDefinition(firstTask.getDefinition(), result.get("definition"));
    }

    private void checkWorkflowTaskPropertiesPresent(JsonNode taskJson) throws Exception
    {
        JsonNode properties = taskJson.get("properties");
        assertNotNull(properties);
        assertTrue(properties.has("bpm_priority"));
        assertTrue(properties.has("bpm_description"));
        assertTrue(properties.has("bpm_reassignable"));
        JsonNode labels =taskJson.get("propertyLabels");
        assertNotNull(labels);
        assertTrue(labels.has("bpm_status"));
        
    }

    private void checkWorkflowTaskReadOnly(JsonNode taskJson) throws Exception
    {
        // Task shouldn't be editable and reassignable, since it's completed
        assertFalse(taskJson.get("isPooled").booleanValue());
        assertFalse(taskJson.get("isEditable").booleanValue());
        assertFalse(taskJson.get("isReassignable").booleanValue());
        assertFalse(taskJson.get("isClaimable").booleanValue());
        assertFalse(taskJson.get("isReleasable").booleanValue());
    }

    private void checkWorkflowTaskOwner(JsonNode taskJson, String user) throws Exception
    {
        JsonNode owner = taskJson.get("owner");
        assertEquals(user, owner.get("userName").textValue());
        assertEquals(personManager.getFirstName(user), owner.get("firstName").textValue());
        assertEquals(personManager.getLastName(user), owner.get("lastName").textValue());
    }

    private void checkWorkflowTaskEditable(JsonNode taskJson) throws Exception
    {
        assertFalse(taskJson.get("isPooled").booleanValue());
        assertTrue(taskJson.get("isEditable").booleanValue());
        assertTrue(taskJson.get("isReassignable").booleanValue());
        assertFalse(taskJson.get("isClaimable").booleanValue());
        assertFalse(taskJson.get("isReleasable").booleanValue());
    }

    private void checkWorkflowInstance(WorkflowInstance wfInstance, JsonNode instance) throws Exception
    {
        assertNotNull(instance);
        assertEquals(wfInstance.getId(), instance.get("id").textValue());
        assertTrue(instance.has("url"));
        assertEquals(wfInstance.getDefinition().getName(), instance.get("name").textValue());
        assertEquals(wfInstance.getDefinition().getTitle(), instance.get("title").textValue());
        assertEquals(wfInstance.getDefinition().getDescription(), instance.get("description").textValue());
        assertEquals(wfInstance.isActive(), instance.get("isActive").booleanValue());
        assertTrue(instance.has("startDate"));

        JsonNode initiator = instance.get("initiator");

        assertEquals(USER1, initiator.get("userName").textValue());
        assertEquals(personManager.getFirstName(USER1), initiator.get("firstName").textValue());
        assertEquals(personManager.getLastName(USER1), initiator.get("lastName").textValue());
    }

    private void checkWorkflowTaskDefinition(WorkflowTaskDefinition wfDefinition, JsonNode definition) throws Exception
    {
        assertNotNull(definition);

        assertEquals(wfDefinition.getId(), definition.get("id").textValue());
        assertTrue(definition.has("url"));

        JsonNode type = definition.get("type");
        TypeDefinition startType = (wfDefinition).getMetadata();

        assertNotNull(type);

        assertEquals(startType.getName().toPrefixString(), type.get("name").textValue());
        assertEquals(startType.getTitle(this.dictionaryService), type.get("title").textValue());
        assertEquals(startType.getDescription(this.dictionaryService), type.get("description").textValue());
        assertTrue(type.has("url"));

        JsonNode node = definition.get("node");
        WorkflowNode startNode = wfDefinition.getNode();

        assertNotNull(node);

        assertEquals(startNode.getName(), node.get("name").textValue());
        assertEquals(startNode.getTitle(), node.get("title").textValue());
        assertEquals(startNode.getDescription(), node.get("description").textValue());
        assertEquals(startNode.isTaskNode(), node.get("isTaskNode").booleanValue());

        ArrayNode transitions = (ArrayNode) node.get("transitions");
        WorkflowTransition[] startTransitions = startNode.getTransitions();

        assertNotNull(transitions);

        assertEquals(startTransitions.length, transitions.size());

        for (int i = 0; i < transitions.size(); i++)
        {
            JsonNode transition = transitions.get(i);
            WorkflowTransition startTransition = startTransitions[i];

            assertNotNull(transition);

            if (startTransition.getId() == null)
            {
                assertEquals("", transition.get("id").textValue());
            }
            else
            {
                assertEquals(startTransition.getId(), transition.get("id").textValue());
            }
            assertEquals(startTransition.getTitle(), transition.get("title").textValue());
            assertEquals(startTransition.getDescription(), transition.get("description").textValue());
            assertEquals(startTransition.isDefault(), transition.get("isDefault").booleanValue());
            assertTrue(transition.has("isHidden"));
        }

    }

    public void testTaskInstancePut() throws Exception
    {
        // Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        
        Calendar dueDate = Calendar.getInstance();
        dueDate.set(Calendar.MILLISECOND, 0);
        
        params.put(WorkflowModel.PROP_DUE_DATE, new Date());
        params.put(WorkflowModel.PROP_PRIORITY, 2);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String worfklowId = adhocPath.getInstance().getId();
        workflows.add(worfklowId);
        
        WorkflowTask startTask = workflowService.getStartTask(adhocPath.getInstance().getId());
        
        // Finish the start-task
        workflowService.endTask(startTask.getId(), null);
        
        WorkflowTask firstTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);

        Response response = sendRequest(new GetRequest(URL_TASKS + "/" + firstTask.getId()), 200);

        ObjectNode jsonProperties = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString()).get("data").get("properties");

        // make some changes in existing properties
        jsonProperties.remove(qnameToString(WorkflowModel.ASSOC_PACKAGE));
        jsonProperties.put(qnameToString(WorkflowModel.PROP_COMMENT), "Edited comment");
        
        Calendar newDueDate = Calendar.getInstance();
        newDueDate.set(Calendar.MILLISECOND, 0);
        
        jsonProperties.put(qnameToString(WorkflowModel.PROP_DUE_DATE), ISO8601DateFormat.format(newDueDate.getTime()));
        jsonProperties.put(qnameToString(WorkflowModel.PROP_DESCRIPTION), "Edited description");
        jsonProperties.put(qnameToString(WorkflowModel.PROP_PRIORITY), 1);

        // Add some custom properties, which are not defined in typeDef
        jsonProperties.set("customIntegerProperty", IntNode.valueOf(1234));
        jsonProperties.set("customBooleanProperty", BooleanNode.TRUE);
        jsonProperties.set("customStringProperty", TextNode.valueOf("Property value"));

        // test USER3 can not update the task
        personManager.setUser(USER3);
        Response unauthResponse = sendRequest(new PutRequest(URL_TASKS + "/" + firstTask.getId(), jsonProperties.toString(), "application/json"), 401);
        assertEquals(Status.STATUS_UNAUTHORIZED, unauthResponse.getStatus());


        // test USER2 (the task owner) can update the task
        personManager.setUser(USER2);
        Response putResponse = sendRequest(new PutRequest(URL_TASKS + "/" + firstTask.getId(), jsonProperties.toString(), "application/json"), 200);

        assertEquals(Status.STATUS_OK, putResponse.getStatus());
        String jsonStr = putResponse.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        JsonNode result = json.get("data");
        assertNotNull(result);

        ObjectNode editedJsonProperties = (ObjectNode) result.get("properties");
        editedJsonProperties.remove(qnameToString(ContentModel.PROP_CREATOR));
        compareProperties(jsonProperties, editedJsonProperties);
        
        // test USER1 (the task workflow initiator) can update the task
        personManager.setUser(USER1);
        putResponse = sendRequest(new PutRequest(URL_TASKS + "/" + firstTask.getId(), jsonProperties.toString(), "application/json"), 200);

        assertEquals(Status.STATUS_OK, putResponse.getStatus());
        jsonStr = putResponse.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        result = json.get("data");
        assertNotNull(result);

        editedJsonProperties = (ObjectNode) result.get("properties");
        editedJsonProperties.remove(qnameToString(ContentModel.PROP_CREATOR));
        compareProperties(jsonProperties, editedJsonProperties);

        // Reassign the task to USER3 using taskInstance PUT
        jsonProperties = AlfrescoDefaultObjectMapper.createObjectNode();
        jsonProperties.put(qnameToString(ContentModel.PROP_OWNER), USER3);
        putResponse = sendRequest(new PutRequest(URL_TASKS + "/" + firstTask.getId(), jsonProperties.toString(), "application/json"), 200);
        assertEquals(Status.STATUS_OK, putResponse.getStatus());
        
        // test USER3 (now the task owner) can update the task
        personManager.setUser(USER3);
        
        jsonProperties.put(qnameToString(WorkflowModel.PROP_COMMENT), "Edited comment by USER3");
        putResponse = sendRequest(new PutRequest(URL_TASKS + "/" + firstTask.getId(), jsonProperties.toString(), "application/json"), 200);
        
        assertEquals(Status.STATUS_OK, putResponse.getStatus());
        
        jsonStr = putResponse.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        result = json.get("data");
        assertNotNull(result);
        
        editedJsonProperties = (ObjectNode) result.get("properties");
        editedJsonProperties.remove(qnameToString(ContentModel.PROP_CREATOR));
        compareProperties(jsonProperties, editedJsonProperties);
    }

    public void testTaskInstancePutCompletedTask() throws Exception
    {
        // Start workflow as USER1 and assign to self
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER1));
        params.put(WorkflowModel.PROP_DUE_DATE, new Date());
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String WorkflowId = adhocPath.getInstance().getId();
        workflows.add(WorkflowId);
        
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        
        // Finish the start-task
        workflowService.endTask(startTask.getId(), null);
        
        WorkflowTask firstTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        
        Response getResponse = sendRequest(new GetRequest(URL_TASKS + "/" + firstTask.getId()), 200);

        ObjectNode jsonProperties = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(getResponse.getContentAsString()).get("data").get("properties");

        // Make a change
        jsonProperties.put(qnameToString(WorkflowModel.PROP_DESCRIPTION), "Edited description");

        // Update task. An error is expected, since the task is completed (and not editable)
       sendRequest(new PutRequest(URL_TASKS + "/" + startTask.getId(), jsonProperties.toString(), "application/json"), Status.STATUS_UNAUTHORIZED);
    }

    public void testWorkflowDefinitionsGet() throws Exception
    {
        personManager.setUser(USER1);
         
        Response response = sendRequest(new GetRequest(URL_WORKFLOW_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertTrue(results.size() > 0);

        boolean adhocWorkflowPresent = false;
        
        String adhocDefName = getAdhocWorkflowDefinitionName();
        for (int i = 0; i < results.size(); i++)
        {
            JsonNode workflowDefinitionJSON = results.get(i);

            assertTrue(workflowDefinitionJSON.has("id"));
            assertTrue(workflowDefinitionJSON.get("id").textValue().length() > 0);

            assertTrue(workflowDefinitionJSON.has("url"));
            String url = workflowDefinitionJSON.get("url").textValue();
            assertTrue(url.length() > 0);
            assertTrue(url.startsWith("api/workflow-definitions/"));

            assertTrue(workflowDefinitionJSON.has("name"));
            assertTrue(workflowDefinitionJSON.get("name").textValue().length() > 0);

            assertTrue(workflowDefinitionJSON.has("title"));
            String title = workflowDefinitionJSON.get("title").textValue();
            assertTrue(title.length() > 0);

            assertTrue(workflowDefinitionJSON.has("description"));
            String description = workflowDefinitionJSON.get("description").textValue();
            assertTrue(description.length() > 0);

            if(adhocDefName.equals(workflowDefinitionJSON.get("name").textValue()))
            {
                assertEquals(getAdhocWorkflowDefinitionTitle(), title);
                assertEquals(getAdhocWorkflowDefinitionDescription(), description);
                adhocWorkflowPresent = true;
            }
        }
        
        assertTrue("Adhoc workflow definition was not present!", adhocWorkflowPresent);
        
        // filter the workflow definitions and check they are not returned
        String exclude = adhocDefName;
        response = sendRequest(new GetRequest(URL_WORKFLOW_DEFINITIONS + "?exclude=" + exclude), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        json = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        
        adhocWorkflowPresent = false;
        for (int i = 0; i < results.size(); i++)
        {
            JsonNode workflowDefinitionJSON = results.get(i);
            
            String name = workflowDefinitionJSON.get("name").textValue();
            if (exclude.equals(name))
            {
                adhocWorkflowPresent = true;
                break;
            }
        }
        
        assertFalse("Found adhoc workflow when it was supposed to be excluded", adhocWorkflowPresent);
        
        // filter with a wildcard and ensure they all get filtered out
        exclude = adhocDefName;
        response = sendRequest(new GetRequest(URL_WORKFLOW_DEFINITIONS + "?exclude=" + exclude), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        json = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        
        adhocWorkflowPresent = false;
        for (int i = 0; i < results.size(); i++)
        {
            JsonNode workflowDefinitionJSON = results.get(i);
            
            String name = workflowDefinitionJSON.get("name").textValue();
            if (name.equals(adhocDefName))
            {
                adhocWorkflowPresent = true;
            }
        }
        
        assertFalse("Found adhoc workflow when it was supposed to be excluded", adhocWorkflowPresent);
    }

    public void testWorkflowDefinitionGet() throws Exception
    {
        personManager.setUser(USER1);
         
        // Get the latest definition for the adhoc-workflow
        WorkflowDefinition wDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());

        String responseUrl = MessageFormat.format(URL_WORKFLOW_DEFINITION, wDef.getId());

        Response response = sendRequest(new GetRequest(responseUrl), Status.STATUS_OK);
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());
        JsonNode workflowDefinitionJSON = json.get("data");
        assertNotNull(workflowDefinitionJSON);
        
        // Check fields
        assertTrue(workflowDefinitionJSON.has("id"));
        assertTrue(workflowDefinitionJSON.get("id").textValue().length() > 0);

        assertTrue(workflowDefinitionJSON.has("url"));
        String url = workflowDefinitionJSON.get("url").textValue();
        assertTrue(url.length() > 0);
        assertTrue(url.startsWith("api/workflow-definitions/"));

        assertTrue(workflowDefinitionJSON.has("name"));
        assertTrue(workflowDefinitionJSON.get("name").textValue().length() > 0);
        assertEquals(getAdhocWorkflowDefinitionName(), workflowDefinitionJSON.get("name").textValue());

        assertTrue(workflowDefinitionJSON.has("title"));
        assertTrue(workflowDefinitionJSON.get("title").textValue().length() > 0);

        assertTrue(workflowDefinitionJSON.has("description"));
        assertTrue(workflowDefinitionJSON.get("description").textValue().length() > 0);
        
        assertTrue(workflowDefinitionJSON.has("startTaskDefinitionUrl"));
        String startTaskDefUrl = workflowDefinitionJSON.get("startTaskDefinitionUrl").textValue();
        assertEquals(startTaskDefUrl, "api/classes/" + getSafeDefinitionName(ADHOC_START_TASK_TYPE));
        
        assertTrue(workflowDefinitionJSON.has("startTaskDefinitionType"));
        assertEquals(ADHOC_START_TASK_TYPE, workflowDefinitionJSON.get("startTaskDefinitionType").textValue());
        
        // Check task-definitions
        ArrayNode taskDefinitions = (ArrayNode) workflowDefinitionJSON.get("taskDefinitions");
        assertNotNull(taskDefinitions);
        
        // Two task definitions should be returned. Start-task is not included
        assertEquals(2, taskDefinitions.size());
        
        // Should be adhoc-task
        JsonNode firstTaskDefinition  =  taskDefinitions.get(0);
        checkTaskDefinitionTypeAndUrl(ADHOC_TASK_TYPE, firstTaskDefinition);
                
        // Should be adhoc completed task
        JsonNode secondTaskDefinition  =  taskDefinitions.get(1);
        checkTaskDefinitionTypeAndUrl(ADHOC_TASK_COMPLETED_TYPE, secondTaskDefinition);
    }
    
    private void checkTaskDefinitionTypeAndUrl(String expectedTaskType, JsonNode taskDefinition) throws Exception
    {
        // Check type
        assertTrue(taskDefinition.has("type"));
        assertEquals(expectedTaskType, taskDefinition.get("type").textValue());
        
        // Check URL
        assertTrue(taskDefinition.has("url"));
        assertEquals("api/classes/" + 
                    getSafeDefinitionName(expectedTaskType), taskDefinition.get("url").textValue());
    }

    private String getSafeDefinitionName(String definitionName) 
    {
        return definitionName.replace(":", "_");
    }

    public void testWorkflowInstanceGet() throws Exception
    {
        //Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_WORKFLOW_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_WORKFLOW_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String WorkflowId = adhocPath.getInstance().getId();
        workflows.add(WorkflowId);
        
        // End start task.
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        startTask = workflowService.endTask(startTask.getId(), null);

        WorkflowInstance adhocInstance = startTask.getPath().getInstance();

        Response response = sendRequest(new GetRequest(URL_WORKFLOW_INSTANCES + "/" + adhocInstance.getId() + "?includeTasks=true"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        JsonNode result = json.get("data");
        assertNotNull(result);

        assertEquals(adhocInstance.getId(), result.get("id").textValue());
        assertTrue(result.get("message").equals(NullNode.getInstance()));
        assertEquals(adhocInstance.getDefinition().getName(), result.get("name").textValue());
        assertEquals(adhocInstance.getDefinition().getTitle(), result.get("title").textValue());
        assertEquals(adhocInstance.getDefinition().getDescription(), result.get("description").textValue());
        assertEquals(adhocInstance.isActive(), result.get("isActive").booleanValue());
        assertEquals(org.springframework.extensions.surf.util.ISO8601DateFormat.format(adhocInstance.getStartDate()), result.get("startDate").textValue());
        assertNotNull(result.get("dueDate"));
        assertNotNull(result.get("endDate"));
        assertEquals(1, result.get("priority").intValue());
        JsonNode initiator = result.get("initiator");

        assertEquals(USER1, initiator.get("userName").textValue());
        assertEquals(personManager.getFirstName(USER1), initiator.get("firstName").textValue());
        assertEquals(personManager.getLastName(USER1), initiator.get("lastName").textValue());

        assertEquals(adhocInstance.getContext().toString(), result.get("context").textValue());
        assertEquals(adhocInstance.getWorkflowPackage().toString(), result.get("package").textValue());
        assertNotNull(result.get("startTaskInstanceId"));

        JsonNode jsonDefinition = result.get("definition");
        WorkflowDefinition adhocDefinition = adhocInstance.getDefinition();

        assertNotNull(jsonDefinition);

        assertEquals(adhocDefinition.getId(), jsonDefinition.get("id").textValue());
        assertEquals(adhocDefinition.getName(), jsonDefinition.get("name").textValue());
        assertEquals(adhocDefinition.getTitle(), jsonDefinition.get("title").textValue());
        assertEquals(adhocDefinition.getDescription(), jsonDefinition.get("description").textValue());
        assertEquals(adhocDefinition.getVersion(), jsonDefinition.get("version").textValue());
        assertEquals(adhocDefinition.getStartTaskDefinition().getMetadata().getName().toPrefixString(namespaceService), jsonDefinition.get("startTaskDefinitionType").textValue());
        assertTrue(jsonDefinition.has("taskDefinitions"));

        ArrayNode tasks = (ArrayNode) result.get("tasks");
        assertTrue(tasks.size() > 1);
    }

    public void testWorkflowInstancesGet() throws Exception
    {
        // Should work even with definitions hidden.
        wfTestHelper.setVisible(false);
        
        //Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_WORKFLOW_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_WORKFLOW_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        String WorkflowId = adhocPath.getInstance().getId();
        workflows.add(WorkflowId);
        
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        WorkflowInstance adhocInstance = startTask.getPath().getInstance();
        workflowService.endTask(startTask.getId(), null);

        // Get Workflow Instance Collection 
        Response response = sendRequest(new GetRequest(URL_WORKFLOW_INSTANCES), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);

        int totalItems = result.size();
        for (int i = 0; i < result.size(); i++)
        {
            checkSimpleWorkflowInstanceResponse(result.get(i));
        }

        Response forDefinitionResponse = sendRequest(new GetRequest(MessageFormat.format(URL_WORKFLOW_INSTANCES_FOR_DEFINITION, adhocDef.getId())), 200);
        assertEquals(Status.STATUS_OK, forDefinitionResponse.getStatus());
        String forDefinitionJsonStr = forDefinitionResponse.getContentAsString();
        JsonNode forDefinitionJson = AlfrescoDefaultObjectMapper.getReader().readTree(forDefinitionJsonStr);
        ArrayNode forDefinitionResult = (ArrayNode) forDefinitionJson.get("data");
        assertNotNull(forDefinitionResult);

        for (int i = 0; i < forDefinitionResult.size(); i++)
        {
            checkSimpleWorkflowInstanceResponse(forDefinitionResult.get(i));
        }
        
        // create a date an hour ago to test filtering
        Calendar hourAgoCal = Calendar.getInstance();
        hourAgoCal.setTime(dueDate);
        hourAgoCal.add(Calendar.HOUR_OF_DAY, -1);
        Date anHourAgo = hourAgoCal.getTime();
        
        Calendar hourLater = Calendar.getInstance();
        hourLater.setTime(dueDate);
        hourLater.add(Calendar.HOUR_OF_DAY, 1);
        Date anHourLater = hourLater.getTime();

        // filter by initiator
        checkFiltering(URL_WORKFLOW_INSTANCES + "?initiator=" + USER1);

        // filter by startedAfter
        checkFiltering(URL_WORKFLOW_INSTANCES + "?startedAfter=" + ISO8601DateFormat.format(anHourAgo));

        // filter by startedBefore
        checkFiltering(URL_WORKFLOW_INSTANCES + "?startedBefore=" + ISO8601DateFormat.format(anHourLater));

        // filter by dueAfter
        checkFiltering(URL_WORKFLOW_INSTANCES + "?dueAfter=" + ISO8601DateFormat.format(anHourAgo));

        // filter by dueBefore
        checkFiltering(URL_WORKFLOW_INSTANCES + "?dueBefore=" + ISO8601DateFormat.format(anHourLater));

        if (adhocInstance.getEndDate() != null)
        {
            // filter by completedAfter
            checkFiltering(URL_WORKFLOW_INSTANCES + "?completedAfter=" + ISO8601DateFormat.format(adhocInstance.getEndDate()));

            // filter by completedBefore
            checkFiltering(URL_WORKFLOW_INSTANCES + "?completedBefore=" + ISO8601DateFormat.format(adhocInstance.getEndDate()));
        }

        // filter by priority        
        checkFiltering(URL_WORKFLOW_INSTANCES + "?priority=1");

        // filter by state
        checkFiltering(URL_WORKFLOW_INSTANCES + "?state=active");
        
        // filter by definition name
        checkFiltering(URL_WORKFLOW_INSTANCES + "?definitionName=" + getAdhocWorkflowDefinitionName());
        
        // paging
        int maxItems = 3;        
        for (int skipCount = 0; skipCount < totalItems; skipCount += maxItems)
        {
            checkPaging(URL_WORKFLOW_INSTANCES + "?maxItems=" + maxItems + "&skipCount=" + skipCount, totalItems, maxItems, skipCount);
        }
        
        // check the exclude filtering
        String exclude = getAdhocWorkflowDefinitionName();
        response = sendRequest(new GetRequest(URL_WORKFLOW_INSTANCES + "?exclude=" + exclude), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        
        boolean adhocWorkflowPresent = false;
        for (int i = 0; i < results.size(); i++)
        {
            JsonNode workflowInstanceJSON = results.get(i);
            
            String type = workflowInstanceJSON.get("name").textValue();
            if (exclude.equals(type))
            {
                adhocWorkflowPresent = true;
                break;
            }
        }
        
        assertFalse("Found adhoc workflows when they were supposed to be excluded", adhocWorkflowPresent);
    }

    public void testWorkflowInstancesForNodeGet() throws Exception
    {
        //Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        params.put(WorkflowModel.PROP_DUE_DATE, new Date());
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);

        nodeService.addChild(packageRef, contentNodeRef, 
                WorkflowModel.ASSOC_PACKAGE_CONTAINS, QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                QName.createValidLocalName((String)nodeService.getProperty(
                        contentNodeRef, ContentModel.PROP_NAME))));

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);

        String url = MessageFormat.format(URL_WORKFLOW_INSTANCES_FOR_NODE, contentNodeRef.getStoreRef().getProtocol(), contentNodeRef.getStoreRef().getIdentifier(), contentNodeRef.getId());
        Response response = sendRequest(new GetRequest(url), 200);

        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);

        assertTrue(result.size() > 0);

        workflowService.cancelWorkflow(adhocPath.getInstance().getId());

        Response afterCancelResponse = sendRequest(new GetRequest(url), 200);

        assertEquals(Status.STATUS_OK, afterCancelResponse.getStatus());
        String afterCancelJsonStr = afterCancelResponse.getContentAsString();
        JsonNode afterCancelJson = AlfrescoDefaultObjectMapper.getReader().readTree(afterCancelJsonStr);
        ArrayNode afterCancelResult = (ArrayNode) afterCancelJson.get("data");
        assertNotNull(afterCancelResult);

        assertTrue(afterCancelResult.size() == 0);
    }
    
    public void testWorkflowInstanceDeleteAsAdministrator() throws Exception
    {
        //Start workflow as USER1 
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        startTask = workflowService.endTask(startTask.getId(), null);

        WorkflowInstance adhocInstance = startTask.getPath().getInstance();
        
        // Run next request as admin
        String admin = authenticationComponent.getDefaultAdministratorUserNames().iterator().next();
        AuthenticationUtil.setFullyAuthenticatedUser(admin);
        
        sendRequest(new DeleteRequest(URL_WORKFLOW_INSTANCES + "/" + adhocInstance.getId()), Status.STATUS_OK);

        WorkflowInstance instance = workflowService.getWorkflowById(adhocInstance.getId());
        if (instance != null)
        {
            assertFalse("The deleted workflow is still active!", instance.isActive());
        }
        
        List<WorkflowInstance> instances = workflowService.getActiveWorkflows(adhocInstance.getDefinition().getId());
        for (WorkflowInstance activeInstance : instances)
        {
            assertFalse(adhocInstance.getId().equals(activeInstance.getId()));
        }
    }
    
    public void testWorkflowInstanceDelete() throws Exception
    {
        //Start workflow as USER1 and assign task to USER2.
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(adhocPath.getId()).get(0);
        startTask = workflowService.endTask(startTask.getId(), null);

        WorkflowInstance adhocInstance = startTask.getPath().getInstance();
        
        // attempt to delete workflow as a user that is not the initiator
        personManager.setUser(USER3);
        String instanceId = adhocInstance.getId();
        sendRequest(new DeleteRequest(URL_WORKFLOW_INSTANCES + "/" + instanceId), Status.STATUS_FORBIDDEN);
        
        // make sure workflow instance is still present
        assertNotNull(workflowService.getWorkflowById(instanceId));

        // now delete as initiator of workflow
        personManager.setUser(USER1);
        sendRequest(new DeleteRequest(URL_WORKFLOW_INSTANCES + "/" + instanceId), Status.STATUS_OK);

        WorkflowInstance instance = workflowService.getWorkflowById(instanceId);
        if (instance != null)
        {
            assertFalse("The deleted workflow is still active!", instance.isActive());
        }
        
        List<WorkflowInstance> instances = workflowService.getActiveWorkflows(adhocInstance.getDefinition().getId());
        for (WorkflowInstance activeInstance : instances)
        {
            assertFalse(instanceId.equals(activeInstance.getId()));
        }
        
        // Try deleting an non-existent workflow instance, should result in 404
        sendRequest(new DeleteRequest(URL_WORKFLOW_INSTANCES + "/" + instanceId), Status.STATUS_NOT_FOUND);
    }
    
    public void testWorkflowInstanceDeleteAsRecreatedUser() throws Exception
    {
        // Create task as USER1 and assign it to another user 
        personManager.setUser(USER1);
        WorkflowDefinition adhocDef = workflowService.getDefinitionByName(getAdhocWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath adhocPath = workflowService.startWorkflow(adhocDef.getId(), params);
        
        // Check the workflow was created 
        assertNotNull(workflowService.getWorkflowById(adhocPath.getInstance().getId()));
       
        // Delete USER1
        personManager.deletePerson(USER1);
        
        // Recreate USER1
        personManager.createPerson(USER1);
        
        // Delete workflow
        personManager.setUser(USER1);
        sendRequest(new DeleteRequest(URL_WORKFLOW_INSTANCES + "/" + adhocPath.getInstance().getId()), Status.STATUS_OK);
        
        // Check the workflow was deleted
        assertNull(workflowService.getWorkflowById(adhocPath.getInstance().getId()));
    }

    public void testReviewProcessFlow() throws Exception 
    {
        // Approve path
        runReviewFlow(true);
        
        // Create package again, since WF is deleteds
        packageRef = workflowService.createPackage(null);
        
        // Reject path
        runReviewFlow(false);
    }
    
    public void testReviewPooledProcessFlow() throws Exception 
    {
        // Approve path
        runReviewPooledFlow(true);
        
        // Create package again, since WF is deleteds
        packageRef = workflowService.createPackage(null);
        
        // Reject path
        runReviewPooledFlow(false);
    }
    
    protected void runReviewFlow(boolean approve) throws Exception
    {
        // Start workflow as USER1
        personManager.setUser(USER1);
        WorkflowDefinition reviewDef = workflowService.getDefinitionByName(getReviewWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();
        // Reviewer is USER2
        params.put(WorkflowModel.ASSOC_ASSIGNEE, personManager.get(USER2));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath reviewPath = workflowService.startWorkflow(reviewDef.getId(), params);
        String workflowId = reviewPath.getInstance().getId();
        workflows.add(workflowId);
        
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(reviewPath.getId()).get(0);

        // End start task
        startTask = workflowService.endTask(startTask.getId(), null);

        // Check of task is available in list of reviewer, USER2
        personManager.setUser(USER2);
        Response response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());

        String taskId = results.get(0).get("id").textValue();

        // Delegate approval/rejection to implementing engine-test
        if (approve)
        {
            approveTask(taskId);
        }
        else
        {
            rejectTask(taskId);
        }

        // 'Approved'/'Rejected' task should be available for initiator
        personManager.setUser(USER1);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER1)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());

        // Correct task type check
        String taskType = results.get(0).get("name").textValue();
        if (approve)
        {
            assertEquals("wf:approvedTask", taskType);
        }
        else
        {
            assertEquals("wf:rejectedTask", taskType);
        }
        workflowService.cancelWorkflow(workflowId);
    }
    
    protected void runReviewPooledFlow(boolean approve) throws Exception
    {
        // Start workflow as USER1
        personManager.setUser(USER1);
        WorkflowDefinition reviewDef = workflowService.getDefinitionByName(getReviewPooledWorkflowDefinitionName());
        Map<QName, Serializable> params = new HashMap<QName, Serializable>();

        // Reviewer is group GROUP
        params.put(WorkflowModel.ASSOC_GROUP_ASSIGNEE, groupManager.get(GROUP));
        Date dueDate = new Date();
        params.put(WorkflowModel.PROP_DUE_DATE, dueDate);
        params.put(WorkflowModel.PROP_PRIORITY, 1);
        params.put(WorkflowModel.ASSOC_PACKAGE, packageRef);
        params.put(WorkflowModel.PROP_CONTEXT, packageRef);

        WorkflowPath reviewPath = workflowService.startWorkflow(reviewDef.getId(), params);
        String workflowId = reviewPath.getInstance().getId();
        workflows.add(workflowId);
        
        WorkflowTask startTask = workflowService.getTasksForWorkflowPath(reviewPath.getId()).get(0);

        // End start task
        startTask = workflowService.endTask(startTask.getId(), null);

        // Check if task is NOT available in list USER3, not a member of the
        // group
        personManager.setUser(USER3);
        Response response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER3)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(0, results.size());

        // Check if task is available in list of reviewer, member of GROUP:
        // USER2
        personManager.setUser(USER2);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());

        // Check if task is claimable and pooled
        JsonNode taskJson = results.get(0);
        String taskId = taskJson.get("id").textValue();
        assertTrue(taskJson.get("isClaimable").booleanValue());
        assertTrue(taskJson.get("isPooled").booleanValue());

        // Claim task, using PUT, updating the owner
        ObjectNode properties = AlfrescoDefaultObjectMapper.createObjectNode();
        properties.put(qnameToString(ContentModel.PROP_OWNER), USER2);
        sendRequest(new PutRequest(URL_TASKS + "/" + taskId, properties.toString(), "application/json"), 200);

        // Check if task insn't claimable anymore
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER2)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());

        taskJson = results.get(0);
        assertFalse(taskJson.get("isClaimable").booleanValue());
        assertTrue(taskJson.get("isPooled").booleanValue());

        // Delegate approval/rejection to implementing engine-test
        if (approve)
        {
            approveTask(taskId);
        }
        else
        {
            rejectTask(taskId);
        }

        // 'Approved'/'Rejected' task should be available for initiator
        personManager.setUser(USER1);
        response = sendRequest(new GetRequest(MessageFormat.format(URL_USER_TASKS, USER1)), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());

        // Correct task type check
        String taskType = results.get(0).get("name").textValue();
        if (approve)
        {
            assertEquals("wf:approvedTask", taskType);
        }
        else
        {
            assertEquals("wf:rejectedTask", taskType);
        }
        workflowService.cancelWorkflow(workflowId);
    }

    protected abstract void approveTask(String taskId) throws Exception;

    protected abstract void rejectTask(String taskId) throws Exception;

    protected abstract String getAdhocWorkflowDefinitionName();
    
    protected abstract String getAdhocWorkflowDefinitionTitle();
    
    protected abstract String getAdhocWorkflowDefinitionDescription();
    
    protected abstract String getReviewWorkflowDefinitionName();
    
    protected abstract String getReviewPooledWorkflowDefinitionName();
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        ApplicationContext appContext = getServer().getApplicationContext();

        namespaceService = (NamespaceService) appContext.getBean("NamespaceService");
        workflowService = (WorkflowService) appContext.getBean("WorkflowService");
        MutableAuthenticationService authenticationService = (MutableAuthenticationService) appContext.getBean("AuthenticationService");
        PersonService personService = (PersonService) appContext.getBean("PersonService");
        SearchService searchService = (SearchService) appContext.getBean("SearchService");
        FileFolderService fileFolderService = (FileFolderService) appContext.getBean("FileFolderService");
        nodeService = (NodeService) appContext.getBean("NodeService");
        
        // for the purposes of the tests make sure workflow engine is enabled/visible.
        WorkflowAdminServiceImpl workflowAdminService = (WorkflowAdminServiceImpl) appContext.getBean("workflowAdminService");
        this.wfTestHelper = new WorkflowTestHelper(workflowAdminService, getEngine(), true);
        
        AuthorityService authorityService = (AuthorityService) appContext.getBean("AuthorityService");
        personManager = new TestPersonManager(authenticationService, personService, nodeService);
        groupManager = new TestGroupManager(authorityService);

        authenticationComponent = (AuthenticationComponent) appContext.getBean("authenticationComponent");
        dictionaryService = (DictionaryService) appContext.getBean("dictionaryService");

        personManager.createPerson(USER1);
        personManager.createPerson(USER2);
        personManager.createPerson(USER3);

        authenticationComponent.setSystemUserAsCurrentUser();

        groupManager.addUserToGroup(GROUP, USER2);

        packageRef = workflowService.createPackage(null);

        NodeRef companyHome = searchService.selectNodes(nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE), COMPANY_HOME, null, namespaceService, false).get(0);

        contentNodeRef = fileFolderService.create(companyHome, TEST_CONTENT + System.currentTimeMillis(), ContentModel.TYPE_CONTENT).getNodeRef();

        authenticationComponent.clearCurrentSecurityContext();
    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        authenticationComponent.setSystemUserAsCurrentUser();
        for (String id: workflows)
        {
            try 
            {
                workflowService.cancelWorkflow(id);
            }
            catch (Throwable t)
            {
                // Do nothing
            }
        }
        wfTestHelper.tearDown();
        groupManager.clearGroups();
        personManager.clearPeople();
        authenticationComponent.clearCurrentSecurityContext();
    }

    private String qnameToString(QName qName)
    {
        String separator = Character.toString(QName.NAMESPACE_PREFIX);

        return qName.toPrefixString(namespaceService).replaceFirst(separator, "_");
    }

    private void compareProperties(JsonNode before, JsonNode after)
    {
        Iterator<String> iterator = after.fieldNames();
        while(iterator.hasNext())
        {
            String name = iterator.next();
            if (before.has(name))
            {
                if (before.get(name) instanceof ArrayNode)
                {
                    for (int i = 0; i < before.get(name).size(); i++)
                    {
                        assertEquals(before.get(name).get(i), after.get(name).get(i));
                    }
                }
                else
                {
                    assertEquals(before.get(name), after.get(name));
                }
            }
        }
    }

    private void checkSimpleWorkflowInstanceResponse(JsonNode json)
    {
        assertTrue(json.has("id"));
        assertTrue(json.get("id").textValue().length() > 0);

        assertTrue(json.has("url"));
        assertTrue(json.get("url").textValue().startsWith(URL_WORKFLOW_INSTANCES));

        assertTrue(json.has("name"));
        assertTrue(json.get("name").textValue().length() > 0);

        assertTrue(json.has("title"));
        assertTrue(json.get("title").textValue().length() > 0);

        assertTrue(json.has("description"));
        assertTrue(json.get("description").textValue().length() > 0);

        assertTrue(json.has("isActive"));

        assertTrue(json.has("startDate"));
        assertTrue(json.get("startDate").textValue().length() > 0);

        assertTrue(json.has("endDate"));

        assertTrue(json.has("initiator"));
        Object initiator = json.get("initiator");
        if (!initiator.equals(NullNode.getInstance()))
        {
            assertTrue(((JsonNode) initiator).has("userName"));
            assertTrue(((JsonNode) initiator).has("firstName"));
            assertTrue(((JsonNode) initiator).has("lastName"));
        }

        assertTrue(json.has("definitionUrl"));
        assertTrue(json.get("definitionUrl").textValue().startsWith(URL_WORKFLOW_DEFINITIONS));
    }

    private void checkPriorityFiltering(String url) throws Exception 
    {
        JsonNode json = getDataFromRequest(url);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);
        assertTrue(result.size() > 0);

        for (int i=0; i<result.size(); i++)
        {
            JsonNode taskObject = result.get(i);
            assertEquals(2, taskObject.get("properties").get("bpm_priority").intValue());
        }
    }
    
    private void checkTasksPresent(String url, boolean mustBePresent, String... ids) throws Exception 
    {
        List<String> taskIds = Arrays.asList(ids);
        JsonNode json = getDataFromRequest(url);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);

        ArrayList<String> resultIds = new ArrayList<String>(result.size());
        for (int i=0; i<result.size(); i++)
        {
            JsonNode taskObject = result.get(i);
            String taskId = taskObject.get("id").textValue();
            resultIds.add(taskId);
            if (mustBePresent == false && taskIds.contains(taskId))
            {
                fail("The results should not contain id: "+taskId);
            }
        }
        if (mustBePresent && resultIds.containsAll(taskIds) == false)
        {
            fail("Not all task Ids were present!\nExpected: "+taskIds +"\nActual: "+resultIds); 
        }
    }
    
    private void checkTasksMatch(String url, String... ids) throws Exception 
    {
        List<String> taskIds = Arrays.asList(ids);
        JsonNode json = getDataFromRequest(url);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);
        
        ArrayList<String> resultIds = new ArrayList<String>(result.size());
        for (int i=0; i<result.size(); i++)
        {
            JsonNode taskObject = result.get(i);
            String taskId = taskObject.get("id").textValue();
            resultIds.add(taskId);
        }
        assertTrue("Expected: "+taskIds +"\nActual: "+resultIds, resultIds.containsAll(taskIds));
        assertTrue("Expected: "+taskIds +"\nActual: "+resultIds, taskIds.containsAll(resultIds));
    }
    
    private void checkTasksState(String url, WorkflowTaskState expectedState) throws Exception 
    {
        JsonNode json = getDataFromRequest(url);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);

        for (int i=0; i<result.size(); i++)
        {
            JsonNode taskObject = result.get(i);
            String state = taskObject.get("state").textValue();
            assertEquals(expectedState.toString(), state);
        }
    }

    private JsonNode getDataFromRequest(String url) throws Exception
    {
        Response response = sendRequest(new GetRequest(url), Status.STATUS_OK);
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        return json;
    }
    
    private void checkTaskPropertyFiltering(String propertiesParamValue, List<String> expectedProperties) throws Exception
    {
        JsonNode data = getDataFromRequest(MessageFormat.format(URL_USER_TASKS_PROPERTIES, USER2, propertiesParamValue));
        ArrayNode taskArray = (ArrayNode) data.get("data");
        assertEquals(1, taskArray.size());

        JsonNode taskProperties = taskArray.get(0).get("properties");
        assertNotNull(taskProperties);

        int expectedNumberOfProperties = 0;
        if(expectedProperties != null)
        {
            expectedNumberOfProperties = expectedProperties.size();
        }
        // Check right number of properties
        assertEquals(expectedNumberOfProperties, taskProperties.size());

        // Check if all properties are present
        if (expectedProperties != null)
        {
            for (String prop : expectedProperties)
            {
                assertTrue(taskProperties.has(prop));
            }
        }
    }
    
    private void checkFiltering(String url) throws Exception
    {
        JsonNode json = getDataFromRequest(url);
        ArrayNode result = (ArrayNode) json.get("data");
        assertNotNull(result);

        assertTrue(result.size() > 0);
    }
    
    private void checkPaging(String url, int totalItems, int maxItems, int skipCount) throws Exception
    {
        Response response = sendRequest(new GetRequest(url), 200);

        assertEquals(Status.STATUS_OK, response.getStatus());
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        
        ArrayNode data = (ArrayNode) json.get("data");
        JsonNode paging = json.get("paging");
        
        assertNotNull(data);
        assertNotNull(paging);

        assertTrue(data.size() >= 0);
        assertTrue(data.size() <= maxItems);
        
        assertEquals(totalItems, paging.get("totalItems").intValue());
        assertEquals(maxItems, paging.get("maxItems").intValue());
        assertEquals(skipCount, paging.get("skipCount").intValue());
    }

    private ArrayNode getJsonArray(Response response, int expectedLength) throws Exception
    {
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(expectedLength, results.size());
        return results;
    }

    protected abstract String getEngine();
    
}
