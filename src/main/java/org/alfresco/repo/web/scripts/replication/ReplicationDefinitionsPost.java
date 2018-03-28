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
package org.alfresco.repo.web.scripts.replication;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Map;

import org.alfresco.service.cmr.replication.ReplicationDefinition;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;


/**
 * @author Nick Burch
 * @since 3.4
 */
public class ReplicationDefinitionsPost extends AbstractReplicationWebscript
{
   @Override
   protected Map<String, Object> buildModel(ReplicationModelBuilder modelBuilder, 
                                            WebScriptRequest req, Status status, Cache cache)
   {
       // Create our definition
       ReplicationDefinition replicationDefinition = null;
       try 
       {
           JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
           
           // Check for the required parameters
           if(! json.has("name"))
              throw new WebScriptException(Status.STATUS_BAD_REQUEST, "name is required but wasn't supplied");
           if(! json.has("description"))
              throw new WebScriptException(Status.STATUS_BAD_REQUEST, "description is required but wasn't supplied");
           
           // Ensure one doesn't already exist with that name
           String name = json.get("name").textValue();
           if(replicationService.loadReplicationDefinition(name) != null)
           {
              throw new WebScriptException(Status.STATUS_BAD_REQUEST, "A replication definition already exists with that name");
           }
           
           // Create the definition
           replicationDefinition = replicationService.createReplicationDefinition(
                 name, json.get("description").textValue()
           );
           
           // Set the extra parts
           updateDefinitionProperties(replicationDefinition, json);
           
           // Save the changes
           replicationService.saveReplicationDefinition(replicationDefinition);
       }
       catch (IOException iox)
       {
           throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from request.", iox);
       }
      
       // Return the details on it
       return modelBuilder.buildDetails(replicationDefinition);
   }
}