/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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
package org.alfresco.repo.web.scripts.servlet;

import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.repo.management.subsystems.DefaultChildApplicationContextManager;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.external.RemoteUserMapper;
import org.alfresco.util.ApplicationContextHelper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.webscripts.Authenticator;
import org.springframework.extensions.webscripts.Description;
import org.springframework.extensions.webscripts.Description.RequiredAuthentication;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
import org.springframework.extensions.webscripts.servlet.WebScriptServletResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RemoteAuthenticatorFactoryAdminConsoleAccessTest
{
    private final String[] contextLocations = new String[] { "classpath:alfresco/application-context.xml",
        "classpath:alfresco/web-scripts-application-context.xml", "classpath:alfresco/web-scripts-application-context-test.xml" };

    private RemoteUserAuthenticatorFactory remoteUserAuthenticatorFactory;
    //    private PersonService personService;
    //    private TransactionService transactionService;
    //    private MutableAuthenticationDao authenticationDAO;
    private BlockingRemoteUserMapper blockingRemoteUserMapper;

    @Before
    public void before() throws Exception
    {
        blockingRemoteUserMapper = new BlockingRemoteUserMapper();

        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext(contextLocations);
        DefaultChildApplicationContextManager childApplicationContextManager = (DefaultChildApplicationContextManager) ctx.getBean("Authentication");
        remoteUserAuthenticatorFactory = (RemoteUserAuthenticatorFactory) ctx.getBean("webscripts.authenticator.remoteuser");
        remoteUserAuthenticatorFactory.setRemoteUserMapper(blockingRemoteUserMapper);
        remoteUserAuthenticatorFactory
            .setGetRemoteUserTimeoutMilliseconds((long) (BlockingRemoteUserMapper.BLOCKING_FOR_MILLIS / 2));//highly impatient

        childApplicationContextManager.stop();
        childApplicationContextManager.setProperty("chain", "external1:external,alfrescoNtlm1:alfrescoNtlm");
        ChildApplicationContextFactory childApplicationContextFactory = childApplicationContextManager.getChildApplicationContextFactory("external1");
        childApplicationContextFactory.stop();
        childApplicationContextFactory.setProperty("external.authentication.proxyUserName", "");
    }

    @Test
    public void testAdminCanAccessAdminConsoleScript()
    {
        Set<String> families = new HashSet<>();
        families.add("AdminConsole");
        final String headerToAdd = "Basic YWRtaW46YWRtaW4=";
        checkForFamilies(families, headerToAdd);
    }

    @Test
    public void testAdminCanAccessAdminConsoleHelperScript()
    {
        Set<String> families = new HashSet<>();
        families.add("AdminConsoleHelper");
        final String headerToAdd = "Basic YWRtaW46YWRtaW4=";
        checkForFamilies(families, headerToAdd);
    }

    private void checkForFamilies(final Set<String> families, final String headerToAdd)
    {
        final String adminUsername = AuthenticationUtil.getAdminUserName();
        {
            blockingRemoteUserMapper.reset();
            // first try a generic resource
            final boolean authenticated = authenticate(Collections.emptySet(), null);

            assertFalse("This should not be authenticated as it is not an Admin Console requested.", authenticated);
            assertFalse("Because it is not an Admin Console, the timeout feature from BasicHttpAuthenticator should not be requested. "
                + "Therefore the interrupt should not have been called. ", blockingRemoteUserMapper.isWasInterrupted());
            assertTrue("No interrupt should have been called.",
                blockingRemoteUserMapper.getTimePassed() > BlockingRemoteUserMapper.BLOCKING_FOR_MILLIS - 1);
        }

        {
            blockingRemoteUserMapper.reset();
            // now try an admin console family page
            final boolean authenticated = authenticate(families, null);

            assertFalse("It is an AdminConsole webscript now, but Admin basic auth header was not present. It should return 401", authenticated);
            assertTrue("Because it is an AdminConsole webscript, the interrupt should have been called.",
                blockingRemoteUserMapper.isWasInterrupted());
            assertTrue("The interrupt should have been called.",
                blockingRemoteUserMapper.getTimePassed() < BlockingRemoteUserMapper.BLOCKING_FOR_MILLIS);
        }

        {
            blockingRemoteUserMapper.reset();
            // now try with valid basic auth as well
            final boolean authenticated = authenticate(families, headerToAdd);

            assertTrue("It is an AdminConsole webscript now and Admin basic auth header was present. It should succeed.", authenticated);
            final String message = "The code from blockingRemoteUserMapper shouldn't have been called";
            assertFalse(message, blockingRemoteUserMapper.isWasInterrupted());
            assertEquals(message, blockingRemoteUserMapper.getTimePassed(), 0);
        }

        {
            blockingRemoteUserMapper.reset();
            // now try with bad password
            String headerToAddWithWrongPassword = "Basic YWRtaW46YmliaQ=="; // admin:bibi
            final boolean authenticated = authenticate(families, headerToAddWithWrongPassword);

            assertFalse("It is an AdminConsole webscript now and Admin basic auth header was present BUT with wrong password. Should fail.",
                authenticated);
            final String message = "The code from blockingRemoteUserMapper shouldn't have been called";
            assertFalse(message, blockingRemoteUserMapper.isWasInterrupted());
            assertEquals(message, blockingRemoteUserMapper.getTimePassed(), 0);
        }
    }

    private boolean authenticate(Set<String> families, String headerToAdd)
    {
        WebScriptServletRequest mockRequest = prepareMockRequest(families, headerToAdd); //admin:admin

        WebScriptServletResponse mockResponse = prepareMockResponse();

        Authenticator authenticator = remoteUserAuthenticatorFactory.create(mockRequest, mockResponse);
        return authenticator.authenticate(RequiredAuthentication.admin, false);
    }

    private WebScriptServletRequest prepareMockRequest(Set<String> families, String headerToAdd)
    {
        HttpServletRequest mockHttpRequest = mock(HttpServletRequest.class);
        when(mockHttpRequest.getScheme()).thenReturn("http");
        if (headerToAdd != null)
        {
            when(mockHttpRequest.getHeader("Authorization")).thenReturn(headerToAdd);
        }
        WebScriptServletRequest mockRequest = mock(WebScriptServletRequest.class);
        when(mockRequest.getHttpServletRequest()).thenReturn(mockHttpRequest);

        WebScript mockWebScript = mock(WebScript.class);
        Match mockMatch = new Match("fake", Collections.EMPTY_MAP, "whatever", mockWebScript);
        when(mockRequest.getServiceMatch()).thenReturn(mockMatch);

        Description mockDescription = mock(Description.class);
        when(mockWebScript.getDescription()).thenReturn(mockDescription);
        when(mockDescription.getFamilys()).thenReturn(families);
        return mockRequest;
    }

    private WebScriptServletResponse prepareMockResponse()
    {
        HttpServletResponse mockHttpResponse = mock(HttpServletResponse.class);
        WebScriptServletResponse mockResponse = mock(WebScriptServletResponse.class);
        when(mockResponse.getHttpServletResponse()).thenReturn(mockHttpResponse);
        return mockResponse;
    }
}

class BlockingRemoteUserMapper implements RemoteUserMapper
{
    public static final int BLOCKING_FOR_MILLIS = 1000;
    private volatile boolean wasInterrupted;
    private volatile int timePassed;

    @Override
    public String getRemoteUser(HttpServletRequest request)
    {
        long t1 = System.currentTimeMillis();
        try
        {
            Thread.sleep(BLOCKING_FOR_MILLIS);
        }
        catch (InterruptedException ie)
        {
            wasInterrupted = true;
        }
        finally
        {
            timePassed = (int) (System.currentTimeMillis() - t1);
        }
        return null;
    }

    public boolean isWasInterrupted()
    {
        return wasInterrupted;
    }

    public int getTimePassed()
    {
        return timePassed;
    }

    public void reset()
    {
        wasInterrupted = false;
        timePassed = 0;
    }
}