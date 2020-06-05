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

package org.alfresco.rest.api.tests;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.repo.tenant.TenantUtil.TenantRunAsWork;
import org.alfresco.rest.api.tests.RepoService.TestSite;
import org.alfresco.rest.api.tests.client.PublicApiClient.Sites;
import org.alfresco.rest.api.tests.client.PublicApiException;
import org.alfresco.rest.api.tests.client.data.GroupMemberOfSite;
import org.alfresco.rest.api.tests.client.data.SiteRole;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestSiteGroups extends AbstractBaseApiTest {
    protected AuthorityService authorityService;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();
        authorityService = (AuthorityService) applicationContext.getBean("AuthorityService");
    }

    @Test
    public void testCreateGroupMembers() {
        String groupName = null;
        Sites sitesProxy = publicApiClient.sites();

        try {
            groupName = createAuthorityContext(user1);

            setRequestContext(networkOne.getId(), DEFAULT_ADMIN, DEFAULT_ADMIN_PWD);


            TestSite site = TenantUtil.runAsUserTenant(new TenantRunAsWork<TestSite>() {
                @Override
                public TestSite doWork() throws Exception {
                     return networkOne.createSite(SiteVisibility.PRIVATE);
                }
            }, DEFAULT_ADMIN, networkOne.getId());


            GroupMemberOfSite reponse = sitesProxy.addGroup(site.getSiteId(), new GroupMemberOfSite(groupName, SiteRole.SiteCollaborator.name()));
            assertEquals(reponse.getGroup().getId(), groupName);
            assertEquals(reponse.getRole(), SiteRole.SiteCollaborator.name());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearAuthorityContext(groupName);
        }
    }

    private String createAuthorityContext(String userName) throws PublicApiException {
        String groupName = "Test_GroupA" + GUID.generate();
        AuthenticationUtil.setRunAsUser(userName);

        groupName = authorityService.getName(AuthorityType.GROUP, groupName);

        if (!authorityService.authorityExists(groupName)) {
            AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

            groupName = authorityService.createAuthority(AuthorityType.GROUP, groupName);
            authorityService.setAuthorityDisplayName(groupName, "Test Group A");
        }


        authorityService.addAuthority(groupName, user1);
        authorityService.addAuthority(groupName, user2);

        return groupName;
    }

    private void clearAuthorityContext(String groupName) {
        if (groupName != null && authorityService.authorityExists(groupName)) {
            AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
            authorityService.deleteAuthority(groupName, true);
        }
    }

    @Override
    public String getScope() {
        return "public";
    }
}
