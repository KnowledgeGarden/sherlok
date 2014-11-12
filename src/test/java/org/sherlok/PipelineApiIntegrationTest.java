package org.sherlok;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.sherlok.SherlokServer.STATUS_INVALID;
import static org.sherlok.SherlokServer.STATUS_OK;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.sherlok.mappings.PipelineDef;
import org.sherlok.mappings.PipelineDef.PipelineEngine;

import spark.StopServer;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Integration tests for pipeline REST API. This runs in a separate Spark server
 * on another port. However, ATM it share the same config, so stuff created
 * should be deleted after a test.
 * 
 * @author renaud@apache.org
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PipelineApiIntegrationTest {

    final static int testPort = 9606;
    final static String url = "http://localhost:" + testPort + "/";

    @BeforeClass
    public static void beforeClass() {
        SherlokServer.init(testPort);
    }

    @AfterClass
    public static void afterClass() {
        StopServer.stop();
    }

    @Test
    public void test010GetPipelines() {
        get(url + "pipelines").then() //
                .contentType(JSON).statusCode(STATUS_OK);
        // TODO .body("[0]",
        // hasItems("name", "version", "description", "language",
        // "domain", "loadOnStartup", "engines"))//
    }

    @Test
    public void test020GetPipeline() {
        get(url + "pipelines/opennlp_en_ners/1.6.2").then()//
                .contentType(JSON).statusCode(STATUS_OK)//
                .body("name", equalTo("opennlp_en_ners"))//
                .body("version", equalTo("1.6.2"))//
                .body("loadOnStartup", equalTo(true))//
                .body("engines[0].id", equalTo("OpenNlpEnSegmenter:1.6.2"))//
                .body("engines[0].script", equalTo(null));
    }

    @Test
    /** Let's put a new test pipeline*/
    public void test030PutPipeline() throws JsonProcessingException {
        PipelineDef e = new PipelineDef().setDomain("test").addEngine(
                new PipelineEngine("SampleEngine:1"));
        e.setName("test");
        e.setVersion("1");
        String testPipelineDef = FileBased.MAPPER.writeValueAsString(e);

        given().content(testPipelineDef)//
                .when().put(url + "pipelines")//
                .then().statusCode(STATUS_OK);
    }

    @Test
    /** Putting a faulty pipeline should fail */
    public void test031PutFaultyPipeline() throws JsonProcessingException {
        given().content("blah")//
                .when().put(url + "pipelines")//
                .then().statusCode(STATUS_INVALID);
    }

    @Test
    /** Putting a faulty pipeline should fail (id is missing) */
    public void test032PutFaultyPipeline() throws JsonProcessingException {
        given().content("{  \"name\" : \"opennlp_en_ners\"}")//
                .when().put(url + "pipelines")//
                .then().statusCode(STATUS_INVALID);
    }

    @Test
    /** .. and check that the new test pipeline is here */
    public void test040GetTestPipeline() {
        get(url + "pipelines/test/1").then()//
                .contentType(JSON).statusCode(STATUS_OK)//
                .body("name", equalTo("test"))//
                .body("version", equalTo("1"))//
                .body("engines[0].id", equalTo("SampleEngine:1"))//
                .body("engines[0].script", equalTo(null))//
        ;
    }

    @Test
    /** .. and check that a bogus pipelien is NOT here */
    public void test041GetFaultyTestPipeline() {
        get(url + "pipelines/test/1000000000198198")//
                .then().statusCode(STATUS_INVALID);
    }

    @Test
    /** Now let's delete the test pipeline*/
    public void test050DeletePipeline() throws JsonProcessingException {
        when().delete(url + "pipelines/test/1").//
                then().statusCode(STATUS_OK);
    }

    @Test
    /** ... and check that it is gone. */
    public void test060GetTestPipelineGone() {
        get(url + "pipelines/test/1")//
                .then().statusCode(STATUS_INVALID);
    }
}
