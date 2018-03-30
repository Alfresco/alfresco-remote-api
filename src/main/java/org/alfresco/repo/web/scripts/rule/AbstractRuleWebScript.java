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
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.action.ActionConditionImpl;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.CompositeActionImpl;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.action.ParameterizedItemDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.service.cmr.rule.RuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.alfresco.util.json.JsonUtil;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author unknown
 *
 */
public abstract class AbstractRuleWebScript extends DeclarativeWebScript
{

    public static final SimpleDateFormat dateFormate = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    private static final String RULE_OUTBOUND = "outbound";
    private static final String ACTION_CHECK_OUT = "check-out";

    private static final String CANNOT_CREATE_RULE = "cannot.create.rule.checkout.outbound";
    
    protected NodeService nodeService;
    protected RuleService ruleService;
    protected DictionaryService dictionaryService;
    protected ActionService actionService;
    protected FileFolderService fileFolderService;
    protected NamespaceService namespaceService;

    /**
     * Sets the node service instance
     * 
     * @param nodeService the node service to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * Set rule service instance 
     * 
     * @param ruleService the rule service to set
     */
    public void setRuleService(RuleService ruleService)
    {
        this.ruleService = ruleService;
    }

    /**
     * Set dictionary service instance
     * 
     * @param dictionaryService the dictionary service to set 
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * Set action service instance
     * 
     * @param actionService the action service to set
     */
    public void setActionService(ActionService actionService)
    {
        this.actionService = actionService;
    }

    /**
     * Set file folder service instance
     * 
     * @param fileFolderService the fileFolderService to set
     */
    public void setFileFolderService(FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    /**
     * Set namespace service instance
     * 
     * @param namespaceService the namespace service to set
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * Parses the request and providing it's valid returns the NodeRef.
     * 
     * @param req The webscript request
     * @return The NodeRef passed in the request
     * 
     */
    protected NodeRef parseRequestForNodeRef(WebScriptRequest req)
    {
        // get the parameters that represent the NodeRef, we know they are present
        // otherwise this webscript would not have matched
        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        String storeType = templateVars.get("store_type");
        String storeId = templateVars.get("store_id");
        String nodeId = templateVars.get("id");

        // create the NodeRef and ensure it is valid
        StoreRef storeRef = new StoreRef(storeType, storeId);
        NodeRef nodeRef = new NodeRef(storeRef, nodeId);

        if (!this.nodeService.exists(nodeRef))
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Unable to find node: " + nodeRef.toString());
        }

        return nodeRef;
    }

    protected Rule parseJsonRule(JsonNode jsonRule)
    {
        Rule result = new Rule();

        if (jsonRule.has("title") == false || jsonRule.get("title").textValue().length() == 0)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Title missing when creating rule");
        }

        result.setTitle(jsonRule.get("title").textValue());

        result.setDescription(jsonRule.has("description") ? jsonRule.get("description").textValue() : "");

