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

package org.alfresco.repo.web.scripts.facet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.repo.search.impl.solr.facet.FacetQNameUtils;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetProperties;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetProperties.CustomProperties;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * This class is the controller for the "solr-facet-config-admin.post" web scripts.
 * 
 * @author Jamal Kaabi-Mofrad
 */
public class SolrFacetConfigAdminPost extends AbstractSolrFacetConfigAdminWebScript
{
    private static final Log logger = LogFactory.getLog(SolrFacetConfigAdminPost.class);

    @Override
    protected Map<String, Object> unprotectedExecuteImpl(WebScriptRequest req, Status status, Cache cache)
    {
        try
        {
            SolrFacetProperties fp = parseRequestForFacetProperties(req);
            facetService.createFacetNode(fp);

            if (logger.isDebugEnabled())
            {
                logger.debug("Created facet node: " + fp);
            }
        }
        catch (Throwable t)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not save the facet configuration.", t);
        }

        return new HashMap<>(); // Needs to be mutable.
    }

    private SolrFacetProperties parseRequestForFacetProperties(WebScriptRequest req)
    {
        try
        {
            JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());

            final String filterID = json.get(PARAM_FILTER_ID).textValue();
            validateFilterID(filterID);

            final String facetQNameStr = json.get(PARAM_FACET_QNAME).textValue();
            // Note: we're using this util class here because we need to be able to deal with
            //       qnames without a URI e.g. "{}SITE" and *not* have them default to the cm: namespace
            //       which happens with the 'normal' Alfresco QName code.
            final QName facetQName = FacetQNameUtils.createQName(facetQNameStr, namespaceService);
            final String displayName = json.get(PARAM_DISPLAY_NAME).textValue();
            final String displayControl = json.get(PARAM_DISPLAY_CONTROL).textValue();
            final int maxFilters = json.get(PARAM_MAX_FILTERS).intValue();
            final int hitThreshold = json.get(PARAM_HIT_THRESHOLD).intValue();
            final int minFilterValueLength = json.get(PARAM_MIN_FILTER_VALUE_LENGTH).intValue();
            final String sortBy = json.get(PARAM_SORT_BY).textValue();
            // Optional params
            final String scope = getValue(String.class, json.get(PARAM_SCOPE), "ALL");
            final boolean isEnabled = getValue(Boolean.class, json.get(PARAM_IS_ENABLED), false);
            ArrayNode scopedSitesJsonArray = json.has(PARAM_SCOPED_SITES) ?
                    (ArrayNode) json.get(PARAM_SCOPED_SITES) : null;
            final Set<String> scopedSites = getScopedSites(scopedSitesJsonArray);
            final ObjectNode customPropJsonObj = json.has(PARAM_CUSTOM_PROPERTIES) ?
                    (ObjectNode) json.get(PARAM_CUSTOM_PROPERTIES) : null;
            final Set<CustomProperties> customProps = getCustomProperties(customPropJsonObj);

            SolrFacetProperties fp = new SolrFacetProperties.Builder()
                        .filterID(filterID)
                        .facetQName(facetQName)
                        .displayName(displayName)
                        .displayControl(displayControl)
                        .maxFilters(maxFilters)
                        .hitThreshold(hitThreshold)
                        .minFilterValueLength(minFilterValueLength)
                        .sortBy(sortBy)
                        .scope(scope)
                        .isEnabled(isEnabled)
                        .scopedSites(scopedSites)
                        .customProperties(customProps).build();
            return fp;
        }
        catch (IOException e)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from req.", e);
        }
    }
}
