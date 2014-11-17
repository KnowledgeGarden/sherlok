package org.sherlok;

import static org.junit.Assert.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;

public class UimaPipelineTest {
    private static Logger LOG = getLogger(UimaPipelineTest.class);

    @Test
    public void testDog() throws Exception {

        UimaPipeline pipeline = new PipelineLoader(new Controller().load())
                .resolvePipeline("ruta_01_annotate_dog", null);
        String result = (pipeline.annotate("dog"));
        LOG.debug(result);

        // TODO better matching
        JSONObject jsonObject = new JSONObject(result);
        JSONObject annotations = jsonObject
                .getJSONObject("@cas_feature_structures");
        JSONArray names = annotations.names();
        assertEquals(3, names.length());

        Object animal = annotations.get("26");
        assertEquals("{\"sofa\":1,\"@type\":\"Dog\",\"end\":3}",
                animal.toString());
    }

    @Test
    public void testCountries() throws Exception {

        UimaPipeline pipeline = new PipelineLoader(new Controller().load())
                .resolvePipeline("ruta_02_annotate_countries", null);
        String result = (pipeline.annotate("Switzerland"));
        LOG.debug(result);

        // TODO better matching
        JSONObject jsonObject = new JSONObject(result);
        JSONObject annotations = jsonObject
                .getJSONObject("@cas_feature_structures");
        JSONArray names = annotations.names();
        assertEquals(3, names.length());
        Object country = annotations.get("30");
        assertEquals("{\"sofa\":1,\"@type\":\"Country\",\"end\":11}",
                country.toString());
    }
}