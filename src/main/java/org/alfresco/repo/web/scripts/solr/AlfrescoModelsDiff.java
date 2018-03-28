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
package org.alfresco.repo.web.scripts.solr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.solr.AlfrescoModelDiff;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Support for SOLR: Track Alfresco model changes
 *
 * @since 4.0
 */
public class AlfrescoModelsDiff extends DeclarativeWebScript
{
    protected static final Log logger = LogFactory.getLog(AlfrescoModelsDiff.class);

    private static final String MSG_IO_EXCEPTION = "IO exception parsing request ";

    private static final String MSG_JSON_EXCEPTION = "Unable to fetch model changes from ";

    private SOLRTrackingComponent solrTrackingComponent;
    
    public void setSolrTrackingComponent(SOLRTrackingComponent solrTrackingComponent)
    {
        this.solrTrackingComponent = solrTrackingComponent;
    }

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status)
    {
        try
        {
            Map<String, Object> model = buildModel(req);
            if (logger.isDebugEnabled())
            {
                logger.debug("Result: \n\tRequest: " + req + "\n\tModel: " + model);
            }
            return model;
        }
        catch (IOException e)
        {
            setExceptionResponse(req, status, MSG_IO_EXCEPTION, Status.STATUS_INTERNAL_SERVER_ERROR, e);
            return null;
        }
    }

    private void setExceptionResponse(WebScriptRequest req, Status responseStatus, String responseMessage, int statusCode, Exception e)
    {
        String message = responseMessage + req;

        if (logger.isDebugEnabled())
        {
            logger.warn(message, e);
        }
        else
        {
            logger.warn(message);
        }

        responseStatus.setCode(statusCode, message);
        responseStatus.setException(e);
    }

    private Map<String, Object> buildModel(WebScriptRequest req) throws IOException
    {
        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);

        Content content = req.getContent();
        if(content == null)
        {
            throw new WebScriptException("Failed to convert request to String");
        }
        JsonNode o = AlfrescoDefaultObjectMapper.getReader().readTree(content.getContent());
        ArrayNode jsonModels = (ArrayNode) o.get("models");
        Map<QName, Long> models = new HashMap<QName, Long>(jsonModels.size());
        for(int i = 0; i < jsonModels.size(); i++)
        {
            JsonNode jsonModel = jsonModels.get(i);
            models.put(QName.createQName(jsonModel.get("name").textValue()), jsonModel.get("checksum").longValue());
        }

        List<AlfrescoModelDiff> diffs = solrTrackingComponent.getModelDiffs(models);
        model.put("diffs", diffs);

        if (logger.isDebugEnabled())
        {
            logger.debug("Result: \n\tRequest: " + req + "\n\tModel: " + model);
        }
        
        return model;
    }
}
