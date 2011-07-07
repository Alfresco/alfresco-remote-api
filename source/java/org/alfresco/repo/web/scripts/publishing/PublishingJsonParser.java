/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */

package org.alfresco.repo.web.scripts.publishing;

import static org.alfresco.repo.web.scripts.WebScriptUtil.getCalendar;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.CHANNEL_NAME;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.CHANNEL_NAMES;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.COMMENT;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.IDS;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.MESSAGE;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.NODE_REF;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.PUBLISH_NODES;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.SCHEDULED_TIME;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.STATUS_UPDATE;
import static org.alfresco.repo.web.scripts.publishing.PublishingWebScriptConstants.UNPUBLISH_NODES;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.node.NodeUtils;
import org.alfresco.service.cmr.publishing.Environment;
import org.alfresco.service.cmr.publishing.MutablePublishingPackage;
import org.alfresco.service.cmr.publishing.PublishingEvent;
import org.alfresco.service.cmr.publishing.PublishingEventFilter;
import org.alfresco.service.cmr.publishing.PublishingPackage;
import org.alfresco.service.cmr.publishing.PublishingQueue;
import org.alfresco.service.cmr.publishing.StatusUpdate;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.collections.Function;
import org.alfresco.util.collections.JsonUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author Nick Smith
 * @since 4.0
 *
 */
public class PublishingJsonParser
{

    public JSONObject getJson(String jsonStr) throws JSONException
    {
        if(jsonStr!=null && jsonStr.isEmpty()==false)
        {
            return new JSONObject(new JSONTokener(jsonStr));
        }
        return new JSONObject();
    }
    
    public List<PublishingEvent> query(Environment environment, String content) throws JSONException
    {
        JSONObject json = getJson(content);
        PublishingEventFilter filter = buildFilter(environment, json);
        return environment.getPublishingEvents(filter);
    }

    private PublishingEventFilter buildFilter(Environment environment, JSONObject json)
    {
        List<NodeRef> publishNodes = toNodes(json.optJSONArray(PUBLISH_NODES));
        List<NodeRef> unpublishNodes = toNodes(json.optJSONArray(UNPUBLISH_NODES));
        List<String> ids = JsonUtils.toListOfStrings(json.optJSONArray(IDS));

        PublishingEventFilter filter = environment.createPublishingEventFilter()
            .setIds(ids)
            .setPublishedNodes(publishNodes)
            .setUnpublishedNodes(unpublishNodes);
        return filter;
    }

    public String schedulePublishingEvent(PublishingQueue queue, String jsonStr) throws ParseException, JSONException
    {
        JSONObject json = getJson(jsonStr);
        String channelName = json.optString(CHANNEL_NAME);
        String comment = json.optString(COMMENT);
        Calendar schedule = getCalendar(json.optJSONObject(SCHEDULED_TIME));
        PublishingPackage publishingPackage = getPublishingPackage(queue, json);
        StatusUpdate statusUpdate = getStatusUpdate(queue, json.optJSONObject(STATUS_UPDATE));
        return queue.scheduleNewEvent(publishingPackage, channelName, schedule, comment, statusUpdate);
    }
    
    public StatusUpdate getStatusUpdate(PublishingQueue queue, JSONObject json)
    {
        if(json == null)
        {
            return null;
        }
        String message = json.optString(MESSAGE);
        NodeRef nodeToLinkTo = null;
        String nodeStr = json.optString(NODE_REF);
        if(nodeStr!=null && nodeStr.isEmpty() == false)
        {
            nodeToLinkTo = new NodeRef(nodeStr);
        }
        Collection<String> channelNames = toStrings(json.optJSONArray(CHANNEL_NAMES));
        return queue.createStatusUpdate(message, nodeToLinkTo, channelNames);
    }
    
    public PublishingPackage getPublishingPackage(PublishingQueue queue, JSONObject json)
    {
        MutablePublishingPackage pckg = queue.createPublishingPackage();
        List<NodeRef> publishNodes = toNodes(json.optJSONArray(PUBLISH_NODES));
        List<NodeRef> unpublishNodes = toNodes(json.optJSONArray(UNPUBLISH_NODES));
        pckg.addNodesToPublish(publishNodes);
        pckg.addNodesToUnpublish(unpublishNodes);
        return pckg;
    }
    
    public List<NodeRef> toNodes(JSONArray json)
    {
        Function<String, NodeRef> transformer = NodeUtils.toNodeRef();
        return JsonUtils.transform(json, transformer);
    }
    
    private List<String> toStrings(JSONArray json)
    {
        if(json == null || json.length() == 0)
        {
            return Collections.emptyList();
        }
        ArrayList<String> results = new ArrayList<String>(json.length());
        for (int i = 0; i < json.length(); i++)
        {
            results.add(json.optString(i));
        }
        return results;
    }

}
