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
package org.alfresco.repo.web.scripts.download;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Web script for creating a new download.
 *
 * @author Alex Miller
 */
public class DownloadPost extends AbstractDownloadWebscript
{
    @Override
    protected Map<String,Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
        if (templateVars == null)
        {
            String error = "No parameters supplied";
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, error);
        }
        
        
        // Parse the JSON, if supplied
        String contentType = req.getContentType();
        if (contentType != null && contentType.indexOf(';') != -1)
        {
           contentType = contentType.substring(0, contentType.indexOf(';'));
        }
        
        List<NodeRef> nodes = new LinkedList<NodeRef>();
        if (MimetypeMap.MIMETYPE_JSON.equals(contentType))
        {
           try
           {
               ArrayNode json = (ArrayNode) AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
              for (int i = 0 ; i < json.size() ; i++)
              {
                JsonNode obj = json.get(i);
                String nodeRefString = obj.get("nodeRef").textValue();
                if (nodeRefString != null) 
                {
                    nodes.add(new NodeRef(nodeRefString));
                }
              }
           }
           catch (IOException io)
           {
               throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Unexpected IOException", io);
           }
        }
        
        if (nodes.size() <= 0) 
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRefs provided");
        }
        
        NodeRef downloadNode = downloadService.createDownload(nodes.toArray(new NodeRef[nodes.size()]), true);
        
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("downloadNodeRef", downloadNode);

        return model;
    }

}
