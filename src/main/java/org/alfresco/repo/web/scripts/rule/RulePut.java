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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.activiti.engine.impl.util.json.JSONException;
import org.alfresco.repo.action.ActionConditionImpl;
import org.alfresco.repo.action.ActionImpl;
import org.alfresco.repo.action.CompositeActionImpl;
import org.alfresco.repo.web.scripts.rule.ruleset.RuleRef;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionCondition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.rule.Rule;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author unknown
 *
 */
public class RulePut extends RulePost
{
    @SuppressWarnings("unused")
    private static Log logger = LogFactory.getLog(RulePut.class);

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        Map<String, Object> model = new HashMap<String, Object>();

        // get request parameters
        NodeRef nodeRef = parseRequestForNodeRef(req);

        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        String ruleId = templateVars.get("rule_id");

        Rule ruleToUpdate = null;

        // get all rules for given nodeRef
        List<Rule> rules = ruleService.getRules(nodeRef);

        //filter by rule id
        for (Rule rule : rules)
        {
            if (rule.getNodeRef().getId().equalsIgnoreCase(ruleId))
            {
                ruleToUpdate = rule;
                break;
            }
        }

        if (ruleToUpdate == null)
        {
            throw new WebScriptException(HttpServletResponse.SC_NOT_FOUND, "Unable to find rule with id: " + ruleId);
        }

        try
        {
            // read request json
            JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());

            // parse request json
            updateRuleFromJSON(json, ruleToUpdate);

            // check the rule
            checkRule(ruleToUpdate);

            // save changes
            ruleService.saveRule(nodeRef, ruleToUpdate);

            RuleRef updatedRuleRef = new RuleRef(ruleToUpdate, fileFolderService.getFileInfo(ruleService.getOwningNodeRef(ruleToUpdate)));

