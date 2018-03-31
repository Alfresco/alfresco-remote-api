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
package org.alfresco.repo.web.scripts.model.filefolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;

import org.alfresco.repo.model.filefolder.FileFolderLoader;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * Link to {@link FileFolderLoader}
 */
public class FileFolderLoaderPost extends AbstractWebScript implements ApplicationContextAware
{
    public static final String KEY_FOLDER_PATH = "folderPath";
    public static final String KEY_FILE_COUNT = "fileCount";
    public static final String KEY_FILES_PER_TXN = "filesPerTxn";
    public static final String KEY_MIN_FILE_SIZE = "minFileSize";
    public static final String KEY_MAX_FILE_SIZE = "maxFileSize";
    public static final String KEY_MAX_UNIQUE_DOCUMENTS = "maxUniqueDocuments";
    public static final String KEY_FORCE_BINARY_STORAGE = "forceBinaryStorage";
    public static final String KEY_DESCRIPTION_COUNT = "descriptionCount";
    public static final String KEY_DESCRIPTION_SIZE = "descriptionSize";
    public static final String KEY_COUNT = "count";
    
    public static final int DEFAULT_FILE_COUNT = 100;
    public static final int DEFAULT_FILES_PER_TXN = 100;
    public static final long DEFAULT_MIN_FILE_SIZE = 80*1024L;
    public static final long DEFAULT_MAX_FILE_SIZE = 120*1024L;
    public static final long DEFAULT_MAX_UNIQUE_DOCUMENTS = Long.MAX_VALUE;
    public static final int DEFAULT_DESCRIPTION_COUNT = 1;
    public static final long DEFAULT_DESCRIPTION_SIZE = 128L;
    public static final boolean DEFAULT_FORCE_BINARY_STORAGE = false;
    
    private ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }
    
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException
    {
        FileFolderLoader loader = (FileFolderLoader) applicationContext.getBean("fileFolderLoader");
        
        int count = 0;
        String folderPath = "";
        try
        {
            JsonNode json = AlfrescoDefaultObjectMapper.getReader().readTree(req.getContent().getContent());
            JsonNode folderPathJson = json.get(KEY_FOLDER_PATH);
            if (folderPathJson == null)
            {
                throw new WebScriptException(Status.STATUS_BAD_REQUEST, KEY_FOLDER_PATH + " not supplied.");
            }
            folderPath = folderPathJson.textValue();
            int fileCount = 100;
            if (json.has(KEY_FILE_COUNT))
            {
                fileCount = json.get(KEY_FILE_COUNT).intValue();
            }
            int filesPerTxn = DEFAULT_FILES_PER_TXN;
            if (json.has(KEY_FILES_PER_TXN))
            {
                filesPerTxn = json.get(KEY_FILES_PER_TXN).intValue();
            }
            long minFileSize = DEFAULT_MIN_FILE_SIZE;
            if (json.has(KEY_MIN_FILE_SIZE))
            {
                minFileSize = json.get(KEY_MIN_FILE_SIZE).intValue();
            }
            long maxFileSize = DEFAULT_MAX_FILE_SIZE;
            if (json.has(KEY_MAX_FILE_SIZE))
            {
                maxFileSize = json.get(KEY_MAX_FILE_SIZE).intValue();
            }
            long maxUniqueDocuments = DEFAULT_MAX_UNIQUE_DOCUMENTS;
            if (json.has(KEY_MAX_UNIQUE_DOCUMENTS))
            {
                maxUniqueDocuments = json.get(KEY_MAX_UNIQUE_DOCUMENTS).intValue();
            }
            boolean forceBinaryStorage = DEFAULT_FORCE_BINARY_STORAGE;
            if (json.has(KEY_FORCE_BINARY_STORAGE))
            {
                forceBinaryStorage = json.get(KEY_FORCE_BINARY_STORAGE).booleanValue();
            }
            int descriptionCount = DEFAULT_DESCRIPTION_COUNT;
            if (json.has(KEY_DESCRIPTION_COUNT))
            {
                descriptionCount = json.get(KEY_DESCRIPTION_COUNT).intValue();
            }
            long descriptionSize = DEFAULT_DESCRIPTION_SIZE;
            if (json.has(KEY_DESCRIPTION_SIZE))
            {
                descriptionSize = json.get(KEY_DESCRIPTION_SIZE).longValue();
            }
            
            // Perform the load
            count = loader.createFiles(
                    folderPath,
                    fileCount, filesPerTxn,
                    minFileSize, maxFileSize,
                    maxUniqueDocuments,
                    forceBinaryStorage,
                    descriptionCount, descriptionSize);
        }
        catch (FileNotFoundException e)
        {
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "Folder not found: ", folderPath);
        }
        catch (IOException iox)
        {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Could not read content from req.", iox);
        }
        // Write the response
        OutputStream os = res.getOutputStream();
        try
        {
            ObjectNode json = AlfrescoDefaultObjectMapper.createObjectNode();
            json.put(KEY_COUNT, count);
            AlfrescoDefaultObjectMapper.getWriter().writeValue(os, json);
        }
        catch (IOException e)
        {
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Failed to write JSON", e);
        }
        finally
        {
            os.close();
        }
    }
}
