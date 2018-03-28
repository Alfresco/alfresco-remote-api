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
package org.alfresco.repo.web.scripts.rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.MessageFormat;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.rule.LinkRules;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.web.scripts.BaseWebScriptTest;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterConstraint;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.TestWebScriptServer.DeleteRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.GetRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PostRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.PutRequest;
import org.springframework.extensions.webscripts.TestWebScriptServer.Response;

/**
 * Unit test to test rules Web Script API
 * 
 * @author Roy Wetherall
 *
 */
public class RuleServiceTest extends BaseWebScriptTest
{

    private static final String URL_RULETYPES = "/api/ruletypes";
    private static final String URL_ACTIONDEFINITIONS = "/api/actiondefinitions";
    private static final String URL_ACTIONCONDITIONDEFINITIONS = "/api/actionconditiondefinitions";

    private static final String URL_ACTIONCONSTRAINTS = "/api/actionConstraints";
    private static final String URL_ACTIONCONSTRAINT = "/api/actionConstraints/{0}";

    private static final String URL_QUEUE_ACTION = "/api/actionQueue?async={0}";

    private static final String URL_RULES = "/api/node/{0}/{1}/{2}/ruleset/rules";
    private static final String URL_INHERITED_RULES = "/api/node/{0}/{1}/{2}/ruleset/inheritedrules";
    private static final String URL_RULESET = "/api/node/{0}/{1}/{2}/ruleset";
    private static final String URL_RULE = "/api/node/{0}/{1}/{2}/ruleset/rules/{3}";

    private static final String TEST_STORE_IDENTIFIER = "test_store-" + System.currentTimeMillis();
    private static final String TEST_FOLDER = "test_folder-" + System.currentTimeMillis();
    private static final String TEST_FOLDER_2 = "test_folder-2-" + System.currentTimeMillis();

    private TransactionService transactionService;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private AuthenticationComponent authenticationComponent;
    private RuleService ruleService;
    private ActionService actionService;

    private NodeRef testNodeRef;
    private NodeRef testNodeRef2;
    private NodeRef testWorkNodeRef;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.transactionService = (TransactionService) getServer().getApplicationContext().getBean("TransactionService");
        this.nodeService = (NodeService) getServer().getApplicationContext().getBean("NodeService");
        this.fileFolderService = (FileFolderService) getServer().getApplicationContext().getBean("FileFolderService");
        this.ruleService = (RuleService) getServer().getApplicationContext().getBean("RuleService");
        this.actionService = (ActionService) getServer().getApplicationContext().getBean("ActionService");
        this.authenticationComponent = (AuthenticationComponent) getServer().getApplicationContext().getBean("authenticationComponent");

        this.authenticationComponent.setSystemUserAsCurrentUser();

        createTestFolders();

