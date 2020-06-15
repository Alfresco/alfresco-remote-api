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

import org.alfresco.query.PagingResults;
import org.alfresco.rest.api.model.*;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Paging;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;

public interface Sites
{
	SiteInfo validateSite(String siteShortName);
	SiteInfo validateSite(NodeRef nodeRef);

    CollectionWithPagingInfo<SiteMember> getSiteMembers(String siteShortName, Parameters parameters);
    Site createSite(Site site, Parameters parameters);
    Site getSite(String siteId);
    Site updateSite(String siteId, SiteUpdate site, Parameters parameters);
	void deleteSite(String siteId, Parameters parameters);

    CollectionWithPagingInfo<MemberOfSite> getSites(String personId, Parameters parameters);
	MemberOfSite getMemberOfSite(String personId, String siteShortName);

	SiteMember getSiteMember(String personId, String siteShortName);
	SiteMember addSiteMember(String siteShortName, SiteMember siteMember);
    SiteMember updateSiteMember(String siteShortName, SiteMember siteMember);
	void removeSiteMember(String personId, String siteId);

	SiteContainer getSiteContainer(String siteShortName, String containerId);
	PagingResults<SiteContainer> getSiteContainers(String siteShortName, Paging paging);
	CollectionWithPagingInfo<Site> getSites(Parameters parameters);

    FavouriteSite getFavouriteSite(String personId, String siteShortName);
    void addFavouriteSite(String personId, FavouriteSite favouriteSite);
    void removeFavouriteSite(String personId, String siteId);
    CollectionWithPagingInfo<FavouriteSite> getFavouriteSites(String personId, Parameters parameters);

    String getSiteRole(String siteId);
    String getSiteRole(String siteId, String personId);

    CollectionWithPagingInfo<SiteGroup> getGroups(String siteId, Parameters parameters);
    SiteGroup addGroup(String siteId, SiteGroup group);
    SiteGroup getGroup(String siteId, String groupId);
    SiteGroup updateGroup(String siteId, SiteGroup group);
    void deleteGroup(String groupId, String siteId);

    String PARAM_PERMANENT = "permanent";
    String PARAM_SKIP_ADDTOFAVORITES = "skipAddToFavorites";
    String PARAM_SKIP_SURF_CONFIGURATION = "skipConfiguration";

    String PARAM_SITE_ID          = "id";
    String PARAM_SITE_TITLE       = "title";
    String PARAM_SITE_DESCRIPTION = "description";

    String PARAM_SITE_ROLE = "role";
    String PARAM_VISIBILITY = "visibility";
    String PARAM_PRESET = "preset";
}
