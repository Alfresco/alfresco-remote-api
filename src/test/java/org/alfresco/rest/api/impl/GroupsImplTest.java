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
package org.alfresco.rest.api.impl;

import org.alfresco.repo.domain.permissions.Authority;
import org.alfresco.repo.security.authority.AuthorityDAO;
import org.alfresco.rest.api.impl.GroupsImpl;
import org.alfresco.rest.api.tests.client.data.Group;
import org.alfresco.rest.framework.resource.parameters.Parameters;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class GroupsImplTest
{
    private GroupsImpl groupsImpl;

    @Test(expected = IllegalArgumentException.class)
    public void illegalCharacterInGroupId() {
        groupsImpl  = new GroupsImpl();
        Group group= new Group(); //Mockito.mock(Group.class);
        String groupId="GROUP_Identifier\"WithIllegalChar";
        group.setId(groupId);
        group.setDisplayName(groupId);
        AuthorityDAO authorityDAO =  Mockito.mock(AuthorityDAO.class);
        when(authorityDAO.getAuthorityDisplayName(anyString())).thenReturn(groupId);
        AuthorityService authorityService = Mockito.mock(AuthorityService.class);
        groupsImpl.setAuthorityService(authorityService);
        groupsImpl.setAuthorityDAO(authorityDAO);
        //String groupId="GROUP_Identifier WithIllegalChar";
        when(authorityService.authorityExists(anyString())).thenReturn(false);
        when(authorityService.getName(any(AuthorityType.class), anyString())).thenReturn("test");
        when(authorityService.createAuthority(any(AuthorityType.class), anyString(), anyString(), anySet())).thenReturn(PermissionService.ALL_AUTHORITIES);
        Parameters parameters = Mockito.mock(Parameters.class);
        //when(parameters.getInclude()).thenReturn(null);
        groupsImpl.create(group, parameters);
    }

    @Test()
    public void legalCharacterInGroupId() {
        groupsImpl  = new GroupsImpl();
        Group group= new Group(); //Mockito.mock(Group.class);
        String groupId="GROUP_IdentifierWithIllegalChar";
        group.setId(groupId);
        group.setDisplayName(groupId);
        AuthorityDAO authorityDAO =  Mockito.mock(AuthorityDAO.class);
        when(authorityDAO.getAuthorityDisplayName(anyString())).thenReturn(groupId);
        AuthorityService authorityService = Mockito.mock(AuthorityService.class);
        groupsImpl.setAuthorityService(authorityService);
        groupsImpl.setAuthorityDAO(authorityDAO);
        //String groupId="GROUP_Identifier WithIllegalChar";
        when(authorityService.authorityExists(anyString())).thenReturn(false);
        when(authorityService.getName(any(AuthorityType.class), anyString())).thenReturn("test");
        when(authorityService.createAuthority(any(AuthorityType.class), anyString(), anyString(), anySet())).thenReturn(PermissionService.ALL_AUTHORITIES);
        Parameters parameters = Mockito.mock(Parameters.class);
        //when(parameters.getInclude()).thenReturn(null);
        groupsImpl.create(group, parameters);
    }
}
