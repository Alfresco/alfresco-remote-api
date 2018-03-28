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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.search.impl.solr.facet.SolrFacetModel;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetProperties.CustomProperties;
import org.alfresco.repo.search.impl.solr.facet.SolrFacetService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.json.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * This class is an abstract base class for the various web script controllers
 * in the SolrFacetService.
 * 
 * @author Jamal Kaabi-Mofrad
 */
public abstract class AbstractSolrFacetConfigAdminWebScript extends DeclarativeWebScript
{
    private static final Log logger = LogFactory.getLog(AbstractSolrFacetConfigAdminWebScript.class);
    
    protected static final String PARAM_FILTER_ID = "filterID";
    protected static final String PARAM_FACET_QNAME = "facetQName";
    protected static final String PARAM_DISPLAY_NAME = "displayName";
    protected static final String PARAM_DISPLAY_CONTROL = "displayControl";
    protected static final String PARAM_MAX_FILTERS = "maxFilters";
    protected static final String PARAM_HIT_THRESHOLD = "hitThreshold";
    protected static final String PARAM_MIN_FILTER_VALUE_LENGTH = "minFilterValueLength";
    protected static final String PARAM_SORT_BY = "sortBy";
    protected static final String PARAM_SCOPE = "scope";
    protected static final String PARAM_SCOPED_SITES = "scopedSites";
    protected static final String PARAM_INDEX = "index";
    protected static final String PARAM_IS_ENABLED = "isEnabled";
    protected static final String PARAM_CUSTOM_PROPERTIES = "customProperties";
    protected static final String CUSTOM_PARAM_NAME = "name";
    protected static final String CUSTOM_PARAM_VALUE = "value";

    // The pattern is equivalent to the pattern defined in the forms-runtime.js
    protected static final Pattern FILTER_ID_PATTERN = Pattern.compile("([\"\\*\\\\\\>\\<\\?\\/\\:\\|]+)|([\\.]?[\\.]+$)");

    protected SolrFacetService facetService;
    protected NamespaceService namespaceService;

    /**
     * @param facetService the facetService to set
     */
    public void setFacetService(SolrFacetService facetService)
    {
        this.facetService = facetService;
    }

