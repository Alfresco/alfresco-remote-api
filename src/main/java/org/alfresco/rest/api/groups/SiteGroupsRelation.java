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
package org.alfresco.rest.api.groups;

import org.alfresco.rest.api.Sites;
import org.alfresco.rest.api.model.GroupMemberOfSite;
import org.alfresco.rest.api.sites.SiteEntityResource;
import org.alfresco.rest.framework.WebApiDescription;
import org.alfresco.rest.framework.resource.RelationshipResource;
import org.alfresco.rest.framework.resource.actions.interfaces.RelationshipResourceAction;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.stream.Collectors;

// handles the group operation
@RelationshipResource(name = "group-members", entityResource = SiteEntityResource.class, title = "Site Groups")
public class SiteGroupsRelation implements RelationshipResourceAction.Read<GroupMemberOfSite>,
        RelationshipResourceAction.Delete,
        RelationshipResourceAction.Create<GroupMemberOfSite>,
        RelationshipResourceAction.Update<GroupMemberOfSite>,
        RelationshipResourceAction.ReadById<GroupMemberOfSite>,
        InitializingBean {

    private static final Log logger = LogFactory.getLog(SiteGroupsRelation.class);
    private Sites sites;

    public void setSites(Sites sites) {
        this.sites = sites;
    }

    @Override
    public void afterPropertiesSet() {
        ParameterCheck.mandatory("sites", this.sites);
    }

    /**
     * Returns a paged list of all the groups of the site 'siteId'.
     * <p>
     * If siteId does not exist, throws NotFoundException (status 404).
     */
    @Override
    @WebApiDescription(title = "A paged list of all the groups of the site 'siteId'.")
    public CollectionWithPagingInfo<GroupMemberOfSite> readAll(String siteId, Parameters parameters) {
        return sites.getGroups(siteId, parameters);
    }

    /**
     * POST sites/<siteId>/group-members
     * <p>
     * Adds groups to site
     * <p>
     * If group does not exist throws NotFoundException (status 404).
     *
     * @see RelationshipResourceAction.Create#create(String, List, Parameters)
     */
    @Override
    @WebApiDescription(title = "Adds personId as a member of site siteId.")
    public List<GroupMemberOfSite> create(String siteId, List<GroupMemberOfSite> siteMembers, Parameters parameters) {
        return siteMembers.stream().map((group) -> sites.addGroup(siteId, group)).collect(Collectors.toList());
    }

    /**
     * DELETE sites/<siteId>/group-members/<groupId>
     * <p>
     * Remove a group from site.
     */
    @Override
    @WebApiDescription(title = "Removes groupId as a member of site siteId.")
    public void delete(String siteId, String groupId, Parameters parameters) {
        sites.deleteGroup(groupId, siteId);
    }

    /**
     * PUT sites/<siteId>/group-members/<groupId>
     * <p>
     * Updates the membership of group in the site.
     */
    @Override
    @WebApiDescription(title = "Updates the membership of groupId in the site.")
    public GroupMemberOfSite update(String siteId, GroupMemberOfSite groupMember, Parameters parameters) {
        return sites.updateGroup(siteId, groupMember);
    }

    /**
     * Returns site membership information for groupId in siteId.
     * <p>
     * GET sites/<siteId>/group-members/<groupId>
     */
    @Override
    @WebApiDescription(title = "Returns site membership information for groupId in siteId.")
    public GroupMemberOfSite readById(String siteId, String groupId, Parameters parameters) {
        return sites.getGroup(groupId, siteId);
    }

}