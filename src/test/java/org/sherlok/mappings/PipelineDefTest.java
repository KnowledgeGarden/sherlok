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
package org.sherlok.mappings;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sherlok.utils.Create.list;
import static org.sherlok.utils.Create.map;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sherlok.FileBased;
import org.sherlok.mappings.PipelineDef.PipelineOutput;
import org.sherlok.mappings.PipelineDef.PipelineTest;

public class PipelineDefTest {

    public static PipelineDef getOpennlp_ners() throws Exception {
        PipelineDef p = (PipelineDef) new PipelineDef()
                .addTest(
                        new PipelineTest().setInput("myinput").setExpected(
                                map("MyAnnot",
                                        list(new JsonAnnotation()
                                                .setBegin(1)
                                                .setEnd(2)
                                                .addProperty("myprop",
                                                        "myValue")))))
                .addScriptLine("ENGINE opennlp.segmenter.en:1.6.2")
                .addScriptLine("ENGINE opennlp.pos.en:1.6.2")
                .setOutput(
                        new PipelineOutput().setAnnotationFilters(list(
                                "org.Filter", "Me"))).setDomain("dkpro");
        p.setName("OpenNlpEnSegmenter");
        p.setVersion("1.6.2");
        return p;
    }

    @Test
    public void testWriteRead() throws Exception {

        File pf = new File("target/pipelineTest_" + currentTimeMillis()
                + ".json");
        PipelineDef p = getOpennlp_ners();
        FileBased.write(pf, p);
        PipelineDef p2 = FileBased.read(pf, PipelineDef.class);
        p2.validate("");
        assertEquals(p.getName(), p2.getName());
        assertEquals(p.getVersion(), p2.getVersion());

        List<String> filters = p2.getOutput().getAnnotationFilters();
        assertEquals(2, filters.size());
        assertEquals("org.Filter", filters.get(0));

        List<PipelineTest> tests = p2.getTests();
        assertEquals(1, tests.size());
        PipelineTest test = tests.get(0);
        assertTrue(test.getInput().startsWith("myinput"));
        Map<String, List<JsonAnnotation>> outs = test.getExpected();
        assertEquals(1, outs.size());
        JsonAnnotation ta = outs.get("MyAnnot").get(0);
        assertEquals(1, ta.getBegin());
        assertEquals("myValue", ta.getProperty("myprop"));
    }

    /** Should not have includes and filters at the same time */
    @Test(expected = SherlokException.class)
    public void testFilters() throws Exception {
        PipelineDef p = new PipelineDef().setOutput(new PipelineOutput()
                .setAnnotationFilters(list("filter")).setAnnotationIncludes(
                        list("include")));
        p.setName("n");
        p.setVersion("v");
        p.validate("");
        assertEquals("n:v", p.toString());
    }

}