            model.put("ruleRef", updatedRuleRef);
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from req.", iox);
        }

        return model;
    }

    protected void updateRuleFromJSON(JsonNode jsonRule, Rule ruleToUpdate) throws JSONException
    {
        if (jsonRule.has("title"))
        {
            ruleToUpdate.setTitle(jsonRule.get("title").textValue());
        }

        if (jsonRule.has("description"))
        {
            ruleToUpdate.setDescription(jsonRule.get("description").textValue());
        }

        if (jsonRule.has("ruleType"))
        {
            ArrayNode jsonTypes = (ArrayNode) jsonRule.get("ruleType");
            List<String> types = new ArrayList<String>();

            for (int i = 0; i < jsonTypes.size(); i++)
            {
                types.add(jsonTypes.get(i).textValue());
            }
            ruleToUpdate.setRuleTypes(types);
        }

        if (jsonRule.has("applyToChildren"))
        {
            ruleToUpdate.applyToChildren(jsonRule.get("applyToChildren").booleanValue());
        }

        if (jsonRule.has("executeAsynchronously"))
        {
            ruleToUpdate.setExecuteAsynchronously(jsonRule.get("executeAsynchronously").booleanValue());
        }

        if (jsonRule.has("disabled"))
        {
            ruleToUpdate.setRuleDisabled(jsonRule.get("disabled").booleanValue());
        }

        if (jsonRule.has("action"))
        {
            JsonNode jsonAction = jsonRule.get("action");

            // update rule action 
            Action action = updateActionFromJson(jsonAction, (ActionImpl) ruleToUpdate.getAction());

            ruleToUpdate.setAction(action);
        }
    }

    protected Action updateActionFromJson(JsonNode jsonAction, ActionImpl actionToUpdate) throws JSONException
    {
        ActionImpl result = null;

        if (jsonAction.has("id"))
        {
            // update existing action
            result = actionToUpdate;
        }
        else
        {
            // create new object as id was not sent by client
            result = parseJsonAction(jsonAction);
            return result;
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
            if (jsonActions.size() == 0)
            {
                // empty array was sent -> clear list
                ((CompositeActionImpl) result).getActions().clear();
            }
            else
            {
                List<Action> existingActions = ((CompositeActionImpl) result).getActions();
                List<Action> newActions = new ArrayList<Action>();

                for (int i = 0; i < jsonActions.size(); i++)
                {
                    JsonNode innerJsonAction = jsonActions.get(i);

                    if (innerJsonAction.has("id"))
                    {
                        // update existing object
                        String actionId = innerJsonAction.get("id").textValue();

                        Action existingAction = getAction(existingActions, actionId);
                        existingActions.remove(existingAction);

                        Action updatedAction = updateActionFromJson(innerJsonAction, (ActionImpl) existingAction);
                        newActions.add(updatedAction);
                    }
                    else
                    {
                        //create new action as id was not sent
                        newActions.add(parseJsonAction(innerJsonAction));
                    }
                }
                existingActions.clear();

                for (Action action : newActions)
                {
                    existingActions.add(action);
                }
            }
        }

        if (jsonAction.has("conditions"))
        {
            ArrayNode jsonConditions = (ArrayNode) jsonAction.get("conditions");

            if (jsonConditions.size() == 0)
            {
                // empty array was sent -> clear list
                result.getActionConditions().clear();
            }
            else
            {
                List<ActionCondition> existingConditions = result.getActionConditions();
                List<ActionCondition> newConditions = new ArrayList<ActionCondition>();

                for (int i = 0; i < jsonConditions.size(); i++)
                {
                    JsonNode jsonCondition = jsonConditions.get(i);

                    if (jsonCondition.has("id"))
                    {
                        // update existing object
                        String conditionId = jsonCondition.get("id").textValue();

                        ActionCondition existingCondition = getCondition(existingConditions, conditionId);
                        existingConditions.remove(existingCondition);

                        ActionCondition updatedActionCondition = updateActionConditionFromJson(jsonCondition, (ActionConditionImpl) existingCondition);
                        newConditions.add(updatedActionCondition);
                    }
                    else
                    {
                        // create new object as id was not sent
                        newConditions.add(parseJsonActionCondition(jsonCondition));
                    }
                }

                existingConditions.clear();

                for (ActionCondition condition : newConditions)
                {
                    existingConditions.add(condition);
                }
            }
        }

        if (jsonAction.has("compensatingAction"))
        {
            JsonNode jsonCompensatingAction = jsonAction.get("compensatingAction");
            Action compensatingAction = updateActionFromJson(jsonCompensatingAction, (ActionImpl) actionToUpdate.getCompensatingAction());

            actionToUpdate.setCompensatingAction(compensatingAction);
        }
        return result;
    }

    protected ActionCondition updateActionConditionFromJson(JsonNode jsonCondition, ActionConditionImpl conditionToUpdate) throws JSONException
    {
        ActionConditionImpl result = null;

        if (jsonCondition.has("id"))
        {
            // update exiting object
            result = conditionToUpdate;
        }
        else
        {
            // create new onject as id was not sent
            result = parseJsonActionCondition(jsonCondition);
            return result;
        }

        if (jsonCondition.has("invertCondition"))
        {
            result.setInvertCondition(jsonCondition.get("invertCondition").booleanValue());
        }

        if (jsonCondition.has("parameterValues"))
        {
            JsonNode jsonParameterValues = jsonCondition.get("parameterValues");
            result.setParameterValues(parseJsonParameterValues(jsonParameterValues, result.getActionConditionDefinitionName(), false));
        }

        return result;
    }

    private Action getAction(List<Action> actions, String id)
    {
        Action result = null;
        for (Action action : actions)
        {
            if (action.getId().equalsIgnoreCase(id))
            {
                result = action;
                break;
            }
        }

        return result;
    }

    private ActionCondition getCondition(List<ActionCondition> conditions, String id)
    {
        ActionCondition result = null;
        for (ActionCondition condition : conditions)
        {
            if (condition.getId().equalsIgnoreCase(id))
            {
                result = condition;
                break;
            }
        }

        return result;
    }
}