    /**
     * @param namespaceService the namespaceService to set
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status, final Cache cache)
    {
        validateCurrentUser();

        return unprotectedExecuteImpl(req, status, cache);
    }

    protected void validateCurrentUser()
    {
        String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
        // check the current user access rights
        if (!facetService.isSearchAdmin(currentUser))
        {
            throw new WebScriptException(HttpServletResponse.SC_FORBIDDEN, "Access denied.");
        }
    }

    protected <T> T getValue(Class<T> clazz, JsonNode value, T defaultValue) throws IOException
    {
        if (value == null || value instanceof NullNode)
        {
            return defaultValue;
        }

        try
        {
            // convert value to Java object before trying to cast
            return clazz.cast(JsonUtil.convertJSONValue((ValueNode) value));
        }
        catch (Exception ex)
        {
            throw new IOException("JSONObject[" + value +"] is not an instance of [" + clazz.getName() +"]");
        }
    }

    protected Set<CustomProperties> getCustomProperties(ObjectNode customPropsJsonObj) throws IOException
    {
        if (customPropsJsonObj == null)
        {
            return null;
        }
        Iterator<String> keys = customPropsJsonObj.fieldNames();
        if (keys == null)
        {
            return Collections.emptySet();
        }

        Set<CustomProperties> customProps = new HashSet<>(customPropsJsonObj.size());
        while(keys.hasNext())
        {
            String key = keys.next();
            JsonNode jsonObj = customPropsJsonObj.get(key);

            QName name = resolveToQName(getValue(String.class, (ValueNode) jsonObj.get(CUSTOM_PARAM_NAME), null));
            validateMandatoryCustomProps(name, CUSTOM_PARAM_NAME);
            
            Serializable value = null;
            JsonNode     customPropValue = jsonObj.has(CUSTOM_PARAM_VALUE) ? jsonObj.get(CUSTOM_PARAM_VALUE) : null;
            validateMandatoryCustomProps(customPropValue, CUSTOM_PARAM_VALUE);
            
            if(customPropValue instanceof ArrayNode)
            {
                ArrayNode array = (ArrayNode) customPropValue;
                ArrayList<Serializable> list = new ArrayList<>(array.size());
                for(int j = 0; j < array.size(); j++)
                {
                    list.add(getSerializableValue(array.get(j)));
                }
                value = list;
            }
            else
            {
                value = getSerializableValue(customPropValue);
            }
            
           customProps.add(new CustomProperties(name, value));
        }

        if (logger.isDebugEnabled() && customProps.size() > 0)
        {
            logger.debug("Processed custom properties:" + customProps);
        }

        return customProps;
    }

    protected Set<String> getScopedSites(ArrayNode scopedSitesJsonArray)
    {
        if (scopedSitesJsonArray == null)
        {
            return null;
        }

        Set<String> scopedSites = new HashSet<String>(scopedSitesJsonArray.size());
        for (int i = 0, length = scopedSitesJsonArray.size(); i < length; i++)
        {
            String site = scopedSitesJsonArray.get(i).textValue();
            scopedSites.add(site);
        }
        return scopedSites;
    }

    private void validateMandatoryCustomProps(Object obj, String paramName) throws IOException
    {
        if (obj == null)
        {
            throw new IOException("Invalid JSONObject in the Custom Properties JSON. [" + paramName + "] cannot be null.");
        }

    }

    protected void validateFilterID(String filterID)
    {
        Matcher matcher = FILTER_ID_PATTERN.matcher(filterID);
        if (matcher.find())
        {
            throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST,
                        "Invalid Filter Id. The characters \" * \\ < > ? / : | are not allowed. The Filter Id cannot end with a dot.");
        }
    }

    private Serializable getSerializableValue(JsonNode node) throws IOException
    {
        if (!(node instanceof ValueNode))
        {
            throw new IOException("Invalid value in the Custom Properties JSON. [" + node.toString() + "] must be an instance of Serializable.");
        }
        Object object = JsonUtil.convertJSONValue((ValueNode) node);
        return (Serializable) object;
    }

    private QName resolveToQName(String qnameStr) throws IOException
    {
        QName typeQName = null;
        if (qnameStr == null)
        {
            return typeQName;
        }
        if(qnameStr.charAt(0) == QName.NAMESPACE_BEGIN && qnameStr.indexOf("solrfacetcustomproperty") < 0)
        {
            throw new IOException("Invalid name in the Custom Properties JSON. Namespace URL must be [" + SolrFacetModel.SOLR_FACET_CUSTOM_PROPERTY_URL + "]");
        }
        else if(qnameStr.charAt(0) == QName.NAMESPACE_BEGIN)
        {
            typeQName = QName.createQName(qnameStr);
        }
        else
        {
            typeQName = QName.createQName(SolrFacetModel.SOLR_FACET_CUSTOM_PROPERTY_URL, qnameStr);
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Resolved facet's custom property name [" + qnameStr + "] into [" + typeQName + "]");
        }
        return typeQName;
    }
    
    /**
     * Retrieves the named parameter as an integer, if the parameter is not present the default value is returned.
     * 
     * @param req The WebScript request
     * @param paramName The name of parameter to look for.
     * @param defaultValue The default value that should be returned if parameter is not present in request or is negative.
     * @return The request parameter or default value
     * @throws WebScriptException if the named parameter cannot be converted to int (HTTP rsp 400).
     */
    protected int getNonNegativeIntParameter(WebScriptRequest req, String paramName, int defaultValue)
    {
        final String paramString = req.getParameter(paramName);
        
        final int result;
        
        if (paramString != null)
        {
            try
            {
                final int paramInt = Integer.valueOf(paramString);
                
                if   (paramInt < 0) { result = defaultValue; }
                else                { result = paramInt; }
            }
            catch (NumberFormatException e) 
            {
                throw new WebScriptException(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }
        else { result = defaultValue; }
        
        return result;
    }
    
    abstract protected Map<String, Object> unprotectedExecuteImpl(WebScriptRequest req, Status status, Cache cache);
}
