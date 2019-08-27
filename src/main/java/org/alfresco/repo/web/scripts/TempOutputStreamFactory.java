/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.repo.web.scripts;

import java.io.File;

public class TempOutputStreamFactory
{
    private final File tempDir;
    private int memoryThreshold;
    private long maxContentSize;
    private boolean encrypt;
    private boolean deleteTempFileOnClose;

    protected TempOutputStreamFactory(File tempDir, int memoryThreshold, long maxContentSize)
    {
        this(tempDir, memoryThreshold, maxContentSize, false, true);
    }

    public TempOutputStreamFactory(File tempDir, int memoryThreshold, long maxContentSize, boolean encrypt, boolean deleteTempFileOnClose)
    {
        this.tempDir = tempDir;
        this.memoryThreshold = memoryThreshold;
        this.maxContentSize = maxContentSize;
        this.encrypt = encrypt;
        this.deleteTempFileOnClose = deleteTempFileOnClose;
    }

    public TempOutputStream createOutputStream()
    {
        return new TempOutputStream(tempDir, memoryThreshold, maxContentSize, deleteTempFileOnClose);
    }

    public File getTempDir()
    {
        return tempDir;
    }

    public int getMemoryThreshold()
    {
        return memoryThreshold;
    }

    public long getMaxContentSize()
    {
        return maxContentSize;
    }

    public boolean isEncrypt()
    {
        return encrypt;
    }

    public boolean isDeleteTempFileOnClose()
    {
        return deleteTempFileOnClose;
    }
}
