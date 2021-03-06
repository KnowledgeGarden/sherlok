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
package org.sherlok.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sherlok.FileBased;
import org.sherlok.utils.ops.FileOps;

public class GitConfigVariableTest {

    /**
     * See note about sherlok_dependency_test repository in
     * {@link ConfigVariable}
     */
    public static final String TEST_URL = "https://github.com/sherlok/sherlok_dependency_test.git";
    private static final String TEST_INVALID_URL = "http://bad_example";
    private static final String FILE_RELATIVE_PATH = "resources/file.txt";

    private static final String MASTER = "master";
    private static final String DEVELOP = "develop";
    /**
     * SHA maps to a special commit where the content of file.txt contains
     * FILE_CONTENT_SHA
     */
    private static final String SHA = "593c73210f7b7578d27158b302002798ab3b10b4"; 
    private static final String TAG = "tag";
    private static final String INVALID_BRANCH = "poleved";

    public static final String FILE_CONTENT_MASTER = "MASTER\n";
    public static final String FILE_CONTENT_DEVELOP = "DEVELOP\n";
    private static final String FILE_CONTENT_SHA = "SHA\n";
    private static final String FILE_CONTENT_TAG = "TAG\n";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Remove any cached repository
        GitConfigVariable.cleanCache();
    }

    @Test
    public final void testGetProcessedValueMaster()
            throws ProcessConfigVariableException, IOException {
        String val1 = testGetProcessedValueImpl(MASTER, FILE_CONTENT_MASTER, true);
        String val2 = testGetProcessedValueImpl(null, FILE_CONTENT_MASTER, true);
        
        assertEquals("null should also map to the maste branch", val1, val2);

        val1 = testGetProcessedValueImpl(MASTER, FILE_CONTENT_MASTER, false);
        val2 = testGetProcessedValueImpl(null, FILE_CONTENT_MASTER, false);

        assertEquals(
                "using RUTA mode should not introduce any differences in paths for the same branch",
                val1, val2);
    }

    @Test
    public final void testGetProcessedValueSHA()
            throws ProcessConfigVariableException, IOException {
        testGetProcessedValueImpl(SHA, FILE_CONTENT_SHA, true);
    }

    @Test
    public final void testGetProcessedValueTAG()
            throws ProcessConfigVariableException, IOException {
        testGetProcessedValueImpl(TAG, FILE_CONTENT_TAG, true);
    }

    @Test
    public final void testGetProcessedValueDevelop()
            throws ProcessConfigVariableException, IOException {
        testGetProcessedValueImpl(DEVELOP, FILE_CONTENT_DEVELOP, true);
    }

    @Test(expected = ProcessConfigVariableException.class)
    public final void testGetProcessedValueInvalidURL()
            throws ProcessConfigVariableException {
        GitConfigVariable var = new GitConfigVariable(TEST_INVALID_URL, null,
                true);
        var.getProcessedValue(); // should fire an exception
    }

    @Test(expected = ProcessConfigVariableException.class)
    public final void testGetProcessedValueInvalidBranch()
            throws ProcessConfigVariableException {
        GitConfigVariable var = new GitConfigVariable(TEST_URL, INVALID_BRANCH,
                true);
        var.getProcessedValue(); // should fire an exception
    }

    private String testGetProcessedValueImpl(String ref,
            String expectedContent, Boolean rutaCompatibility)
            throws ProcessConfigVariableException, IOException {
        GitConfigVariable var = new GitConfigVariable(TEST_URL, ref, rutaCompatibility);
        String val = var.getProcessedValue(); // should not fire an exception
        assertNotNull(val);

        // should not fire an exception:
        String content = getTestFileContent(val, rutaCompatibility);
        assertEquals("checking content", expectedContent, content);

        return val;
    }

    private static File getTestFile(String processedValue,
            Boolean rutaCompatibility) {
        File dir = null;
        if (rutaCompatibility) {
            dir = new File(FileBased.RUTA_RESOURCES_PATH, processedValue);
        } else {
            dir = new File(processedValue);
        }
        return new File(dir, FILE_RELATIVE_PATH);
    }

    private static String getTestFileContent(String processedValue,
            Boolean rutaCompatibility)
            throws IOException {
        File file = getTestFile(processedValue, rutaCompatibility);
        return FileOps.readContent(file);
    }

}
