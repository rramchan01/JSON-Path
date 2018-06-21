package com.ravi.kickstart;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.Lists;

public class Sample {

      public static void main(String[] args) throws IOException, URISyntaxException {
             String input = loadJSON();

             List<String> testJsonPaths = Lists.newArrayList("$.version", 
                          "$.tags.instanceID",
                          "$.data-pipeline-product",
                          "$.domain",
                          "$.tags.correlationContext.runID",
                          "$.tags.correlationContext.agentName",
                          "$.tags.correlationContext.processName");

             List<JSONRecord> actualResult = new JsonFlattener("testValidJsonWithoutArray", testJsonPaths).flatten(input);
             for (JSONRecord record : actualResult) {
                    System.out.println(record.getCells());
             }
      }

      private static String loadJSON() throws IOException, URISyntaxException {
             String message = FileUtils.readFileToString(new File(
                          "/Users/r0r00i9/eclipse-workspace/JSON-Path/resources/Sample.json"));
             return message;
      }
}