        if (jsonRule.has("ruleType") == false || jsonRule.get("ruleType").size() == 0)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Rule type missing when creating rule");
        }

        ArrayNode types = (ArrayNode) jsonRule.get("ruleType");
        List<String> ruleTypes = new ArrayList<String>();

        for (int i = 0; i < types.size(); i++)
        {
            ruleTypes.add(types.get(i).textValue());
        }

        result.setRuleTypes(ruleTypes);

        result.applyToChildren(jsonRule.has("applyToChildren") ?
                jsonRule.get("applyToChildren").booleanValue() : false);

        result.setExecuteAsynchronously(jsonRule.has("executeAsynchronously") ?
                jsonRule.get("executeAsynchronously").booleanValue() : false);

        result.setRuleDisabled(jsonRule.has("disabled") ? jsonRule.get("disabled").booleanValue() : false);

        JsonNode jsonAction = jsonRule.get("action");

        // parse action object
        Action ruleAction = parseJsonAction(jsonAction);

        result.setAction(ruleAction);

        return result;
    }

    protected ActionImpl parseJsonAction(JsonNode jsonAction)
    {
        ActionImpl result = null;

        String actionId = jsonAction.has("id") ? jsonAction.get("id").textValue() : GUID.generate();

        if (jsonAction.get("actionDefinitionName").textValue().equalsIgnoreCase("composite-action"))
        {
            result = new CompositeActionImpl(null, actionId);
        }
        else
        {
            result = new ActionImpl(null, actionId, jsonAction.get("actionDefinitionName").textValue());
        }

        // Post Action Queue parameter
        if (jsonAction.has("actionedUponNode"))
        {
            NodeRef actionedUponNode = new NodeRef(jsonAction.get("actionedUponNode").textValue());
            result.setNodeRef(actionedUponNode);
        }

        if (jsonAction.has("description"))
        {
            result.setDescription(jsonAction.get("description").textValue());
        }

        if (jsonAction.has("title"))
        {
            result.setTitle(jsonAction.get("title").textValue());
        }

        if (jsonAction.has("parameterValues"))
        {
            JsonNode jsonParameterValues = jsonAction.get("parameterValues");
            result.setParameterValues(parseJsonParameterValues(jsonParameterValues, result.getActionDefinitionName(), true));
        }

        if (jsonAction.has("executeAsync"))
        {
            result.setExecuteAsynchronously(jsonAction.get("executeAsync").booleanValue());
        }

        if (jsonAction.has("runAsUser"))
        {
            result.setRunAsUser(jsonAction.get("runAsUser").textValue());
        }

        if (jsonAction.has("actions"))
        {
            ArrayNode jsonActions = (ArrayNode) jsonAction.get("actions");

            for (int i = 0; i < jsonActions.size(); i++)
            {
                JsonNode innerJsonAction = jsonActions.get(i);

                Action innerAction = parseJsonAction(innerJsonAction);

                // we assume that only composite-action contains actions json array, so should be no cast exception
                ((CompositeActionImpl) result).addAction(innerAction);
            }
        }

        if (jsonAction.has("conditions"))
        {
            ArrayNode jsonConditions = (ArrayNode) jsonAction.get("conditions");

            for (int i = 0; i < jsonConditions.size(); i++)
            {
                JsonNode jsonCondition = jsonConditions.get(i);

                // parse action conditions
                ActionCondition actionCondition = parseJsonActionCondition(jsonCondition);

                result.getActionConditions().add(actionCondition);
            }
        }

        if (jsonAction.has("compensatingAction"))
        {
            Action compensatingAction = parseJsonAction(jsonAction.get("compensatingAction"));
            result.setCompensatingAction(compensatingAction);
        }

        return result;
    }

    protected ActionConditionImpl parseJsonActionCondition(JsonNode jsonActionCondition)
    {
        String id = jsonActionCondition.has("id") ? jsonActionCondition.get("id").textValue() : GUID.generate();

        ActionConditionImpl result = new ActionConditionImpl(id, jsonActionCondition.get("conditionDefinitionName").textValue());

        if (jsonActionCondition.has("invertCondition"))
        {
            result.setInvertCondition(jsonActionCondition.get("invertCondition").booleanValue());
        }

        if (jsonActionCondition.has("parameterValues"))
        {
            JsonNode jsonParameterValues = jsonActionCondition.get("parameterValues");

            result.setParameterValues(parseJsonParameterValues(jsonParameterValues, result.getActionConditionDefinitionName(), false));
        }

        return result;
    }

    protected Map<String, Serializable> parseJsonParameterValues(JsonNode jsonParameterValues, String name, boolean isAction)
    {
        Map<String, Serializable> parameterValues = new HashMap<String, Serializable>();

        Iterator<Map.Entry<String,JsonNode>> parametersIterator = jsonParameterValues.fields();
        if (jsonParameterValues.size() == 0)
        {
            return null;
        }

        // Get the action or condition definition
        ParameterizedItemDefinition definition = null;
        if (isAction == true)
        {
            definition = actionService.getActionDefinition(name);
        }
        else
        {
            definition = actionService.getActionConditionDefinition(name);
        }
        if (definition == null)
        {
            throw new AlfrescoRuntimeException("Could not find defintion for action/condition " + name);
        }

        while (parametersIterator.hasNext())
        {
            Map.Entry<String,JsonNode> parameter = parametersIterator.next();
            String propertyName = parameter.getKey();
            JsonNode propertyValue = parameter.getValue();
            
            // Get the parameter definition we care about
            ParameterDefinition paramDef = definition.getParameterDefintion(propertyName);
            if (paramDef == null && !definition.getAdhocPropertiesAllowed())
            {
                throw new AlfrescoRuntimeException("Invalid parameter " + propertyName + " for action/condition " + name);
            }
            if (paramDef != null)
            {
                QName typeQName = paramDef.getType();

                // Convert the property value
                Serializable value = convertValue(typeQName, propertyValue);
                parameterValues.put(propertyName, value);
            }
            else
            {
                // If there is no parameter definition we can only rely on the object representation of the ad-hoc property
                parameterValues.put(propertyName, (Serializable) JsonUtil.convertJSONValue((ValueNode) propertyValue));
            }
            
        }

        return parameterValues;
    }
    
    private Serializable convertValue(QName typeQName, JsonNode propertyValue)
    {        
        Serializable value = null;
        
        DataTypeDefinition typeDef = dictionaryService.getDataType(typeQName);
        if (typeDef == null)
        {
            throw new AlfrescoRuntimeException("Action property type definition " + typeQName.toPrefixString() + " is unknown.");
        }
        
        if (propertyValue instanceof ArrayNode)
        {
            // Convert property type to java class
            Class<?> javaClass = null;
            
            String javaClassName = typeDef.getJavaClassName();
            try
            {
                javaClass = Class.forName(javaClassName);
            }
            catch (ClassNotFoundException e)
            {
                throw new DictionaryException("Java class " + javaClassName + " of property type " + typeDef.getName() + " is invalid", e);
            }
            
            int length = propertyValue.size();
            List<Serializable> list = new ArrayList<Serializable>(length);
            for (int i = 0; i < length; i++)
            {
                list.add(convertValue(typeQName, propertyValue.get(i)));
            }
            value = (Serializable)list;
        }
        else
        {
            if (typeQName.equals(DataTypeDefinition.QNAME) == true && 
                typeQName.toString().contains(":") == true)
            {
                 value = QName.createQName(propertyValue.textValue(), namespaceService);
            }
            else
            {
                value = (Serializable)DefaultTypeConverter.INSTANCE.convert(dictionaryService.getDataType(typeQName),
                        JsonUtil.convertJSONValue((ValueNode) propertyValue));
            }
        }
        
        return value;
    }

    protected void checkRule(Rule rule)
    {
        List<String> ruleTypes = rule.getRuleTypes();
        if (ruleTypes.contains(RULE_OUTBOUND))
        {
            List<Action> actions = ((CompositeActionImpl) rule.getAction()).getActions();
            for (Action action : actions)
            {
                if (action.getActionDefinitionName().equalsIgnoreCase(ACTION_CHECK_OUT))
                {
                    throw new WebScriptException(CANNOT_CREATE_RULE);
                }
            }
        }
    }
}
