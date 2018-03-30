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
package org.alfresco.repo.web.scripts.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.person.TestPersonManager;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.action.ActionStatus;
import org.alfresco.service.cmr.action.ActionTrackingService;
import org.alfresco.service.cmr.replication.ReplicationDefinition;
import org.alfresco.service.cmr.replication.ReplicationService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.ISO8601DateFormat;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Tests for the Replication Webscripts
 * @author Nick Burch
 * 
 * TODO - Scheduling parts
 */
public class ReplicationRestApiTest extends BaseWebScriptTest
{
    private static final String URL_REPLICATION_SERVICE_STATUS = "/api/replication-service-status";
    private static final String URL_DEFINITION = "api/replication-definition/";
    private static final String URL_DEFINITIONS = "api/replication-definitions";
    private static final String URL_RUNNING_ACTION = "api/running-action/";
    
    private static final String JSON = "application/json";
    
    private static final String USER_NORMAL = "Normal" + GUID.generate();
    
    private NodeService nodeService;
    private TestPersonManager personManager;
    private ReplicationService replicationService;
    private TransactionService transactionService;
    private ActionTrackingService actionTrackingService;
    
    private Repository repositoryHelper;
    private NodeRef dataDictionary;
    
    /**
     * @since 3.5
     */
    public void testReplicationServiceIsEnabled() throws Exception
    {
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        
        Response response = sendRequest(new GetRequest(URL_REPLICATION_SERVICE_STATUS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        JsonNode data = json.get("data");
        assertNotNull(data);
        assertTrue("ReplicationService was unexpectedly disabled.", data.get(ReplicationServiceStatusGet.ENABLED).booleanValue());
    }
    
    public void testReplicationDefinitionsGet() throws Exception
    {
        Response response;
        
        
        // Not allowed if you're not an admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getGuestUserName());
        response = sendRequest(new GetRequest(URL_DEFINITIONS), Status.STATUS_UNAUTHORIZED);
        assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
        
        AuthenticationUtil.setFullyAuthenticatedUser(USER_NORMAL);
        response = sendRequest(new GetRequest(URL_DEFINITIONS), Status.STATUS_UNAUTHORIZED);
        assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
        
       
        // If no definitions exist, you don't get anything back
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        ArrayNode results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(0, results.size());

        
        // Add a definition, it should show up
        ReplicationDefinition rd = replicationService.createReplicationDefinition("Test1", "Testing");
        replicationService.saveReplicationDefinition(rd);
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());
        
        JsonNode jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        
        // Ensure we didn't get any unexpected data back,
        //  only the keys we should have done
        Iterator<String> iterator = jsonRD.fieldNames();
        while(iterator.hasNext())
        {
           String key = iterator.next();
           if(key.equals("name") || key.equals("status") ||
               key.equals("startedAt") || key.equals("enabled") ||
               key.equals("details")) {
              // All good
           } else {
              fail("Unexpected key '"+key+"' found in json, raw json is\n" + jsonStr);
           }
        }
        
        
        // Mark it as pending execution, and re-check
        actionTrackingService.recordActionPending(rd);
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());
        
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Pending", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        
        // Change the status to running, and re-check
        actionTrackingService.recordActionExecuting(rd);
        String startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(1, results.size());
        
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
       
        // Add a 2nd and 3rd
        rd = replicationService.createReplicationDefinition("Test2", "2nd Testing");
        replicationService.saveReplicationDefinition(rd);
        rd = replicationService.createReplicationDefinition("AnotherTest", "3rd Testing");
        replicationService.saveReplicationDefinition(rd);
        
        // They should come back sorted by name
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("AnotherTest", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/AnotherTest", jsonRD.get("details").textValue());
        
        jsonRD = results.get(1);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        jsonRD = results.get(2);
        assertNotNull(jsonRD);
        assertEquals("Test2", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/Test2", jsonRD.get("details").textValue());
        
        
        // Sort by status
        response = sendRequest(new GetRequest(URL_DEFINITIONS + "?sort=status"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // New, name sorts higher 
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("AnotherTest", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/AnotherTest", jsonRD.get("details").textValue());
        
        // New, name sorts lower
        jsonRD = results.get(1);
        assertNotNull(jsonRD);
        assertEquals("Test2", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/Test2", jsonRD.get("details").textValue());
        
        // Running
        jsonRD = results.get(2);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
                
        
        // Set start times and statuses on these other two
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        rd = replicationService.loadReplicationDefinition("Test2");
        actionTrackingService.recordActionExecuting(rd);
        actionTrackingService.recordActionComplete(rd);
        String startedAt2 = ISO8601DateFormat.format(rd.getExecutionStartDate());
        txn.commit();
        
        
        // Try the different sorts
        response = sendRequest(new GetRequest(URL_DEFINITIONS + "?sort=status"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        // Complete
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("Test2", jsonRD.get("name").textValue());
        assertEquals("Completed", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt2, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test2", jsonRD.get("details").textValue());
        
        // New
        jsonRD = results.get(1);
        assertNotNull(jsonRD);
        assertEquals("AnotherTest", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/AnotherTest", jsonRD.get("details").textValue());
        
        // Running
        jsonRD = results.get(2);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        
        // By last run
        response = sendRequest(new GetRequest(URL_DEFINITIONS + "?sort=lastRun"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(3, results.size());

        // Ran most recently
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("Test2", jsonRD.get("name").textValue());
        assertEquals("Completed", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt2, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test2", jsonRD.get("details").textValue());
        
        // Ran least recently
        jsonRD = results.get(1);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        // Never run last
        jsonRD = results.get(2);
        assertNotNull(jsonRD);
        assertEquals("AnotherTest", jsonRD.get("name").textValue());
        assertEquals("New", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), jsonRD.get("startedAt"));
        assertEquals("/api/replication-definition/AnotherTest", jsonRD.get("details").textValue());
        
       
        // Cancel one of these
        rd = replicationService.loadReplicationDefinition("AnotherTest");
        rd.setEnabled(false);
        replicationService.saveReplicationDefinition(rd);
        actionTrackingService.recordActionExecuting(rd);
        actionTrackingService.requestActionCancellation(rd);
        String startedAt3 = ISO8601DateFormat.format(rd.getExecutionStartDate());
        
        response = sendRequest(new GetRequest(URL_DEFINITIONS), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr);
        results = (ArrayNode) json.get("data");
        assertNotNull(results);
        assertEquals(3, results.size());
        
        jsonRD = results.get(0);
        assertNotNull(jsonRD);
        assertEquals("AnotherTest", jsonRD.get("name").textValue());
        assertEquals("CancelRequested", jsonRD.get("status").textValue());
        assertFalse(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt3, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/AnotherTest", jsonRD.get("details").textValue());
        
        jsonRD = results.get(1);
        assertNotNull(jsonRD);
        assertEquals("Test1", jsonRD.get("name").textValue());
        assertEquals("Running", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test1", jsonRD.get("details").textValue());
        
        jsonRD = results.get(2);
        assertNotNull(jsonRD);
        assertEquals("Test2", jsonRD.get("name").textValue());
        assertEquals("Completed", jsonRD.get("status").textValue());
        assertTrue(jsonRD.get("enabled").booleanValue());
        assertEquals(startedAt2, jsonRD.get("startedAt").get("iso8601").textValue());
        assertEquals("/api/replication-definition/Test2", jsonRD.get("details").textValue());
    }
    
    public void testReplicationDefinitionGet() throws Exception
    {
        Response response;
        
        
        // Not allowed if you're not an admin
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getGuestUserName());
        response = sendRequest(new GetRequest(URL_DEFINITION + "madeup"), Status.STATUS_UNAUTHORIZED);
        assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
        
        AuthenticationUtil.setFullyAuthenticatedUser(USER_NORMAL);
        response = sendRequest(new GetRequest(URL_DEFINITION + "madeup"), Status.STATUS_UNAUTHORIZED);
        assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
        
       
        // If an invalid name is given, you get a 404
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        response = sendRequest(new GetRequest(URL_DEFINITION + "madeup"), 404);
        assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
        
        
        // Add a definition, it should show up
        ReplicationDefinition rd = replicationService.createReplicationDefinition("Test1", "Testing");
        replicationService.saveReplicationDefinition(rd);
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        String jsonStr = response.getContentAsString();
        JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        assertNotNull(json);
        
        // Check 
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("New", json.get("status").textValue());
        assertEquals(NullNode.getInstance(), json.get("startedAt"));
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals(NullNode.getInstance(), json.get("executionDetails"));
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        // Payload is empty
        assertEquals(0, json.get("payload").size());
        
        
        // Ensure we didn't get any unexpected data back
        Iterator<String> iterator = json.fieldNames();
        while(iterator.hasNext())
        {
            String key = iterator.next();
           if(key.equals("name") || key.equals("description") || 
               key.equals("status") || key.equals("startedAt") ||
               key.equals("endedAt") || key.equals("failureMessage") ||
               key.equals("executionDetails") || key.equals("payload") ||
               key.equals("transferLocalReport") ||
               key.equals("transferRemoteReport") ||
               key.equals("enabled") || key.equals("targetName") || key.equals("schedule") ||
               key.equals("targetExists")) {
              // All good
           } else {
              fail("Unexpected key '"+key+"' found in json, raw json is\n" + jsonStr);
           }
        }
        
        
        // Mark it as pending, and check
        actionTrackingService.recordActionPending(rd);
        String actionId = rd.getId();
        int instanceId = ((ActionImpl)rd).getExecutionInstance();
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("Pending", json.get("status").textValue());
        assertEquals(NullNode.getInstance(), json.get("startedAt"));
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        // Payload is empty
        assertEquals(0, json.get("payload").size());
        
        
        // Change the status to running, and re-check
        actionTrackingService.recordActionExecuting(rd);
        assertEquals(actionId, rd.getId());
        assertEquals(instanceId, ((ActionImpl)rd).getExecutionInstance());
        String startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("Running", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        // Payload is empty
        assertEquals(0, json.get("payload").size());
        
        
        // Cancel it
        actionTrackingService.requestActionCancellation(rd);
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("CancelRequested", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        // Payload is empty
        assertEquals(0, json.get("payload").size());

        
        // Add some payload details, ensure that they get expanded
        //  as they should be
        rd.getPayload().add(
              repositoryHelper.getCompanyHome()
        );
        rd.getPayload().add( dataDictionary );
        replicationService.saveReplicationDefinition(rd);
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("CancelRequested", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        
        // Check Payload
        assertEquals(2, json.get("payload").size());
        
        JsonNode payload = json.get("payload").get(0);
        assertEquals(repositoryHelper.getCompanyHome().toString(), payload.get("nodeRef").textValue());
        assertTrue(payload.get("isFolder").booleanValue());
        assertEquals("Company Home", payload.get("name").textValue());
        assertEquals("/Company Home", payload.get("path").textValue());

        payload = json.get("payload").get(1);
        assertEquals(dataDictionary.toString(), payload.get("nodeRef").textValue());
        assertTrue(payload.get("isFolder").booleanValue());
        assertEquals("Data Dictionary", payload.get("name").textValue());
        assertEquals("/Company Home/Data Dictionary", payload.get("path").textValue());
        
        
        // Add a deleted NodeRef too, will be silently ignored
        //  by the webscript layer
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        NodeRef deleted = nodeService.createNode(
              dataDictionary, ContentModel.ASSOC_CONTAINS,
              QName.createQName("IwillBEdeleted"),
              ContentModel.TYPE_CONTENT
        ).getChildRef();
        nodeService.deleteNode(deleted);
        txn.commit();
        
        rd.getPayload().add( deleted );
        replicationService.saveReplicationDefinition(rd);
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("CancelRequested", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        
        // Check Payload
        assertEquals(2, json.get("payload").size());
        payload = json.get("payload").get(0);
        assertEquals("Company Home", payload.get("name").textValue());
        payload = json.get("payload").get(1);
        assertEquals("Data Dictionary", payload.get("name").textValue());

        
        // Add a 2nd and 3rd definition
        rd = replicationService.createReplicationDefinition("Test2", "2nd Testing");
        replicationService.saveReplicationDefinition(rd);
        
        rd = replicationService.createReplicationDefinition("Test3", "3rd Testing");
        rd.setLocalTransferReport( repositoryHelper.getRootHome() );
        rd.setRemoteTransferReport( repositoryHelper.getCompanyHome() );
        rd.setEnabled(false);
        
        // Have the 3rd one flagged as having failed
        txn = transactionService.getUserTransaction();
        txn.begin();
        replicationService.saveReplicationDefinition(rd);
        actionTrackingService.recordActionExecuting(rd);
        actionTrackingService.recordActionFailure(rd, new Exception("Test Failure"));
        txn.commit();
        Thread.sleep(50);
        replicationService.saveReplicationDefinition(rd);
        
        
        // Original one comes back unchanged
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test1"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test1", json.get("name").textValue());
        assertEquals("Testing", json.get("description").textValue());
        assertEquals("CancelRequested", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        
        // Check Payload
        assertEquals(2, json.get("payload").size());
        
        payload = json.get("payload").get(0);
        assertEquals(repositoryHelper.getCompanyHome().toString(), payload.get("nodeRef").textValue());
        assertTrue(payload.get("isFolder").booleanValue());
        assertEquals("Company Home", payload.get("name").textValue());
        assertEquals("/Company Home", payload.get("path").textValue());

        payload = json.get("payload").get(1);
        assertEquals(dataDictionary.toString(), payload.get("nodeRef").textValue());
        assertTrue(payload.get("isFolder").booleanValue());
        assertEquals("Data Dictionary", payload.get("name").textValue());
        assertEquals("/Company Home/Data Dictionary", payload.get("path").textValue());
        
        
        // They show up things as expected
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test2"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test2", json.get("name").textValue());
        assertEquals("2nd Testing", json.get("description").textValue());
        assertEquals("New", json.get("status").textValue());
        assertEquals(NullNode.getInstance(), json.get("startedAt"));
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals(NullNode.getInstance(), json.get("executionDetails"));
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertTrue(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());
        
        
        // And the 3rd one, which is failed
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test3"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        String endedAt = ISO8601DateFormat.format(rd.getExecutionEndDate());
        
        assertEquals("Test3", json.get("name").textValue());
        assertEquals("3rd Testing", json.get("description").textValue());
        assertEquals("Failed", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(endedAt, json.get("endedAt").get("iso8601").textValue());
        assertEquals("Test Failure", json.get("failureMessage").textValue());
        assertEquals(NullNode.getInstance(), json.get("executionDetails"));
        assertEquals(repositoryHelper.getRootHome().toString(), json.get("transferLocalReport").textValue());
        assertEquals(repositoryHelper.getCompanyHome().toString(), json.get("transferRemoteReport").textValue());
        assertFalse(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());
        
        
        // When pending/running, the previous end time, transfer reports and
        //  failure details are hidden
        rd = replicationService.loadReplicationDefinition("Test3");
        assertEquals(0, actionTrackingService.getExecutingActions(rd).size());
        actionTrackingService.recordActionPending(rd);
        assertEquals(1, actionTrackingService.getExecutingActions(rd).size());
        instanceId = ((ActionImpl)rd).getExecutionInstance();
        actionId = rd.getId();
        
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test3"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        
        assertEquals("Test3", json.get("name").textValue());
        assertEquals("3rd Testing", json.get("description").textValue());
        assertEquals("Pending", json.get("status").textValue());
        assertEquals(NullNode.getInstance(), json.get("startedAt"));
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertFalse(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());
        
        
        actionTrackingService.recordActionExecuting(rd);
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test3"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        
        assertEquals("Test3", json.get("name").textValue());
        assertEquals("3rd Testing", json.get("description").textValue());
        assertEquals("Running", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertFalse(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());
        
        
        actionTrackingService.requestActionCancellation(rd);
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test3"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        
        assertEquals("Test3", json.get("name").textValue());
        assertEquals("3rd Testing", json.get("description").textValue());
        assertEquals("CancelRequested", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("endedAt"));
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
              actionId + "=" + instanceId, json.get("executionDetails").textValue());
        assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
        assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
        assertFalse(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());

        
        // These show up again when no longer running
        txn = transactionService.getUserTransaction();
        txn.begin();
        actionTrackingService.recordActionComplete(rd);
        txn.commit();
        response = sendRequest(new GetRequest(URL_DEFINITION + "Test3"), 200);
        assertEquals(Status.STATUS_OK, response.getStatus());
        
        jsonStr = response.getContentAsString();
        json = AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
        startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
        endedAt = ISO8601DateFormat.format(rd.getExecutionEndDate());
        
        assertEquals("Test3", json.get("name").textValue());
        assertEquals("3rd Testing", json.get("description").textValue());
        assertEquals("Completed", json.get("status").textValue());
        assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
        assertEquals(endedAt, json.get("endedAt").get("iso8601").textValue());
        assertEquals(NullNode.getInstance(), json.get("failureMessage"));
        assertEquals(NullNode.getInstance(), json.get("executionDetails"));
        assertEquals(repositoryHelper.getRootHome().toString(), json.get("transferLocalReport").textValue());
        assertEquals(repositoryHelper.getCompanyHome().toString(), json.get("transferRemoteReport").textValue());
        assertFalse(json.get("enabled").booleanValue());
        assertEquals(NullNode.getInstance(), json.get("targetName"));
        assertEquals(0, json.get("payload").size());
    }
    
    public void testReplicationDefinitionsPost() throws Exception
    {
       Response response;
       
       
       // Not allowed if you're not an admin
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getGuestUserName());
       response = sendRequest(new PostRequest(URL_DEFINITIONS, "", JSON), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
       
       AuthenticationUtil.setFullyAuthenticatedUser(USER_NORMAL);
       response = sendRequest(new PostRequest(URL_DEFINITIONS, "", JSON), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());

       
       // Ensure there aren't any to start with
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
       assertEquals(0, replicationService.loadReplicationDefinitions().size());

       
       // If you don't give it name + description, it won't like you
       ObjectNode json = AlfrescoDefaultObjectMapper.createObjectNode();
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_BAD_REQUEST);
       assertEquals(Status.STATUS_BAD_REQUEST, response.getStatus());
       
       json.put("name", "New Definition");
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_BAD_REQUEST);
       assertEquals(Status.STATUS_BAD_REQUEST, response.getStatus());
       
       
       // If it has both, it'll work
       json.put("description", "Testing");
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       
       // Check we got the right information back
       String jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("New Definition", json.get("name").textValue());
       assertEquals("Testing", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals(NullNode.getInstance(), json.get("targetName"));
       assertEquals(0, json.get("payload").size());
       
     
       // Check that the right stuff ended up in the repository
       ReplicationDefinition rd = replicationService.loadReplicationDefinition("New Definition");
       assertEquals("New Definition", rd.getReplicationName());
       assertEquals("Testing", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals(null, rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(true, rd.isEnabled());
       
       
       // Post with the full set of options
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", "Test");
       json.put("description", "Test Description");
       json.put("targetName", "Target");
       json.put("enabled", false);
       ArrayNode payloadRefs = AlfrescoDefaultObjectMapper.createArrayNode();
       payloadRefs.add(repositoryHelper.getCompanyHome().toString());
       payloadRefs.add(dataDictionary.toString());
       json.put("payload", payloadRefs);
       
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       
       // Check the response for this
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Test Description", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertFalse(json.get("enabled").booleanValue());
       assertEquals("Target", json.get("targetName").textValue());
       assertEquals(2, json.get("payload").size());
       
       JsonNode payload = json.get("payload").get(0);
       assertEquals(repositoryHelper.getCompanyHome().toString(), payload.get("nodeRef").textValue());
       assertTrue(payload.get("isFolder").booleanValue());
       assertEquals("Company Home", payload.get("name").textValue());
       assertEquals("/Company Home", payload.get("path").textValue());

       payload = json.get("payload").get(1);
       assertEquals(dataDictionary.toString(), payload.get("nodeRef").textValue());
        assertTrue(payload.get("isFolder").booleanValue());
       assertEquals("Data Dictionary", payload.get("name").textValue());
       assertEquals("/Company Home/Data Dictionary", payload.get("path").textValue());
       
       
       // Check the database for this
       rd = replicationService.loadReplicationDefinition("Test");
       assertEquals("Test", rd.getReplicationName());
       assertEquals("Test Description", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals("Target", rd.getTargetName());
       assertEquals(false, rd.isEnabled());       
       assertEquals(2, rd.getPayload().size());
       assertEquals(repositoryHelper.getCompanyHome(), rd.getPayload().get(0));
       assertEquals(dataDictionary, rd.getPayload().get(1));

       
       // Ensure that the original one wasn't changed by anything
       rd = replicationService.loadReplicationDefinition("New Definition");
       assertEquals("New Definition", rd.getReplicationName());
       assertEquals("Testing", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals(null, rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(true, rd.isEnabled());
       
       
       // Ensure we can't create with a duplicate name
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", "Test");
       json.put("description", "New Duplicate");
       json.put("targetName", "New Duplicate Target");
       
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_BAD_REQUEST);
       assertEquals(Status.STATUS_BAD_REQUEST, response.getStatus());
       
       // Ensure that even though we got BAD REQUEST back, nothing changed
       rd = replicationService.loadReplicationDefinition("New Definition");
       assertEquals("New Definition", rd.getReplicationName());
       assertEquals("Testing", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals(null, rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(true, rd.isEnabled());
    }
    
    public void testReplicationDefinitionPut() throws Exception
    {
       Response response;
       
       
       // Not allowed if you're not an admin
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getGuestUserName());
       response = sendRequest(new PutRequest(URL_DEFINITION + "MadeUp", "", JSON), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
       
       AuthenticationUtil.setFullyAuthenticatedUser(USER_NORMAL);
       response = sendRequest(new PutRequest(URL_DEFINITION + "MadeUp", "", JSON), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());

       
       // Ensure there aren't any to start with
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       
       
       // You need to specify a real definition
       response = sendRequest(new PutRequest(URL_DEFINITION + "MadeUp", "", JSON), Status.STATUS_NOT_FOUND);
       assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
       
       
       // Create one, and change it
       ReplicationDefinition rd = replicationService.createReplicationDefinition("Test", "Testing");
       replicationService.saveReplicationDefinition(rd);
       
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", "{}", JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       
       // Check we got the right information back on it
       String jsonStr = response.getContentAsString();
       ObjectNode json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Testing", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals(NullNode.getInstance(), json.get("targetName"));
       assertEquals(0, json.get("payload").size());
       
       
       // Ensure we didn't get any unexpected data back
        Iterator<String> iterator = json.fieldNames();
        while(iterator.hasNext())
        {
            String key = iterator.next();
          if(key.equals("name") || key.equals("description") || 
              key.equals("status") || key.equals("startedAt") ||
              key.equals("endedAt") || key.equals("failureMessage") ||
              key.equals("executionDetails") || key.equals("payload") ||
              key.equals("transferLocalReport") ||
              key.equals("transferRemoteReport") ||
              key.equals("enabled") || key.equals("targetName") || key.equals("schedule") ||
              key.equals("targetExists"))
          {
             // All good
          } else {
             fail("Unexpected key '"+key+"' found in json, raw json is\n" + jsonStr);
          }
       }
       
       
       
       // Change some details, and see them updated in both
       //  the JSON and on the object in the repo
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("description", "Updated Description");
       json.put("enabled", false);
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Updated Description", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertFalse(json.get("enabled").booleanValue());
       assertEquals(NullNode.getInstance(), json.get("targetName"));
       assertEquals(0, json.get("payload").size());
       
       rd = replicationService.loadReplicationDefinition("Test");
       assertEquals("Test", rd.getReplicationName());
       assertEquals("Updated Description", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals(null, rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(false, rd.isEnabled());
       
       
       
       // Create a 2nd definition, and check that the correct
       //  one gets updated
       rd = replicationService.createReplicationDefinition("Test2", "Testing2");
       rd.setTargetName("Target");
       replicationService.saveReplicationDefinition(rd);
       
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("description", "Updated Description 2");
       json.put("enabled", false);
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test2", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       // Check the response we got
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test2", json.get("name").textValue());
       assertEquals("Updated Description 2", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertFalse(json.get("enabled").booleanValue());
       assertEquals("Target", json.get("targetName").textValue());
       assertEquals(0, json.get("payload").size());
       
       // Check the 1st definition
       rd = replicationService.loadReplicationDefinition("Test");
       assertEquals("Test", rd.getReplicationName());
       assertEquals("Updated Description", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals(null, rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(false, rd.isEnabled());
       
       // Check the 2nd definition
       rd = replicationService.loadReplicationDefinition("Test2");
       assertEquals("Test2", rd.getReplicationName());
       assertEquals("Updated Description 2", rd.getDescription());
       assertEquals(ActionStatus.New, rd.getExecutionStatus());
       assertEquals(null, rd.getExecutionStartDate());
       assertEquals(null, rd.getExecutionEndDate());
       assertEquals(null, rd.getExecutionFailureMessage());
       assertEquals(null, rd.getLocalTransferReport());
       assertEquals(null, rd.getRemoteTransferReport());
       assertEquals("Target", rd.getTargetName());
       assertEquals(0, rd.getPayload().size());
       assertEquals(false, rd.isEnabled());
       
       
       // Mark it as running, then change some details and
       //  see it change as expected
       rd = replicationService.loadReplicationDefinition("Test");
       actionTrackingService.recordActionExecuting(rd);
       replicationService.saveReplicationDefinition(rd);
       String startedAt = ISO8601DateFormat.format(rd.getExecutionStartDate());
       String actionId = rd.getId();
       int instanceId = ((ActionImpl)rd).getExecutionInstance();
       
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("enabled", true);
       json.put("targetName", "Another Target");
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Updated Description", json.get("description").textValue());
       assertEquals("Running", json.get("status").textValue());
       assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
             actionId + "=" + instanceId, json.get("executionDetails").textValue());
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals("Another Target", json.get("targetName").textValue());
       assertEquals(0, json.get("payload").size());

       
       // Change the payload, and see the right information in
       //  the response JSON for it
       ArrayNode payloadRefs = AlfrescoDefaultObjectMapper.createArrayNode();
       payloadRefs.add(repositoryHelper.getCompanyHome().toString());
       payloadRefs.add(dataDictionary.toString());
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("payload", payloadRefs);
       
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Updated Description", json.get("description").textValue());
       assertEquals("Running", json.get("status").textValue());
       assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
             actionId + "=" + instanceId, json.get("executionDetails").textValue());
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals("Another Target", json.get("targetName").textValue());
       assertEquals(2, json.get("payload").size());
       
       JsonNode payload = json.get("payload").get(0);
       assertEquals(repositoryHelper.getCompanyHome().toString(), payload.get("nodeRef").textValue());
       assertTrue(payload.get("isFolder").booleanValue());
       assertEquals("Company Home", payload.get("name").textValue());
       assertEquals("/Company Home", payload.get("path").textValue());

       payload = json.get("payload").get(1);
       assertEquals(dataDictionary.toString(), payload.get("nodeRef").textValue());
       assertTrue(payload.get("isFolder").booleanValue());
       assertEquals("Data Dictionary", payload.get("name").textValue());
       assertEquals("/Company Home/Data Dictionary", payload.get("path").textValue());
       
       
       // Remove the payload again
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       payloadRefs = AlfrescoDefaultObjectMapper.createArrayNode();
       json.put("payload", payloadRefs);
       
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Test", json.get("name").textValue());
       assertEquals("Updated Description", json.get("description").textValue());
       assertEquals("Running", json.get("status").textValue());
       assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
             actionId + "=" + instanceId, json.get("executionDetails").textValue());
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals("Another Target", json.get("targetName").textValue());
       assertEquals(0, json.get("payload").size());

       
       // Rename to a taken name, won't be allowed
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", "Test2");
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_BAD_REQUEST);
       assertEquals(Status.STATUS_BAD_REQUEST, response.getStatus());
       
       
       // Rename to a spare name, will be updated
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", "Renamed");
       
       response = sendRequest(new PutRequest(URL_DEFINITION + "Test", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Renamed", json.get("name").textValue());
       assertEquals("Updated Description", json.get("description").textValue());
       assertEquals("Running", json.get("status").textValue());
       assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
             actionId + "=" + instanceId, json.get("executionDetails").textValue());
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals("Another Target", json.get("targetName").textValue());
       assertEquals(0, json.get("payload").size());
       
       // Check the repo too
       assertEquals(null, replicationService.loadReplicationDefinition("Test"));
       assertNotNull(replicationService.loadReplicationDefinition("Renamed"));

       
       // Rename can both rename + change details
       json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", "Renamed Again");
       json.put("description", "Was Renamed");
       json.put("targetName", "New Target");
       
       response = sendRequest(new PutRequest(URL_DEFINITION + "Renamed", json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals("Renamed Again", json.get("name").textValue());
       assertEquals("Was Renamed", json.get("description").textValue());
       assertEquals("Running", json.get("status").textValue());
       assertEquals(startedAt, json.get("startedAt").get("iso8601").textValue());
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals("/" + URL_RUNNING_ACTION + "replicationActionExecutor="+
             actionId + "=" + instanceId, json.get("executionDetails").textValue());
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals("New Target", json.get("targetName").textValue());
       assertEquals(0, json.get("payload").size());
    }
    
    public void testReplicationDefinitionDelete() throws Exception 
    {
       Response response;
       
       
       // Not allowed if you're not an admin
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getGuestUserName());
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "MadeUp"), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());
       
       AuthenticationUtil.setFullyAuthenticatedUser(USER_NORMAL);
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "MadeUp"), Status.STATUS_UNAUTHORIZED);
       assertEquals(Status.STATUS_UNAUTHORIZED, response.getStatus());

       
       // Ensure there aren't any to start with
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       
       
       // You need to specify a real definition
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "MadeUp"), Status.STATUS_NOT_FOUND);
       assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
       
       
       // Create one, and then delete it
       ReplicationDefinition rd = replicationService.createReplicationDefinition("Test", "Testing");
       replicationService.saveReplicationDefinition(rd);
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       
       // Because some of the delete operations happen post-commit, and
       //  because we don't have real transactions, fake it
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Call the delete webscript
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "Test"), Status.STATUS_NO_CONTENT);
       assertEquals(Status.STATUS_NO_CONTENT, response.getStatus());
       
       // Let the node service do its work
       txn.commit();
       Thread.sleep(50);
       
       
       // Check the details webscript to ensure it went
       response = sendRequest(new GetRequest(URL_DEFINITION + "Test"), Status.STATUS_NOT_FOUND);
       assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
       
       
       // Check the replication service to ensure it went
       assertNull(replicationService.loadReplicationDefinition("Test"));
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
       
       
       // If there are several, make sure the right one goes
       rd = replicationService.createReplicationDefinition("Test", "Testing");
       replicationService.saveReplicationDefinition(rd);
       rd = replicationService.createReplicationDefinition("Test 2", "Testing");
       replicationService.saveReplicationDefinition(rd);
       rd = replicationService.createReplicationDefinition("Test 3", "Testing");
       replicationService.saveReplicationDefinition(rd);
       
       // Delete one of three, correct one goes
       assertEquals(3, replicationService.loadReplicationDefinitions().size());
       
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "Test"), Status.STATUS_NO_CONTENT);
       assertEquals(Status.STATUS_NO_CONTENT, response.getStatus());
       
       txn.commit();
       Thread.sleep(50);
       
       assertEquals(2, replicationService.loadReplicationDefinitions().size());
       assertNull(replicationService.loadReplicationDefinition("Test"));
       assertNotNull(replicationService.loadReplicationDefinition("Test 2"));
       assertNotNull(replicationService.loadReplicationDefinition("Test 3"));
       
       // Delete the next one, correct one goes
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "Test 3"), Status.STATUS_NO_CONTENT);
       assertEquals(Status.STATUS_NO_CONTENT, response.getStatus());
       
       txn.commit();
       Thread.sleep(50);
       
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertNull(replicationService.loadReplicationDefinition("Test"));
       assertNotNull(replicationService.loadReplicationDefinition("Test 2"));
       assertNull(replicationService.loadReplicationDefinition("Test 3"));
       
       
       // Ensure you can't delete for a 2nd time
       txn = transactionService.getUserTransaction();
       txn.begin();
       
       response = sendRequest(new DeleteRequest(URL_DEFINITION + "Test 3"), Status.STATUS_NOT_FOUND);
       assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
       
       txn.commit();
       Thread.sleep(50);
       
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertNull(replicationService.loadReplicationDefinition("Test"));
       assertNotNull(replicationService.loadReplicationDefinition("Test 2"));
       assertNull(replicationService.loadReplicationDefinition("Test 3"));
    }
    
    /**
     * Test that when creating and working with replication
     *  definitions with a name that includes "nasty"
     *  characters, things still work.
     * Related to ALF-4610.
     */
    public void testReplicationDefinitionsNastyNames() throws Exception
    {
       AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
       Response response;
       String jsonStr;
       
       String nastyName = "~!@#$%^&()_+-={}[];";
       String nastyNameURL = URLEncoder.encodeUriComponent(nastyName);
       
       
       // Create
       ObjectNode json = AlfrescoDefaultObjectMapper.createObjectNode();
       json.put("name", nastyName);
       json.put("description", "Nasty Characters");
       response = sendRequest(new PostRequest(URL_DEFINITIONS, json.toString(), JSON), Status.STATUS_OK);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals(nastyName, json.get("name").textValue());
       assertEquals("Nasty Characters", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       assertEquals(NullNode.getInstance(), json.get("startedAt"));
       assertEquals(NullNode.getInstance(), json.get("endedAt"));
       assertEquals(NullNode.getInstance(), json.get("failureMessage"));
       assertEquals(NullNode.getInstance(), json.get("executionDetails"));
       assertEquals(NullNode.getInstance(), json.get("transferLocalReport"));
       assertEquals(NullNode.getInstance(), json.get("transferRemoteReport"));
       assertTrue(json.get("enabled").booleanValue());
       assertEquals(NullNode.getInstance(), json.get("targetName"));
       assertEquals(0, json.get("payload").size());
       
       
       // Check it turned up
       assertEquals(1, replicationService.loadReplicationDefinitions().size());
       assertEquals(nastyName, replicationService.loadReplicationDefinitions().get(0).getReplicationName());
       
       
       // Fetch the details
       response = sendRequest(new GetRequest(URL_DEFINITION + nastyNameURL), 200);
       assertEquals(Status.STATUS_OK, response.getStatus());
       
       jsonStr = response.getContentAsString();
       json = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(jsonStr).get("data");
       assertNotNull(json);
       
       assertEquals(nastyName, json.get("name").textValue());
       assertEquals("Nasty Characters", json.get("description").textValue());
       assertEquals("New", json.get("status").textValue());
       
       
       // Delete
       // Because some of the delete operations happen post-commit, and
       //  because we don't have real transactions, fake it
       UserTransaction txn = transactionService.getUserTransaction();
       txn.begin();
       
       // Call the delete webscript
       response = sendRequest(new DeleteRequest(URL_DEFINITION + nastyNameURL), Status.STATUS_NO_CONTENT);
       assertEquals(Status.STATUS_NO_CONTENT, response.getStatus());
       
       // Let the node service do its work
       txn.commit();
       Thread.sleep(50);
       
       // Check the details webscript to ensure it went
       response = sendRequest(new GetRequest(URL_DEFINITION + nastyNameURL), Status.STATUS_NOT_FOUND);
       assertEquals(Status.STATUS_NOT_FOUND, response.getStatus());
       
       // And check the service too
       assertEquals(0, replicationService.loadReplicationDefinitions().size());
    }
    
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        ApplicationContext appContext = getServer().getApplicationContext();

        nodeService = (NodeService)appContext.getBean("NodeService");
        replicationService = (ReplicationService)appContext.getBean("ReplicationService");
        actionTrackingService = (ActionTrackingService)appContext.getBean("actionTrackingService");
        repositoryHelper = (Repository)appContext.getBean("repositoryHelper");
        transactionService = (TransactionService)appContext.getBean("transactionService");
        
        MutableAuthenticationService authenticationService = (MutableAuthenticationService)appContext.getBean("AuthenticationService");
        PersonService personService = (PersonService)appContext.getBean("PersonService");
        personManager = new TestPersonManager(authenticationService, personService, nodeService);

        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        
        personManager.createPerson(USER_NORMAL);
        
        // Ensure we start with no replication definitions
        // (eg another test left them behind)
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        for(ReplicationDefinition rd : replicationService.loadReplicationDefinitions()) {
           replicationService.deleteReplicationDefinition(rd);
        }
        txn.commit();
        
        // Grab a reference to the data dictionary
        dataDictionary = nodeService.getChildByName(
                 repositoryHelper.getCompanyHome(),
                 ContentModel.ASSOC_CONTAINS,
                 "Data Dictionary"
        );
        
        AuthenticationUtil.clearCurrentSecurityContext();
    }
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        UserTransaction txn = transactionService.getUserTransaction();
        txn.begin();
        
        personManager.clearPeople();
        
        // Zap any replication definitions we created
        AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getAdminUserName());
        for(ReplicationDefinition rd : replicationService.loadReplicationDefinitions()) {
           replicationService.deleteReplicationDefinition(rd);
        }
        AuthenticationUtil.clearCurrentSecurityContext();
        
        txn.commit();
    }
}
