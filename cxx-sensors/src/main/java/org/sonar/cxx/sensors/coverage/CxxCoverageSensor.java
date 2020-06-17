/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2020 SonarOpenCommunity
 * http://github.com/SonarOpenCommunity/sonar-cxx
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.cxx.sensors.coverage;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.cxx.sensors.coverage.bullseye.BullseyeParser;
import org.sonar.cxx.sensors.coverage.cobertura.CoberturaParser;
import org.sonar.cxx.sensors.coverage.ctc.TestwellCtcTxtParser;
import org.sonar.cxx.sensors.coverage.vs.VisualStudioParser;
import org.sonar.cxx.sensors.utils.CxxReportSensor;
import org.sonar.cxx.sensors.utils.CxxUtils;
import org.sonar.cxx.sensors.utils.EmptyReportException;
import org.sonar.cxx.sensors.utils.ReportException;

/**
 * {@inheritDoc}
 */
public class CxxCoverageSensor extends CxxReportSensor {

  public static final String REPORT_PATH_KEY = "sonar.cxx.coverage.reportPaths";

  private static final Logger LOG = Loggers.get(CxxCoverageSensor.class);

  private final List<CoverageParser> parsers = new LinkedList<>();

  /**
   * {@inheritDoc}
   *
   * @param cache for all coverage data
   */
  public CxxCoverageSensor() {
    parsers.add(new CoberturaParser());
    parsers.add(new BullseyeParser());
    parsers.add(new VisualStudioParser());
    parsers.add(new TestwellCtcTxtParser());
  }

  public static List<PropertyDefinition> properties() {
    return Collections.unmodifiableList(Arrays.asList(
      PropertyDefinition.builder(getReportPathsKey())
        .name("Coverage report(s)")
        .description("List of paths to reports containing coverage data, relative to projects root."
                       + " The values are separated by commas."
                       + " See <a href='https://github.com/SonarOpenCommunity/sonar-cxx/wiki/Get-code-coverage-metrics'>"
                     + "here</a> for supported formats.")
        .category("CXX External Analyzers")
        .subCategory("Coverage")
        .onQualifiers(Qualifiers.PROJECT)
        .multiValues(true)
        .build()
    ));
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("CXX coverage report import")
      .onlyOnLanguage("cxx")
      .onlyWhenConfiguration(conf -> conf.hasKey(getReportPathsKey()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void executeImpl() {
    List<File> reports = getReports(getReportPathsKey());
    for (var report : reports) {
      executeReport(report);
    }
  }

  protected static String getReportPathsKey() {
    return REPORT_PATH_KEY;
  }

  /**
   * @param report to read
   */
  protected void executeReport(File report) {
    try {
      LOG.info("Processing report '{}'", report);
      processReport(report);
      LOG.info("Processing successful");
    } catch (ReportException e) {
      var msg = e.getMessage() + ", report='" + report + "'";
      CxxUtils.validateRecovery(msg, e, context.config());
    }
  }

  protected void processReport(File report) throws ReportException {
    Map<String, CoverageMeasures> measuresTotal = new HashMap<>();

    for (var parser : parsers) {
      try {
        var measuresForReport = new HashMap<String, CoverageMeasures>();
        try {
          parser.parse(report, measuresForReport);
        } catch (XMLStreamException e) {
          throw new EmptyReportException("Coverage report" + report + "cannot be parsed by" + parser, e);
        }

        if (measuresForReport.isEmpty()) {
          throw new EmptyReportException("Coverage report " + report + " result is empty (parsed by " + parser + ")");
        }

        measuresTotal.putAll(measuresForReport);
        LOG.info("Added coverage report '{}' (parsed by: {})", report, parser);

        saveMeasures(measuresTotal);
        break;
      } catch (EmptyReportException e) {
        LOG.debug("Report is empty {}", e.getMessage());
      }
    }
  }

  private void saveMeasures(Map<String, CoverageMeasures> coverageMeasures) {
    for (var entry : coverageMeasures.entrySet()) {
      final String filePath = PathUtils.sanitize(entry.getKey());
      if (filePath != null) {
        InputFile cxxFile = getInputFileIfInProject(filePath);
        LOG.debug("save coverage measure for file: '{}' cxxFile = '{}'", filePath, cxxFile);

        if (cxxFile != null) {

          NewCoverage newCoverage = context.newCoverage().onFile(cxxFile);

          Collection<CoverageMeasure> measures = entry.getValue().getCoverageMeasures();
          LOG.debug("Saving '{}' coverage measures for file '{}'", measures.size(), filePath);

          measures.forEach((CoverageMeasure measure) -> checkCoverage(newCoverage, measure));

          try {
            newCoverage.save();
            LOG.debug("Saved '{}' coverage measures for file '{}'", measures.size(), filePath);
          } catch (RuntimeException e) {
            var msg = "Cannot save coverage measures for file '" + filePath + "'";
            CxxUtils.validateRecovery(msg, e, context.config());
          }
        } else {
          LOG.debug("Cannot find the file '{}', ignoring coverage measures", filePath);
          if (filePath.startsWith(context.fileSystem().baseDir().getAbsolutePath())) {
            LOG.warn("Cannot find the file '{}', ignoring coverage measures", filePath);
          }
        }
      } else {
        LOG.debug("Cannot sanitize file path '{}'", entry.getKey());
      }
    }
  }

  /**
   * @param newCoverage
   * @param measure
   */
  private void checkCoverage(NewCoverage newCoverage, CoverageMeasure measure) {
    try {
      newCoverage.lineHits(measure.getLine(), measure.getHits());
      newCoverage.conditions(measure.getLine(), measure.getConditions(), measure.getCoveredConditions());
      LOG.debug("line '{}' Hits '{}' Conditions '{}:{}'", measure.getLine(), measure.getHits(),
                measure.getConditions(), measure.getCoveredConditions());
    } catch (RuntimeException e) {
      var msg = "Cannot save Conditions Hits for Line '" + measure.getLine() + "'";
      CxxUtils.validateRecovery(msg, e, context.config());
    }
  }

}
