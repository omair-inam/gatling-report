# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Gatling Report is a Java-based reporting tool that parses Gatling simulation log files and generates HTML reports with Plotly charts or CSV output. It supports multiple Gatling log formats (2.1, 2.3, 2.4, 2.5, 3.0-3.10) and can create simulation reports, differential reports, and trend reports.

## Build and Development Commands

### Building the Project
```bash
# Build the all-in-one JAR with all dependencies
mvn package

# The JAR will be created at:
# ./target/gatling-report-VERSION-capsule-fat.jar
```

### Running Tests
```bash
# Run all unit tests
mvn test

# Run a specific test class
mvn test -Dtest=TestParser
mvn test -Dtest=TestReport
```

### Cleaning and Rebuilding
```bash
# Clean and rebuild
mvn clean package
```

## Architecture

### Core Components

- **SimulationParser Hierarchy**: Different parser implementations for various Gatling log format versions
  - `SimulationParser` (abstract base)
  - `SimulationParserV2`, `SimulationParserV23`, `SimulationParserV3`, `SimulationParserV32`, `SimulationParserV34`, `SimulationParserV35`
  - `ParserFactory` determines which parser to use based on log file format

- **Report Generation**:
  - `Report.java` - Main report generator using Mustache templates
  - Templates in `src/main/resources/html/`:
    - `simulation.mustache` - Single simulation report
    - `diff.mustache` - Differential report comparing two simulations
    - `trend.mustache` - Trend report for multiple simulations

- **Statistical Processing**:
  - `RequestStat.java` - Calculates statistics per request (min, max, percentiles, etc.)
  - `SimulationContext.java` - Holds parsed simulation data
  - `Apdex.java` - Calculates Apdex scores and ratings

- **Main Entry Point**: `App.java` - CLI application using JCommander for argument parsing

## Key Technical Details

- **Java Version**: Requires Java 1.8+
- **Build System**: Maven
- **Packaging**: Uses Capsule for creating self-contained executable JARs
- **Main Class**: `org.nuxeo.tools.gatling.report.App`
- **Dependencies**:
  - Mustache for templating
  - JCommander for CLI parsing
  - Apache Commons Math for statistics
  - SimpleCSV for CSV output
  - Plotly (bundled JS) for charts

## Usage Examples

```bash
# Generate CSV stats to stdout
java -jar target/gatling-report-*-capsule-fat.jar path/to/simulation.log

# Generate HTML simulation report
java -jar target/gatling-report-*-capsule-fat.jar path/to/simulation.log -o /path/to/report/

# Generate differential report (2 files)
java -jar target/gatling-report-*-capsule-fat.jar ref/simulation.log challenger/simulation.log -o /path/to/report/

# Generate trend report (3+ files)
java -jar target/gatling-report-*-capsule-fat.jar sim1.log sim2.log sim3.log -o /path/to/report/
```