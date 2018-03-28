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
package org.alfresco.repo.web.scripts.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * REST API - create tenant
 * 
 * @author janv
 * @since 4.2
 */
public class TenantPost extends AbstractTenantAdminWebScript
{
    protected static final Log logger = LogFactory.getLog(TenantPost.class);
    
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache)
    {
        String tenantDomain = null;
        String tenantAdminPassword = null;
        String contentStoreRoot = null;
        
        try
        {
            JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
            
            if (! json.has(TENANT_DOMAIN))
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not find required 'tenantDomain' parameter");
            }
            tenantDomain = json.get(TENANT_DOMAIN).textValue();
            
            if (! json.has(TENANT_ADMIN_PASSWORD))
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not find required 'tenantAdminPassword' parameter");
            }
            tenantAdminPassword = json.get(TENANT_ADMIN_PASSWORD).textValue();
            
            if (json.has(TENANT_CONTENT_STORE_ROOT))
            {
                contentStoreRoot = json.get(TENANT_CONTENT_STORE_ROOT).textValue();
            }
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from req.", iox);
        }
        
        tenantAdminService.createTenant(tenantDomain, tenantAdminPassword.toCharArray(), contentStoreRoot);
        
        Map<String, Object> model = new HashMap<String, Object>(0);
        return model;
    }
}
