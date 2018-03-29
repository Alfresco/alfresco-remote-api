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
package org.alfresco.repo.web.scripts.comments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.activities.ActivityService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * This class is the abstract controller for the comments web scripts (delete
 * and post)
 * 
 * @author Ramona Popa
 * @since 4.2.6
 */
public abstract class AbstractCommentsWebScript extends DeclarativeWebScript
{

    protected final static String COMMENTS_TOPIC_NAME = "Comments";

    private static Log logger = LogFactory.getLog(CommentsPost.class);

    protected final static String JSON_KEY_SITE = "site";
    protected final static String JSON_KEY_SITE_ID = "siteid";
    protected final static String JSON_KEY_ITEM_TITLE = "itemTitle";
    protected final static String JSON_KEY_PAGE = "page";
    protected final static String JSON_KEY_TITLE = "title";
    protected final static String JSON_KEY_PAGE_PARAMS = "pageParams";
    protected final static String JSON_KEY_NODEREF = "nodeRef";
    protected final static String JSON_KEY_CONTENT = "content";

    protected final static String COMMENT_CREATED_ACTIVITY = "org.alfresco.comments.comment-created";
    protected final static String COMMENT_DELETED_ACTIVITY = "org.alfresco.comments.comment-deleted";

    protected ServiceRegistry serviceRegistry;
    protected NodeService nodeService;
    protected ContentService contentService;
    protected PersonService personService;
    protected SiteService siteService;
    protected PermissionService permissionService;
    protected ActivityService activityService;

    protected BehaviourFilter behaviourFilter;

    protected static final String PARAM_MESSAGE = "message";
    protected static final String PARAM_NODE = "node";
    protected static final String PARAM_ITEM = "item";

    public void setServiceRegistry(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
        this.nodeService = serviceRegistry.getNodeService();
        this.siteService = serviceRegistry.getSiteService();
        this.contentService = serviceRegistry.getContentService();
        this.personService = serviceRegistry.getPersonService();
        this.permissionService = serviceRegistry.getPermissionService();
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    public void setActivityService(ActivityService activityService)
    {
        this.activityService = activityService;
    }

    /**
     * returns the nodeRef from  web script request
     */
    protected NodeRef parseRequestForNodeRef(WebScriptRequest req)
    {
        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        String storeType = templateVars.get("store_type");
        String storeId = templateVars.get("store_id");
        String nodeId = templateVars.get("id");

        // create the NodeRef and ensure it is valid
        StoreRef storeRef = new StoreRef(storeType, storeId);
        return new NodeRef(storeRef, nodeId);
    }

    /**
     * get the value from JSON for given key if exists
     */
    protected String getOrNull(JsonNode json, String key)
    {
        if (json != null && json.has(key))
        {
            return json.get(key).textValue();
        }
        return null;
    }

    /**
     * parse JSON from request
     */
    protected JsonNode parseJSON(WebScriptRequest req)
    {
        JsonNode json = null;
        String contentType = req.getContentType();
        if (contentType != null && contentType.indexOf(';') != -1)
        {
            contentType = contentType.substring(0, contentType.indexOf(';'));
        }
        if (MimetypeMap.MIMETYPE_JSON.equals(contentType))
        {
            try
            {
                json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
            }
            catch (IOException io)
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: " + io.getMessage());
            }
        }
        return json;
    }

    /**
     * parse JSON for a given input string
     */
    protected JsonNode parseJSONFromString(String input)
    {
        try
        {
            if (input != null)
            {
                JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(input);
                return json;
            }
        }
        catch (IOException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Invalid JSON: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Post an activity entry for the comment added or deleted
     * 
     * @param json - is not sent null with this activity type - only for delete
     */
    protected void postActivity(JsonNode json, WebScriptRequest req, NodeRef nodeRef, String activityType)
    {
        String jsonActivityData = "";
        String siteId = "";
        String page = "";
        String title = "";

        if (nodeRef == null)
        {
            // in case we don't have an parent nodeRef provided we do not need
            // to post activity for parent node
            return;
        }

        String strNodeRef = nodeRef.toString();

        SiteInfo siteInfo = getSiteInfo(req, COMMENT_CREATED_ACTIVITY.equals(activityType));

        // post an activity item, but only if we've got a site
        if (siteInfo == null || siteInfo.getShortName() == null || siteInfo.getShortName().length() == 0)
        {
            return;
        }
        else
        {
            siteId = siteInfo.getShortName();
        }

        // json is not sent null with this activity type - only for delete
        if (COMMENT_CREATED_ACTIVITY.equals(activityType))
        {
            try
            {
                if (json.has(JSON_KEY_PAGE_PARAMS))
                {
                    JsonNode params = json.get(JSON_KEY_PAGE_PARAMS);
                    String strParams = "";

                    Iterator<String> itr = params.fieldNames();
                    while (itr.hasNext())
                    {
                        String strParam = itr.next();
                        strParams += strParam + "=" + params.get(strParam).textValue() + "&";
                    }
                    page = getOrNull(json, JSON_KEY_PAGE) + "?" + (strParams != "" ? strParams.substring(0, strParams.length() - 1) : "");
                    title = getOrNull(json, JSON_KEY_ITEM_TITLE);
                }
            }
            catch (Exception e)
            {
                logger.warn("Error parsing JSON", e);
            }
        }
        else
        {
            // COMMENT_DELETED_ACTIVITY
            title = req.getParameter(JSON_KEY_ITEM_TITLE);
            page = req.getParameter(JSON_KEY_PAGE) + "?" + JSON_KEY_NODEREF + "=" + strNodeRef;
        }

        try
        {
            ObjectNode objectNode = AlfrescoDefaultObjectMapper.createObjectNode();
            objectNode.put(JSON_KEY_TITLE, title);
            objectNode.put(JSON_KEY_PAGE, page);
            objectNode.put(JSON_KEY_NODEREF, strNodeRef);

            jsonActivityData = AlfrescoDefaultObjectMapper.writeValueAsString(objectNode);
            activityService.postActivity(activityType, siteId, COMMENTS_TOPIC_NAME, jsonActivityData);
        }
        catch (Exception e)
        {
            logger.warn("Error adding comment to activities feed", e);
        }

    }

    /**
     * returns SiteInfo needed for post activity
     */
    protected SiteInfo getSiteInfo(WebScriptRequest req, boolean searchForSiteInJSON)
    {
        String siteName = req.getParameter(JSON_KEY_SITE);

        if (siteName == null && searchForSiteInJSON )
        {
            JsonNode json = parseJSON(req);
            if (json != null){
                if (json.has(JSON_KEY_SITE))
                {
                    siteName = json.get(JSON_KEY_SITE).textValue();
                }
                else if (json.has(JSON_KEY_SITE_ID))
                {
                    siteName = json.get(JSON_KEY_SITE_ID).textValue();
                }
            }
        }
        if (siteName != null)
        {
            SiteInfo site = siteService.getSite(siteName);
            return site;
        }

        return null;
    }

    /**
     * Overrides DeclarativeWebScript with parse request for nodeRef 
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        // get requested node
        NodeRef nodeRef = parseRequestForNodeRef(req);

        // Have the real work done
        return executeImpl(nodeRef, req, status, cache);
    }

    protected abstract Map<String, Object> executeImpl(NodeRef nodeRef, WebScriptRequest req, Status status, Cache cache);

}
