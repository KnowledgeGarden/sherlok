/**
 * Copyright (C) 2014-2015 Renaud Richardet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sherlok;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.http.Part;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sherlok.utils.ValidationException;

import spark.utils.IOUtils;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileBasedTest {

    static final String uploadFile = "testUploadFile.txt";

    @Test(expected = ValidationException.class)
    public void test01_GetInexistentFile() throws Exception {
        FileBased.getResource("test/" + uploadFile);
    }

    @Test
    public void test02_UploadFile() throws Exception {
        FileBased.putResource("test/" + uploadFile, new Part() {

            @Override
            public void write(String fileName) throws IOException {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public long getSize() {
                return 1;
            }

            @Override
            public String getName() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return FileBasedTest.class.getResourceAsStream(uploadFile);
            }

            @Override
            public Collection<String> getHeaders(String name) {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public Collection<String> getHeaderNames() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public String getHeader(String name) {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public String getContentType() {
                throw new UnsupportedOperationException("not implemented");
            }

            @Override
            public void delete() throws IOException {
                throw new UnsupportedOperationException("not implemented");
            }
        });
    }

    @Test
    public void test03_GetFile() throws Exception {
        InputStream is = FileBased.getResource("test/" + uploadFile);
        String txt = IOUtils.toString(is);
        assertEquals("a test file\n", txt);
    }

    @Test
    public void test04_ListFile() throws Exception {
        Collection<String> resources = FileBased.allResources();
        assertTrue(resources.contains("test/" + uploadFile));
    }

    @Test
    public void test05_DeleteFile() throws Exception {
        FileBased.deleteResource("test/" + uploadFile);
    }
}
