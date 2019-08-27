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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;

import org.alfresco.repo.content.ContentLimitViolationException;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TempOutputStream extends OutputStream
{
    private static final Log logger = LogFactory.getLog(TempOutputStream.class);

    private static final int DEFAULT_MEMORY_THRESHOLD = 4 * 1024 * 1024; // 4mb
    private static final String ALGORITHM = "AES";
    private static final String MODE = "CTR";
    private static final String PADDING = "PKCS5Padding";
    private static final String TRANSFORMATION = ALGORITHM + '/' + MODE + '/' + PADDING;
    private static final int KEY_SIZE = 128;

    private final File tempDir;
    private final int memoryThreshold;
    private final long maxContentSize;
    private boolean encrypt;
    private boolean deleteTempFileOnClose;

    private long length = 0;
    private OutputStream outputStream;
    private File tempFile;
    private TempByteArrayOutputStream tempStream;

    private Key symKey;
    private byte[] iv;

    public TempOutputStream(File tempDir, int memoryThreshold, long maxContentSize)
    {
        this(tempDir, memoryThreshold, maxContentSize, true);
    }

    public TempOutputStream(File tempDir, int memoryThreshold, long maxContentSize, boolean deleteTempFileOnClose)
    {
        this.tempDir = tempDir;
        this.memoryThreshold = (memoryThreshold < 0) ? DEFAULT_MEMORY_THRESHOLD : memoryThreshold;
        this.maxContentSize = maxContentSize;
        this.deleteTempFileOnClose = deleteTempFileOnClose;

        this.tempStream = new TempByteArrayOutputStream();
        this.outputStream = this.tempStream;
    }

    /**
     * Returns the data as an InputStream.
     */
    public InputStream getInputStream() throws IOException
    {
        if (tempFile != null)
        {
            if (encrypt)
            {
                final Cipher cipher;
                try
                {
                    cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.DECRYPT_MODE, symKey, new IvParameterSpec(iv));
                }
                catch (Exception e)
                {
                    destroy();

                    if (logger.isErrorEnabled())
                    {
                        logger.error("Cannot initialize decryption cipher", e);
                    }

                    throw new IOException("Cannot initialize decryption cipher", e);
                }

                return new BufferedInputStream(new CipherInputStream(new FileInputStream(tempFile), cipher));
            }
            return new BufferedInputStream(new FileInputStream(tempFile));
        }
        else
        {
            return new ByteArrayInputStream(tempStream.getBuffer(), 0, tempStream.getCount());
        }
    }

    @Override
    public void write(int b) throws IOException
    {
        update(1);
        outputStream.write(b);
    }

    public void write(byte b[], int off, int len) throws IOException
    {
        update(len);
        outputStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        outputStream.flush();
    }

    @Override
    public void close() throws IOException
    {
        close(deleteTempFileOnClose);
    }

    public void destroy() throws IOException
    {
        close(true);
    }

    public long getLength()
    {
        return length;
    }

    private void close(boolean deleteTempFileOnClose)
    {
        if (outputStream != null)
        {
            try
            {
                outputStream.flush();
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Flushing the output stream failed", e);
                }
            }

            try
            {
                outputStream.close();
            }
            catch (IOException e)
            {
                if (logger.isErrorEnabled())
                {
                    logger.error("Closing the output stream failed", e);
                }
            }
        }

        if (deleteTempFileOnClose && (tempFile != null))
        {
            try
            {
                boolean isDeleted = tempFile.delete();
                if (!isDeleted)
                {
                    if (logger.isErrorEnabled())
                    {
                        logger.error("Temp file could not be deleted: " + tempFile.getAbsolutePath());
                    }
                }
                else
                {
                    if (logger.isErrorEnabled())
                    {
                        logger.debug("Deleted temp file: " + tempFile.getAbsolutePath());
                    }
                }
            }
            finally
            {
                tempFile = null;
            }
        }
    }

    private void update(int len) throws IOException
    {
        if (maxContentSize > -1 && length + len > maxContentSize)
        {
            destroy();
            StringBuilder msg = new StringBuilder();
            msg.append("Content size violation, limit = ").append(maxContentSize);

            throw new ContentLimitViolationException(msg.toString());
        }

        if (tempFile == null && (tempStream.getCount() + len) > memoryThreshold)
        {
            File file = TempFileProvider.createTempFile("ws_request_", ".bin", tempDir);

            BufferedOutputStream fileOutputStream;
            if (encrypt)
            {
                try
                {
                    // Generate a symmetric key
                    final KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
                    keyGen.init(KEY_SIZE);
                    symKey = keyGen.generateKey();

                    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.ENCRYPT_MODE, symKey);

                    iv = cipher.getIV();

                    fileOutputStream = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(tempFile), cipher));
                }
                catch (Exception e)
                {
                    if (logger.isErrorEnabled())
                    {
                        logger.error("Cannot initialize encryption cipher", e);
                    }

                    throw new IOException("Cannot initialize encryption cipher", e);
                }
            }
            else
            {
                fileOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            }

            fileOutputStream.write(this.tempStream.getBuffer(), 0, this.tempStream.getCount());
            fileOutputStream.flush();

            try
            {
                tempStream.close();
            }
            catch (IOException e)
            {
                // Ignore exception
            }
            tempStream = null;

            tempFile = file;
            outputStream = fileOutputStream;
        }

        length += len;
    }

    private static class TempByteArrayOutputStream extends ByteArrayOutputStream
    {
        /**
         * @return The internal buffer where data is stored
         */
        public byte[] getBuffer()
        {
            return buf;
        }

        /**
         * @return The number of valid bytes in the buffer.
         */
        public int getCount()
        {
            return count;
        }
    }
}
