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
import org.alfresco.rest.api.tests.RepoService.TestNetwork;
import org.alfresco.rest.api.tests.RepoService.TestPerson;
import org.alfresco.rest.api.tests.RepoService.TestSite;
import org.alfresco.rest.api.tests.client.PublicApiClient.Sites;
import org.alfresco.rest.api.tests.client.RequestContext;
import org.alfresco.rest.api.tests.client.data.Group;
import org.alfresco.rest.api.tests.client.data.GroupMemberOfSite;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;

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

        try {
            groupName = createAuthorityContext(user1);
            System.out.println(groupName);

        } finally {
            clearAuthorityContext(groupName);
        }
    }

    private String createAuthorityContext(String userName) {
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


    @Test
    public void testSiteGroups() throws Exception {
        Group firstGroup = new Group();
        Group secondGroup = new Group();
        Sites sitesProxy = publicApiClient.sites();

        // test: create site membership, remove it, get list of site membership
        Iterator<TestNetwork> accountsIt = getTestFixture().getNetworksIt();
        assertTrue(accountsIt.hasNext());
        final TestNetwork network = accountsIt.next();
        assertTrue(accountsIt.hasNext());

        final List<TestPerson> people = new ArrayList<TestPerson>();

        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

        // Create user
        TenantUtil.runAsSystemTenant(new TenantRunAsWork<Void>() {
            @Override
            public Void doWork() throws Exception {
                TestPerson person = network.createUser();
                people.add(person);
                person = network.createUser();
                people.add(person);
                person = network.createUser();
                people.add(person);
                person = network.createUser();
                people.add(person);

                return null;
            }
        }, network.getId());

        TestPerson person1 = people.get(0);
        TestPerson person2 = people.get(1);
        TestPerson person3 = people.get(2);
        TestPerson person4 = people.get(3);

        //add users to groups
        firstGroup.setId("GROUP_firstGroup");
        firstGroup.setDisplayName("firstGroup");
        String firstAuthName = authorityService.createAuthority(AuthorityType.GROUP, firstGroup.getId());
        authorityService.setAuthorityDisplayName(firstGroup.getId(), firstGroup.getDisplayName());

        secondGroup.setId("GROUP_secondGroup");
        secondGroup.setDisplayName("Aaaaah, a secondGroup!");
        String secondAuthName = authorityService.createAuthority(AuthorityType.GROUP, secondGroup.getId());
        authorityService.setAuthorityDisplayName(secondGroup.getId(), secondGroup.getDisplayName());

        authorityService.addAuthority(firstAuthName, person2.getId());
        authorityService.addAuthority(firstAuthName, person3.getId());
        authorityService.addAuthority(secondAuthName, person4.getId());

        // Create site
        TestSite site = TenantUtil.runAsUserTenant(new TenantRunAsWork<TestSite>() {
            @Override
            public TestSite doWork() throws Exception {
                TestSite site = network.createSite(SiteVisibility.PRIVATE);
                return site;
            }
        }, person1.getId(), network.getId());

        {
            // create a site member
            publicApiClient.setRequestContext(new RequestContext(network.getId(), person2.getId()));
//            GroupMemberOfSite siteMember = sitesProxy.addGroup(site.getSiteId(), firstGroup);
//            System.out.println(siteMember);

//				// create another site member
//				publicApiClient.setRequestContext(new RequestContext(network.getId(), person2.getId()));
//				SiteMember siteMemberAno = sitesProxy.createSiteMember(site.getSiteId(), new SiteMember(person3.getId(), SiteRole.SiteCollaborator.toString()));
//				assertEquals(person3.getId(), siteMemberAno.getMemberId());
//				assertEquals(SiteRole.SiteCollaborator.toString(), siteMemberAno.getRole());
//				siteMemberAno.setSiteId(site.getSiteId()); // note: needed for contains check below, ugh
//
//				// unknown site
//				try {
//					publicApiClient.setRequestContext(new RequestContext(network.getId(), person2.getId()));
//					sitesProxy.removeSiteMember(GUID.generate(), siteMember);
//					fail();
//				} catch (PublicApiException e) {
//					assertEquals(HttpStatus.SC_NOT_FOUND, e.getHttpResponse().getStatusCode());
//				}
//

        }
    }


    @Override
    public String getScope() {
        return "public";
    }
}
