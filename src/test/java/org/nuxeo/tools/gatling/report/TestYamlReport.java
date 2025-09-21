/*
 * (C) Copyright 2025 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.nuxeo.tools.gatling.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class TestYamlReport {

    protected static final String SIM_LOG = "simulation-small.log";
    protected static final String SIM_WITH_SPACES_LOG = "simulation-v3.2.log.gz";
    protected static final List<String> TREND_LOGS = Arrays.asList(
            "simulation.log.1.gz", "simulation.log.2.gz", "simulation.log.3.gz");

    @Test
    public void testYamlReportDefaultFilename() throws Exception {
        // Test that YAML reports get .yaml extension by default (no explicit filename set)
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Create report with YAML mode but NO explicit filename
        Report report = new Report(stats)
                .yamlReport(true)
                .setOutputDirectory(new File("/tmp"));

        // Check the default filename BEFORE calling create()
        File reportPath = report.getReportPath();
        Assert.assertTrue("YAML report should default to .yaml extension when no filename specified",
                reportPath.getName().endsWith(".yaml"));
        Assert.assertEquals("Default YAML filename should be index.yaml",
                "index.yaml", reportPath.getName());
    }

    @Test
    public void testHtmlReportDefaultFilename() throws Exception {
        // Test that HTML reports get .html extension by default (regression test)
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Create report WITHOUT YAML mode and NO explicit filename
        Report report = new Report(stats)
                .yamlReport(false)  // Explicitly set to false for HTML
                .setOutputDirectory(new File("/tmp"));

        // Check the default filename
        File reportPath = report.getReportPath();
        Assert.assertTrue("HTML report should default to .html extension when no filename specified",
                reportPath.getName().endsWith(".html"));
        Assert.assertEquals("Default HTML filename should be index.html",
                "index.html", reportPath.getName());
    }

    @Test
    public void testYamlSimulationReportIsValid() throws Exception {
        // Parse simulation
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Generate YAML report
        Writer writer = new StringWriter();
        String reportPath = new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Assert file extension
        Assert.assertTrue("Report should have .yaml extension",
                reportPath.endsWith("index.yaml"));

        // Parse YAML to validate syntax
        String yamlContent = writer.toString();
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Validate structure
        Assert.assertNotNull("YAML should parse successfully", data);
        Assert.assertTrue("Should contain simulation key",
                data.containsKey("simulation"));
        Assert.assertTrue("Should contain requests key",
                data.containsKey("requests"));
    }

    @Test
    public void testYamlApdexIndentation() throws Exception {
        // Parse simulation
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Generate YAML report
        Writer writer = new StringWriter();
        new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Parse YAML
        String yamlContent = writer.toString();
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Check apdex is properly nested
        Assert.assertTrue("Should have apdex object",
                data.containsKey("apdex"));
        Object apdexObj = data.get("apdex");
        Assert.assertTrue("Apdex should be a Map",
                apdexObj instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> apdex = (Map<String, Object>) apdexObj;
        Assert.assertTrue("Apdex should contain 't' field",
                apdex.containsKey("t"));
        Assert.assertTrue("Apdex should contain 'rating' field",
                apdex.containsKey("rating"));
        Assert.assertTrue("Apdex should contain 'score' field",
                apdex.containsKey("score"));
    }

    @Test
    public void testYamlRequestKeysWithSpecialCharacters() throws Exception {
        // Parse simulation that has requests with spaces and special chars
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_WITH_SPACES_LOG)).parse());

        // Generate YAML report
        Writer writer = new StringWriter();
        new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Parse YAML - this will fail if keys aren't properly quoted
        String yamlContent = writer.toString();
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Verify requests section exists and is valid
        Assert.assertTrue("Should have requests key",
                data.containsKey("requests"));
        Object requestsObj = data.get("requests");
        Assert.assertTrue("Requests should be a Map",
                requestsObj instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> requests = (Map<String, Object>) requestsObj;

        // Check that each request has proper structure
        for (Map.Entry<String, Object> entry : requests.entrySet()) {
            String requestName = entry.getKey();
            Assert.assertNotNull("Request name should not be null", requestName);

            Object requestData = entry.getValue();
            Assert.assertTrue("Request data should be a Map",
                    requestData instanceof Map);

            @SuppressWarnings("unchecked")
            Map<String, Object> request = (Map<String, Object>) requestData;

            // Verify nested apdex in request
            if (request.containsKey("apdex")) {
                Object reqApdex = request.get("apdex");
                Assert.assertTrue("Request apdex should be a Map",
                        reqApdex instanceof Map);

                @SuppressWarnings("unchecked")
                Map<String, Object> apdexMap = (Map<String, Object>) reqApdex;
                Assert.assertTrue("Request apdex should have 't' field",
                        apdexMap.containsKey("t"));
            }
        }
    }

    @Test
    public void testYamlDiffReportStructure() throws Exception {
        // Test that differential reports have correct nested structure
        // Parse two simulations for diff report
        List<SimulationContext> stats = Arrays.asList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse(),
                ParserFactory.getParser(getResourceFile(SIM_WITH_SPACES_LOG)).parse()
        );

        // Generate YAML diff report
        Writer writer = new StringWriter();
        String reportPath = new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Parse YAML
        String yamlContent = writer.toString();
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Validate correct structure - should have exactly these root keys
        Assert.assertTrue("Should have 'throughput' at root", data.containsKey("throughput"));
        Assert.assertTrue("Should have 'average' at root", data.containsKey("average"));
        Assert.assertTrue("Should have 'ref' at root", data.containsKey("ref"));
        Assert.assertTrue("Should have 'challenger' at root", data.containsKey("challenger"));

        // These should NOT be at root level (they should be nested)
        Assert.assertFalse("'simulation' should not be at root level", data.containsKey("simulation"));
        Assert.assertFalse("'start' should not be at root level", data.containsKey("start"));
        Assert.assertFalse("'duration' should not be at root level", data.containsKey("duration"));

        // Verify ref is a proper nested object
        Object refObj = data.get("ref");
        Assert.assertTrue("'ref' should be a Map", refObj instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> ref = (Map<String, Object>) refObj;
        Assert.assertTrue("ref should contain 'simulation'", ref.containsKey("simulation"));
        Assert.assertTrue("ref should contain 'start'", ref.containsKey("start"));
        Assert.assertTrue("ref should contain 'requests'", ref.containsKey("requests"));

        // Verify challenger is a proper nested object
        Object challengerObj = data.get("challenger");
        Assert.assertTrue("'challenger' should be a Map", challengerObj instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> challenger = (Map<String, Object>) challengerObj;
        Assert.assertTrue("challenger should contain 'simulation'", challenger.containsKey("simulation"));
        Assert.assertTrue("challenger should contain 'start'", challenger.containsKey("start"));
        Assert.assertTrue("challenger should contain 'requests'", challenger.containsKey("requests"));

        // Verify throughput structure
        Object throughputObj = data.get("throughput");
        Assert.assertTrue("'throughput' should be a Map", throughputObj instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> throughput = (Map<String, Object>) throughputObj;
        Assert.assertTrue("throughput should contain 'gain'", throughput.containsKey("gain"));
        Assert.assertTrue("throughput should contain 'status'", throughput.containsKey("status"));
    }

    @Test
    public void testYamlTrendReport() throws Exception {
        // Parse multiple simulations for trend report
        List<SimulationContext> stats = new ArrayList<>(TREND_LOGS.size());
        for (String file : TREND_LOGS) {
            stats.add(ParserFactory.getParser(getResourceFile(file)).parse());
        }

        // Generate YAML trend report
        Writer writer = new StringWriter();
        String reportPath = new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Assert file extension
        Assert.assertTrue("Report should have .yaml extension",
                reportPath.endsWith("index.yaml"));

        // Parse YAML to validate syntax
        String yamlContent = writer.toString();
        System.out.println("Generated YAML content:\n" + yamlContent);

        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Validate it's a trend report structure
        Assert.assertNotNull("YAML should parse successfully", data);

        // Validate trend report structure
        Assert.assertTrue("Should have trend section", data.containsKey("trend"));
        Map<String, Object> trend = (Map<String, Object>) data.get("trend");
        Assert.assertEquals("Should be trend_report type", "trend_report", trend.get("type"));
        Assert.assertNotNull("Should have simulations_count", trend.get("simulations_count"));

        // Validate simulations section
        Assert.assertTrue("Should have simulations section", data.containsKey("simulations"));
        List<Map<String, Object>> simulations = (List<Map<String, Object>>) data.get("simulations");
        Assert.assertNotNull("Simulations should not be null", simulations);
        Assert.assertEquals("Should have 3 simulations", 3, simulations.size());

        // Validate first simulation structure
        Map<String, Object> firstSim = simulations.get(0);
        Assert.assertNotNull("Should have simulation name", firstSim.get("simulation"));
        Assert.assertNotNull("Should have start date", firstSim.get("start"));
        Assert.assertNotNull("Should have duration", firstSim.get("duration"));
        Assert.assertNotNull("Should have throughput", firstSim.get("throughput"));
        Assert.assertNotNull("Should have averageMs", firstSim.get("averageMs"));
        Assert.assertNotNull("Should have count", firstSim.get("count"));
        Assert.assertNotNull("Should have successCount", firstSim.get("successCount"));
        Assert.assertNotNull("Should have errorCount", firstSim.get("errorCount"));

        // Validate apdex structure
        Assert.assertNotNull("Should have apdex", firstSim.get("apdex"));
        Map<String, Object> apdex = (Map<String, Object>) firstSim.get("apdex");
        Assert.assertNotNull("Apdex should have threshold", apdex.get("t"));
        Assert.assertNotNull("Apdex should have rating", apdex.get("rating"));
        Assert.assertNotNull("Apdex should have score", apdex.get("score"));

        // Validate percentiles
        Assert.assertNotNull("Should have min", firstSim.get("min"));
        Assert.assertNotNull("Should have max", firstSim.get("max"));
        Assert.assertNotNull("Should have p50", firstSim.get("p50"));
        Assert.assertNotNull("Should have p95", firstSim.get("p95"));
        Assert.assertNotNull("Should have p99", firstSim.get("p99"));
        Assert.assertNotNull("Should have avg", firstSim.get("avg"));

        // Validate requests structure
        Assert.assertNotNull("Should have requests", firstSim.get("requests"));
        Map<String, Object> requests = (Map<String, Object>) firstSim.get("requests");
        Assert.assertFalse("Requests should not be empty", requests.isEmpty());

        // Validate a request entry
        Map.Entry<String, Object> firstRequest = requests.entrySet().iterator().next();
        Map<String, Object> requestData = (Map<String, Object>) firstRequest.getValue();
        Assert.assertNotNull("Request should have name", requestData.get("name"));
        Assert.assertNotNull("Request should have averageMs", requestData.get("averageMs"));
        Assert.assertNotNull("Request should have rps", requestData.get("rps"));
        Assert.assertNotNull("Request should have count", requestData.get("count"));
    }

    @Test
    public void testYamlReportStatistics() throws Exception {
        // Parse simulation
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Generate YAML report
        Writer writer = new StringWriter();
        new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        // Parse YAML
        String yamlContent = writer.toString();
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);

        // Verify statistics fields are present
        Assert.assertTrue("Should have min field", data.containsKey("min"));
        Assert.assertTrue("Should have max field", data.containsKey("max"));
        Assert.assertTrue("Should have p50 field", data.containsKey("p50"));
        Assert.assertTrue("Should have p95 field", data.containsKey("p95"));
        Assert.assertTrue("Should have p99 field", data.containsKey("p99"));
        Assert.assertTrue("Should have avg field", data.containsKey("avg"));
        Assert.assertTrue("Should have count field", data.containsKey("count"));
        Assert.assertTrue("Should have successCount field",
                data.containsKey("successCount"));
        Assert.assertTrue("Should have errorCount field",
                data.containsKey("errorCount"));
    }

    @Test
    public void testYamlReportIndentationConsistency() throws Exception {
        // Parse simulation
        List<SimulationContext> stats = Collections.singletonList(
                ParserFactory.getParser(getResourceFile(SIM_LOG)).parse());

        // Generate YAML report
        Writer writer = new StringWriter();
        new Report(stats)
                .yamlReport(true)
                .setWriter(writer)
                .create();

        String yamlContent = writer.toString();
        String[] lines = yamlContent.split("\n");

        // Check that indentation is consistent (2 spaces per level)
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue; // Skip empty lines and comments
            }

            // Count leading spaces
            int spaces = 0;
            for (char c : line.toCharArray()) {
                if (c == ' ') {
                    spaces++;
                } else {
                    break;
                }
            }

            // Indentation should be multiple of 2
            Assert.assertTrue("Line should have even number of spaces for indentation: " + line,
                    spaces % 2 == 0);
        }
    }

    protected File getResourceFile(String filename) throws FileNotFoundException {
        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader.getResource(filename) == null) {
            throw new FileNotFoundException(filename);
        }
        return new File(Objects.requireNonNull(classLoader.getResource(filename)).getFile());
    }
}