        assertNotNull(testWorkNodeRef);
        assertNotNull(testNodeRef);
        assertNotNull(testNodeRef2);
    }

    private void createTestFolders()
    {
        StoreRef testStore = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, TEST_STORE_IDENTIFIER);

        if (!nodeService.exists(testStore))
        {
            testStore = nodeService.createStore(StoreRef.PROTOCOL_WORKSPACE, TEST_STORE_IDENTIFIER);
        }

        NodeRef rootNodeRef = nodeService.getRootNode(testStore);

        testWorkNodeRef = nodeService.createNode(rootNodeRef, ContentModel.ASSOC_CHILDREN, QName.createQName("{test}testnode"), ContentModel.TYPE_FOLDER).getChildRef();

        testNodeRef = fileFolderService.create(testWorkNodeRef, TEST_FOLDER, ContentModel.TYPE_FOLDER).getNodeRef();
        testNodeRef2 = fileFolderService.create(testWorkNodeRef, TEST_FOLDER_2, ContentModel.TYPE_FOLDER).getNodeRef();
    }

    private String formatRulesUrl(NodeRef nodeRef, boolean inherited)
    {
        if (inherited)
        {
            return MessageFormat.format(URL_INHERITED_RULES, nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId());
        }
        else
        {
            return MessageFormat.format(URL_RULES, nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId());
        }
    }

    private String formatRulesetUrl(NodeRef nodeRef)
    {
        return MessageFormat.format(URL_RULESET, nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId());
    }

    private String formateRuleUrl(NodeRef nodeRef, String ruleId)
    {
        return MessageFormat.format(URL_RULE, nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), ruleId);
    }

    private String formateActionConstraintUrl(String name)
    {
        return MessageFormat.format(URL_ACTIONCONSTRAINT, name);
    }

    private String formateQueueActionUrl(boolean async)
    {
        return MessageFormat.format(URL_QUEUE_ACTION, async);
    }

    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        RetryingTransactionCallback<Void> deleteCallback = new RetryingTransactionCallback<Void>()
        {
            @Override
            public Void execute() throws Throwable
            {
                deleteNodeIfExists(testNodeRef2);
                deleteNodeIfExists(testNodeRef);
                deleteNodeIfExists(testWorkNodeRef);
                return null;
            }
            
            private void deleteNodeIfExists(NodeRef nodeRef)
            {
                if (nodeService.exists(nodeRef))
                {
                    nodeService.deleteNode(nodeRef);
                }
            }
        };
        this.transactionService.getRetryingTransactionHelper().doInTransaction(deleteCallback);
        this.authenticationComponent.clearCurrentSecurityContext();
    }

    private JsonNode createRule(NodeRef ruleOwnerNodeRef) throws Exception
    {
        return createRule(ruleOwnerNodeRef, "test_rule");
    }
    
    private JsonNode createRule(NodeRef ruleOwnerNodeRef, String title) throws Exception
    {
        JsonNode jsonRule = buildTestRule(title);

        Response response = sendRequest(new PostRequest(formatRulesUrl(ruleOwnerNodeRef, false), jsonRule.toString(), "application/json"), 200);

        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        return result;
    }

    private ArrayNode getNodeRules(NodeRef nodeRef, boolean inherited) throws Exception
    {
        Response response = sendRequest(new GetRequest(formatRulesUrl(nodeRef, inherited)), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        ArrayNode data = (ArrayNode) result.get("data");

        return data;
    }

    private void checkRuleComplete(JsonNode result) throws Exception
    {
        assertNotNull("Response is null.", result);

        // if id present in response -> rule was created
        assertTrue(result.has("id"));

        assertEquals(result.get("title").textValue(), "test_rule");
        assertEquals(result.get("description").textValue(), "this is description for test_rule");

        ArrayNode ruleType = (ArrayNode) result.get("ruleType");

        assertEquals(1, ruleType.size());
        assertEquals("outbound", ruleType.get(0).textValue());

        assertTrue(result.get("applyToChildren").booleanValue());
        assertFalse(result.get("executeAsynchronously").booleanValue());
        assertFalse(result.get("disabled").booleanValue());
        assertTrue(result.has("owningNode"));
        JsonNode owningNode = result.get("owningNode");
        assertTrue(owningNode.has("nodeRef"));
        assertTrue(owningNode.has("name"));
        assertTrue(result.has("url"));

        JsonNode jsonAction = result.get("action");

        assertTrue(jsonAction.has("id"));

        assertEquals(jsonAction.get("actionDefinitionName").textValue(), "composite-action");
        assertEquals(jsonAction.get("description").textValue(), "this is description for composite-action");
        assertEquals(jsonAction.get("title").textValue(), "test_title");

        assertTrue(jsonAction.get("executeAsync").booleanValue());

        assertTrue(jsonAction.has("actions"));
        assertTrue(jsonAction.has("conditions"));
        assertTrue(jsonAction.has("compensatingAction"));
        assertTrue(jsonAction.has("url"));
    }

    private void checkRuleSummary(JsonNode result) throws Exception
    {
        assertNotNull("Response is null.", result);

        assertTrue(result.has("data"));

        JsonNode data = result.get("data");

        // if id present in response -> rule was created
        assertTrue(data.has("id"));

        assertEquals(data.get("title").textValue(), "test_rule");
        assertEquals(data.get("description").textValue(), "this is description for test_rule");

        ArrayNode ruleType = (ArrayNode) data.get("ruleType");

        assertEquals(1, ruleType.size());
        assertEquals("outbound", ruleType.get(0));

        assertFalse(data.get("disabled").booleanValue());
        assertTrue(data.has("url"));

    }

    private void checkUpdatedRule(JsonNode before, JsonNode after)
    {
        // check saving of basic feilds 
        assertEquals("It seams that 'id' is not correct", before.get("id").textValue(), after.get("id").textValue());

        assertEquals("It seams that 'title' was not saved", before.get("title").textValue(), after.get("title").textValue());

        assertEquals("It seams that 'description' was not saved", before.get("description").textValue(), after.get("description").textValue());

        assertEquals("It seams that 'ruleType' was not saved", before.get("ruleType").size(), after.get("ruleType").size());

        assertEquals(before.get("applyToChildren").booleanValue(), after.get("applyToChildren").booleanValue());
        assertEquals(before.get("executeAsynchronously").booleanValue(), after.get("executeAsynchronously").booleanValue());
        assertEquals(before.get("disabled").booleanValue(), after.get("disabled").booleanValue());

        // check saving of collections        
        JsonNode afterAction = after.get("action");

        // we didn't change actions collection
        assertEquals(1, afterAction.get("actions").size());

        // conditions should be empty (should not present in response), 
        assertFalse(afterAction.has("conditions"));

        assertEquals(before.has("url"), after.has("url"));
    }

    private void checkRuleset(JsonNode result, int rulesCount, String[] ruleIds, int inhRulesCount, String[] parentRuleIds,
                                boolean isLinkedFrom, boolean isLinkedTo) throws Exception
    {
        assertNotNull("Response is null.", result);

        assertTrue(result.has("data"));

        JsonNode data = result.get("data");

        if (data.has("rules"))
        {
            ArrayNode rulesArray = (ArrayNode) data.get("rules");

            assertEquals(rulesCount, rulesArray.size());

            for (int i = 0; i < rulesArray.size(); i++)
            {
                JsonNode ruleSum = rulesArray.get(i);
                assertTrue(ruleSum.has("id"));
                assertEquals(ruleIds[i], ruleSum.get("id").textValue());
                assertTrue(ruleSum.has("title"));
                assertTrue(ruleSum.has("ruleType"));
                assertTrue(ruleSum.has("disabled"));
                assertTrue(ruleSum.has("owningNode"));
                JsonNode owningNode = ruleSum.get("owningNode");
                assertTrue(owningNode.has("nodeRef"));
                assertTrue(owningNode.has("name"));
                assertTrue(ruleSum.has("url"));
            }
        }

        if (data.has("inheritedRules"))
        {
            ArrayNode inheritedRulesArray = (ArrayNode) data.get("inheritedRules");

            assertEquals(inhRulesCount, inheritedRulesArray.size());

            for (int i = 0; i < inheritedRulesArray.size(); i++)
            {
                JsonNode ruleSum = inheritedRulesArray.get(i);
                assertTrue(ruleSum.has("id"));
                assertEquals(parentRuleIds[i], ruleSum.get("id").textValue());
                assertTrue(ruleSum.has("title"));
                assertTrue(ruleSum.has("ruleType"));
                assertTrue(ruleSum.has("disabled"));
                assertTrue(ruleSum.has("owningNode"));
                JsonNode owningNode = ruleSum.get("owningNode");
                assertTrue(owningNode.has("nodeRef"));
                assertTrue(owningNode.has("name"));
                assertTrue(ruleSum.has("url"));
            }
        }

        assertEquals(isLinkedTo, data.has("linkedToRuleSet"));
        
        assertEquals(isLinkedFrom, data.has("linkedFromRuleSets"));

        assertTrue(data.has("url"));
    }

    public void testGetRuleTypes() throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_RULETYPES), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        ArrayNode data = (ArrayNode) result.get("data");

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode ruleType = data.get(i);

            assertTrue(ruleType.has("name"));
            assertTrue(ruleType.has("displayLabel"));
            assertTrue(ruleType.has("url"));
        }
    }

    public void testGetActionDefinitions() throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_ACTIONDEFINITIONS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        ArrayNode data = (ArrayNode) result.get("data");

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode actionDefinition = data.get(i);

            assertTrue(actionDefinition.has("name"));
            assertTrue(actionDefinition.has("displayLabel"));
            assertTrue(actionDefinition.has("description"));
            assertTrue(actionDefinition.has("adHocPropertiesAllowed"));
            assertTrue(actionDefinition.has("parameterDefinitions"));
            assertTrue(actionDefinition.has("applicableTypes"));
        }
    }

    public void testGetActionConditionDefinitions() throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_ACTIONCONDITIONDEFINITIONS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        ArrayNode data = (ArrayNode) result.get("data");

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode actionConditionDefinition = data.get(i);

            assertTrue(actionConditionDefinition.has("name"));
            assertTrue(actionConditionDefinition.has("displayLabel"));
            assertTrue(actionConditionDefinition.has("description"));
            assertTrue(actionConditionDefinition.has("adHocPropertiesAllowed"));
            assertTrue(actionConditionDefinition.has("parameterDefinitions"));
        }
    }
    
    public void testGetActionConstraints() throws Exception
    {
        Response response = sendRequest(new GetRequest(URL_ACTIONCONSTRAINTS), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        ArrayNode data = (ArrayNode) result.get("data");

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode actionConstraint = data.get(i);

            assertTrue(actionConstraint.has("name"));
            assertTrue(actionConstraint.has("values"));

            ArrayNode values = (ArrayNode) actionConstraint.get("values");

            for (int j = 0; j < values.size(); j++)
            {
                JsonNode value = values.get(j);

                assertTrue(value.has("value"));
                assertTrue(value.has("displayLabel"));
            }
        }
    }
    
    public void testGetActionConstraint() throws Exception
    {

        List<ParameterConstraint> constraints = actionService.getParameterConstraints();

        if (constraints.size() == 0)
        {
            return;
        }

        String name = constraints.get(0).getName();

        Response response = sendRequest(new GetRequest(formateActionConstraintUrl(name)), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        JsonNode data = result.get("data");

        assertTrue(data.has("name"));
        assertTrue(data.has("values"));

        ArrayNode values = (ArrayNode) data.get("values");

        for (int i = 0; i < values.size(); i++)
        {
            JsonNode value = values.get(i);

            assertTrue(value.has("value"));
            assertTrue(value.has("displayLabel"));
        }
    }

    public void testQueueAction() throws Exception
    {
        String url = formateQueueActionUrl(false);

        ObjectNode copyAction = buildCopyAction(testWorkNodeRef);

        copyAction.put("actionedUponNode", testNodeRef.toString());

        // execute before response (should be successful)
        Response successResponse = sendRequest(new PostRequest(url, copyAction.toString(), "application/json"), 200);

        JsonNode successResult = AlfrescoDefaultObjectMapper.getReader().readTree(successResponse.getContentAsString());

        assertNotNull(successResult);

        assertTrue(successResult.has("data"));

        JsonNode successData = successResult.get("data");

        assertTrue(successData.has("status"));
        assertEquals("success", successData.get("status").textValue());
        assertTrue(successData.has("actionedUponNode"));
        assertFalse(successData.has("exception"));
        assertTrue(successData.has("action"));

        // execute before response (should fail)
        sendRequest(new PostRequest(url, copyAction.toString(), "application/json"), 500);

        // execute after response (should fail but error should not present in response)
        String asyncUrl = formateQueueActionUrl(true);
        Response response = sendRequest(new PostRequest(asyncUrl, copyAction.toString(), "application/json"), 200);

        // wait while action executed
        Thread.sleep(1000);

        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("data"));

        JsonNode data = result.get("data");

        assertTrue(data.has("status"));
        assertEquals("queued", data.get("status").textValue());
        assertTrue(data.has("actionedUponNode"));
        assertFalse(data.has("exception"));
        assertTrue(data.has("action"));
    }

    public void testCreateRule() throws Exception
    {
        JsonNode result = createRule(testNodeRef);

        checkRuleSummary(result);

        List<Rule> rules = ruleService.getRules(testNodeRef);

        assertEquals(1, rules.size());

    }

    public void testGetRulesCollection() throws Exception
    {
        ArrayNode data = getNodeRules(testNodeRef, false);

        assertEquals(0, data.size());

        createRule(testNodeRef);

        data = getNodeRules(testNodeRef, false);

        assertEquals(1, data.size());

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode ruleSum = data.get(i);
            assertTrue(ruleSum.has("id"));
            assertTrue(ruleSum.has("title"));
            assertTrue(ruleSum.has("ruleType"));
            assertTrue(ruleSum.has("disabled"));
            assertTrue(ruleSum.has("owningNode"));
            JsonNode owningNode = ruleSum.get("owningNode");
            assertTrue(owningNode.has("nodeRef"));
            assertTrue(owningNode.has("name"));
            assertTrue(ruleSum.has("url"));
        }
    }

    public void testGetInheritedRulesCollection() throws Exception
    {
        ArrayNode data = getNodeRules(testNodeRef, true);

        assertEquals(0, data.size());

        createRule(testWorkNodeRef);

        data = getNodeRules(testNodeRef, true);

        assertEquals(1, data.size());

        for (int i = 0; i < data.size(); i++)
        {
            JsonNode ruleSum = data.get(i);
            assertTrue(ruleSum.has("id"));
            assertTrue(ruleSum.has("title"));
            assertTrue(ruleSum.has("ruleType"));
            assertTrue(ruleSum.has("disabled"));
            assertTrue(ruleSum.has("owningNode"));
            JsonNode owningNode = ruleSum.get("owningNode");
            assertTrue(owningNode.has("nodeRef"));
            assertTrue(owningNode.has("name"));
            assertTrue(ruleSum.has("url"));
        }
    }

    public void testGetRuleset() throws Exception
    {
        JsonNode parentRule = createRule(testWorkNodeRef);
        String[] parentRuleIds = new String[] { parentRule.get("data").get("id").textValue() };

        JsonNode jsonRule = createRule(testNodeRef);
        String[] ruleIds = new String[] { jsonRule.get("data").get("id").textValue() };

        Action linkRulesAction = actionService.createAction(LinkRules.NAME);
        linkRulesAction.setParameterValue(LinkRules.PARAM_LINK_FROM_NODE, testNodeRef);
        actionService.executeAction(linkRulesAction, testNodeRef2);

        Response linkedFromResponse = sendRequest(new GetRequest(formatRulesetUrl(testNodeRef)), 200);
        JsonNode linkedFromResult = AlfrescoDefaultObjectMapper.getReader().readTree(linkedFromResponse.getContentAsString());
        
        checkRuleset(linkedFromResult, 1, ruleIds, 1, parentRuleIds, true, false);

        Response linkedToResponse = sendRequest(new GetRequest(formatRulesetUrl(testNodeRef2)), 200);
        JsonNode linkedToResult = AlfrescoDefaultObjectMapper.getReader().readTree(linkedToResponse.getContentAsString());
        
        checkRuleset(linkedToResult, 1, ruleIds, 1, parentRuleIds, false, true);
    }

    public void testGetRuleDetails() throws Exception
    {
        JsonNode jsonRule = createRule(testNodeRef);

        String ruleId = jsonRule.get("data").get("id").textValue();

        Response response = sendRequest(new GetRequest(formateRuleUrl(testNodeRef, ruleId)), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        checkRuleComplete(result);
    }

    public void testUpdateRule() throws Exception
    {
        JsonNode jsonRule = createRule(testNodeRef);

        String ruleId = jsonRule.get("data").get("id").textValue();

        Response getResponse = sendRequest(new GetRequest(formateRuleUrl(testNodeRef, ruleId)), 200);

        ObjectNode before = (ObjectNode) AlfrescoDefaultObjectMapper.getReader().readTree(getResponse.getContentAsString());

        // do some changes
        before.put("description", "this is modified description for test_rule");

        // do some changes for action object
        ObjectNode beforeAction = (ObjectNode) before.get("action");
        // no changes for actions list  
        beforeAction.remove("actions");
        // clear conditions
        beforeAction.set("conditions", AlfrescoDefaultObjectMapper.createArrayNode());

        Response putResponse = sendRequest(new PutRequest(formateRuleUrl(testNodeRef, ruleId), before.toString(), "application/json"), 200);

        JsonNode after = AlfrescoDefaultObjectMapper.getReader().readTree(putResponse.getContentAsString());

        // sent and retrieved objects should be the same (except ids and urls)
        // this means that all changes was saved
        checkUpdatedRule(before, after);
    }

    public void testDeleteRule() throws Exception
    {
        JsonNode jsonRule = createRule(testNodeRef);

        assertEquals(1, ruleService.getRules(testNodeRef).size());

        String ruleId = jsonRule.get("data").get("id").textValue();

        Response response = sendRequest(new DeleteRequest(formateRuleUrl(testNodeRef, ruleId)), 200);
        JsonNode result = AlfrescoDefaultObjectMapper.getReader().readTree(response.getContentAsString());

        assertNotNull(result);

        assertTrue(result.has("success"));

        boolean success = result.get("success").booleanValue();

        assertTrue(success);

        // no more rules present 
        assertEquals(0, ruleService.getRules(testNodeRef).size());
    }
    
    @SuppressWarnings("unused")
    public void testRuleReorder() throws Exception
    {
        assertEquals(0, ruleService.getRules(testNodeRef).size());
        
        // Create 3 rules
        NodeRef rule1 = createRuleNodeRef(testNodeRef, "Rule 1");
        NodeRef rule2 = createRuleNodeRef(testNodeRef, "Rule 2");
        NodeRef rule3 = createRuleNodeRef(testNodeRef, "Rule 3");
        
        List<Rule> rules = ruleService.getRules(testNodeRef);
        assertEquals(3, rules.size());
        assertEquals("Rule 1", rules.get(0).getTitle());
        assertEquals("Rule 2", rules.get(1).getTitle());
        assertEquals("Rule 3", rules.get(2).getTitle());

        ObjectNode action = AlfrescoDefaultObjectMapper.createObjectNode();
        action.put("actionDefinitionName", "reorder-rules");
        action.put("actionedUponNode", testNodeRef.toString());

        ObjectNode params = AlfrescoDefaultObjectMapper.createObjectNode();
        ArrayNode orderArray = AlfrescoDefaultObjectMapper.createArrayNode();
        orderArray.add(rules.get(2).getNodeRef().toString());
        orderArray.add(rules.get(1).getNodeRef().toString());
        orderArray.add(rules.get(0).getNodeRef().toString());
        params.set("rules", orderArray);
        action.set("parameterValues", params);
        
        String url = formateQueueActionUrl(false);

        // execute before response (should be successful)
        Response successResponse = sendRequest(new PostRequest(url, action.toString(), "application/json"), 200);
        JsonNode successResult = AlfrescoDefaultObjectMapper.getReader().readTree(successResponse.getContentAsString());
        assertNotNull(successResult);
        assertTrue(successResult.has("data"));
        JsonNode successData = successResult.get("data");
        assertTrue(successData.has("status"));
        assertEquals("success", successData.get("status").textValue());
        assertTrue(successData.has("actionedUponNode"));
        assertFalse(successData.has("exception"));
        assertTrue(successData.has("action"));
        
        rules = ruleService.getRules(testNodeRef);
        assertEquals(3, rules.size());
        assertEquals("Rule 3", rules.get(0).getTitle());
        assertEquals("Rule 2", rules.get(1).getTitle());
        assertEquals("Rule 1", rules.get(2).getTitle());
    }
    
    private NodeRef createRuleNodeRef(NodeRef folder, String title) throws Exception
    {
        JsonNode jsonRule = createRule(folder, title);
        String id = jsonRule.get("data").get("id").textValue();
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, id);
    }

    private ObjectNode buildCopyAction(NodeRef destination)
    {
        ObjectNode result = AlfrescoDefaultObjectMapper.createObjectNode();

        // add actionDefinitionName
        result.put("actionDefinitionName", "copy");

        // build parameterValues
        ObjectNode parameterValues = AlfrescoDefaultObjectMapper.createObjectNode();
        parameterValues.put("destination-folder", destination.toString());

        // add parameterValues
        result.set("parameterValues", parameterValues);

        // add executeAsync
        result.put("executeAsync", false);

        return result;
    }

    private ObjectNode buildTestRule(String title)
    {
        ObjectNode result = AlfrescoDefaultObjectMapper.createObjectNode();

        result.put("title", title);
        result.put("description", "this is description for test_rule");

        ArrayNode ruleType = AlfrescoDefaultObjectMapper.createArrayNode();
        ruleType.add("outbound");

        result.set("ruleType", ruleType);

        result.put("applyToChildren", true);

        result.put("executeAsynchronously", false);

        result.put("disabled", false);

        result.set("action", buildTestAction("composite-action", true, true));

        return result;
    }

    private ObjectNode buildTestAction(String actionName, boolean addActions, boolean addCompensatingAction)
    {
        ObjectNode result = AlfrescoDefaultObjectMapper.createObjectNode();

        result.put("actionDefinitionName", actionName);
        result.put("description", "this is description for " + actionName);
        result.put("title", "test_title");

        //JsonNode parameterValues = AlfrescoDefaultObjectMapper.createObjectNode()
        //parameterValues.put("test_name", "test_value");

        //result.put("parameterValues", parameterValues);

        result.put("executeAsync", addActions);

        if (addActions)
        {
            ArrayNode actions = AlfrescoDefaultObjectMapper.createArrayNode();

            actions.add(buildTestAction("counter", false, false));

            result.set("actions", actions);
        }

        ArrayNode conditions = AlfrescoDefaultObjectMapper.createArrayNode();

        conditions.add(buildTestCondition("no-condition"));

        result.set("conditions", conditions);

        if (addCompensatingAction)
        {
            result.put("compensatingAction", buildTestAction("script", false, false));
        }

        return result;
    }

    private ObjectNode buildTestCondition(String conditionName)
    {
        ObjectNode result = AlfrescoDefaultObjectMapper.createObjectNode();

        result.put("conditionDefinitionName", conditionName);
        result.put("invertCondition", false);

        //JsonNode parameterValues = AlfrescoDefaultObjectMapper.createObjectNode()
        //parameterValues.put("test_name", "test_value");

        //result.put("parameterValues", parameterValues);

        return result;
    }
}
