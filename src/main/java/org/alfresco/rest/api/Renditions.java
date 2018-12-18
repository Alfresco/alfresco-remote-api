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

package org.alfresco.rest.api;

import org.alfresco.rest.api.model.Rendition;
import org.alfresco.rest.framework.core.exceptions.ConstraintViolatedException;
import org.alfresco.rest.framework.core.exceptions.NotFoundException;
import org.alfresco.rest.framework.resource.content.BinaryResource;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

/**
 * Renditions API
 *
 * @author Jamal Kaabi-Mofrad
 */
public interface Renditions
{
    String PARAM_STATUS = "status";

    /**
     * Lists all available renditions includes those that have been created and those that are yet to be created.
     *
     * @param nodeRef
     * @param parameters the {@link Parameters} object to get the parameters passed into the request
     * @return the rendition results
     */
    CollectionWithPagingInfo<Rendition> getRenditions(NodeRef nodeRef, Parameters parameters);

    /**
     * Gets information about a rendition of a node in the repository.
     * If there is no rendition, then returns the available/registered rendition.
     *
     * @param nodeRef
     * @param renditionId the rendition id
     * @param parameters  the {@link Parameters} object to get the parameters passed into the request
     * @return the {@link Rendition} object
     */
    Rendition getRendition(NodeRef nodeRef, String renditionId, Parameters parameters);

    /**
     * Creates a rendition for the given node asynchronously.
     *
     * @param nodeRef
     * @param rendition  the {@link Rendition} request
     * @param parameters the {@link Parameters} object to get the parameters passed into the request
     */
    void createRendition(NodeRef nodeRef, Rendition rendition, Parameters parameters);

    /**
     * Creates a rendition for the given node - either async r sync
     *
     * @param nodeRef
     * @param rendition
     * @param executeAsync
     * @param parameters
     */
    void createRendition(NodeRef nodeRef, Rendition rendition, boolean executeAsync, Parameters parameters);

    /**
     * Creates renditions that don't already exist for the given node asynchronously.
     *
     * @param nodeRef
     * @param renditions the {@link Rendition} request
     * @param parameters the {@link Parameters} object to get the parameters passed into the request
     * @throws NotFoundException if any of the rendition id do not exist.
     * @throws ConstraintViolatedException if all of the renditions already exist.
     */
    void createRenditions(NodeRef nodeRef, List<Rendition> renditions, Parameters parameters)
            throws NotFoundException, ConstraintViolatedException;

    /**
     * Downloads rendition.
     *
     * @param sourceNodeRef the source nodeRef
     * @param renditionId   the rendition id
     * @param parameters    the {@link Parameters} object to get the parameters passed into the request
     * @return the rendition stream
     */
    BinaryResource getContent(NodeRef sourceNodeRef, String renditionId, Parameters parameters);

    /**
     * Downloads rendition.
     *
     * @param sourceNodeRef the source nodeRef
     * @param renditionId   the rendition id
     * @param parameters    the {@link Parameters} object to get the parameters passed into the request
     * @return the rendition stream
     */
    BinaryResource getContentNoValidation(NodeRef sourceNodeRef, String renditionId, Parameters parameters);
}

