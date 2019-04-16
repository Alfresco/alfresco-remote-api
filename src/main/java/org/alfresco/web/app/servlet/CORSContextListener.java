/*
 * #%L
 * Alfresco Repository WAR Community
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
package org.alfresco.web.app.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.EnumSet;

public class CORSContextListener implements ServletContextListener
{
    private Log logger = LogFactory.getLog(getClass());

    private final EnumSet<DispatcherType> DISPATCHER_TYPE = EnumSet
            .of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC);

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        ServletContext servletContext = sce.getServletContext();
        ApplicationContext rootContext = WebApplicationContextUtils.getRequiredWebApplicationContext(sce.getServletContext());
        initCORS(servletContext, rootContext);

    }

    /**
     * Initializes CORS filter
     */
    private void initCORS(ServletContext servletContext, ApplicationContext rootContext)
    {
        Environment env = rootContext.getEnvironment();
        Boolean corsEnabled = env.getProperty("cors.enabled", Boolean.class, false);
        if (corsEnabled)
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Registering CORS Filter");
            }
            FilterRegistration.Dynamic corsFilter = servletContext.addFilter("CorsFilter", "org.apache.catalina.filters.CorsFilter");
            corsFilter.setInitParameter("cors.allowed.origins", env.getProperty("cors.allowed.origins"));
            corsFilter.setInitParameter("cors.allowed.methods", env.getProperty("cors.allowed.methods"));
            corsFilter.setInitParameter("cors.allowed.headers", env.getProperty("cors.allowed.headers"));
            corsFilter.setInitParameter("cors.exposed.headers", env.getProperty("cors.exposed.headers"));
            corsFilter.setInitParameter("cors.support.credentials", env.getProperty("cors.support.credentials", Boolean.class, false).toString());
            corsFilter.setInitParameter("cors.preflight.maxage", env.getProperty("cors.preflight.maxage"));
            corsFilter.addMappingForUrlPatterns(DISPATCHER_TYPE, false, "/*");
        }
        else
        {
            if(logger.isDebugEnabled())
            {
                logger.debug("Skipping registration of CORS Filter");
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {

    }
}
