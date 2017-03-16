/*
 * Sonar C++ Plugin (Community)
 * Copyright (C) 2010-2017 SonarOpenCommunity
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
package org.sonar.plugins.cxx.externalrules;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.cxx.CxxLanguage;
import org.sonar.plugins.cxx.utils.CxxMetrics;
import org.sonar.plugins.cxx.utils.CxxReportSensor;
import org.sonar.plugins.cxx.utils.CxxUtils;
import org.sonar.plugins.cxx.utils.StaxParser;

/**
 * Custom Rule Import, all static analysis are supported.
 *
 * @author jorge costa, stefan weiser
 */
public class CxxExternalRulesSensor extends CxxReportSensor {
  public static final Logger LOG = Loggers.get(CxxExternalRulesSensor.class);
  public static final String REPORT_PATH_KEY = "sonar.cxx.other.reportPath";
  private static final String SONAR_CXX_XSLT_KEY = "sonar.cxx.xslt.";
  private static final String STYLESHEET_KEY = ".stylesheet";
  private static final String SOURCE_KEY = ".source";
  private static final String OUTPUT_KEY = ".output";
  private Settings settings;

  /**
   * {@inheritDoc}
   */
  public CxxExternalRulesSensor(Settings settings) {
    super(settings, CxxMetrics.EXTERNAL);
    this.settings = settings;
  }

  @Override
  protected String reportPathKey() {
    return REPORT_PATH_KEY;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.onlyOnLanguage(CxxLanguage.KEY).name("CxxExternalRulesSensor");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(SensorContext context) {
    try {
      transformFiles();
    } catch (Exception e) {
      String msg = new StringBuilder()
        .append("Cannot transform report files: '")
        .append(e)
        .append("'")
        .toString();
      LOG.error(msg);
      CxxUtils.validateRecovery(e, this.settings);
    }
    super.execute(context);
  }

  @Override
  public void processReport(final SensorContext context, File report) throws XMLStreamException, IOException, URISyntaxException, TransformerException {
    LOG.debug("Parsing 'other' format");

    StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

      /**
       * {@inheritDoc}
       */
      @Override
      public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
        rootCursor.advance();

        SMInputCursor errorCursor = rootCursor.childElementCursor("error");
        while (errorCursor.getNext() != null) {
          String file = errorCursor.getAttrValue("file");
          String line = errorCursor.getAttrValue("line");
          String id = errorCursor.getAttrValue("id");
          String msg = errorCursor.getAttrValue("msg");

          saveUniqueViolation(context, CxxExternalRuleRepository.KEY, file, line, id, msg);
        }
      }
    });

    parser.parse(report);
  }

  private void transformFiles() throws TransformerException {
    for(int i = 1; i < 100; i++) {
      String stylesheetKey = SONAR_CXX_XSLT_KEY + i + STYLESHEET_KEY;
      String sourceKey = SONAR_CXX_XSLT_KEY + i + SOURCE_KEY;
      String outputKey = SONAR_CXX_XSLT_KEY + i + OUTPUT_KEY;

      String stylesheet = FilenameUtils.normalize(settings.getString(SONAR_CXX_XSLT_KEY + i + STYLESHEET_KEY));
      String[] sources = settings.getStringArray(SONAR_CXX_XSLT_KEY + i + SOURCE_KEY);
      String[] outputs = settings.getStringArray(SONAR_CXX_XSLT_KEY + i + OUTPUT_KEY);

      if (sources.length != outputs.length) {
        LOG.error("Number of source XML files is not equal to the the number of output files.");
      } else if ((stylesheet != null) ||
        ((sources != null) && (sources.length > 0)) ||
        ((outputs != null) && (outputs.length > 0))) {
        if (stylesheet == null) {
          LOG.error(SONAR_CXX_XSLT_KEY + i + STYLESHEET_KEY + " is not defined.");
        } else if (sources == null) {
          LOG.error(SONAR_CXX_XSLT_KEY + i + SOURCE_KEY + " file is not defined.");
        } else if (outputs == null) {
          LOG.error(SONAR_CXX_XSLT_KEY + i + OUTPUT_KEY + " is not defined.");
        } else {
          File stylesheetFile = new File(stylesheet);
          if (stylesheetFile.isAbsolute()) {
            for (int j = 0; j < sources.length; j++) {
              transformFile(stylesheetFile, sources[j], outputs[j]);
            }
          }
        }
      }
    }
  }

  private void transformFile(File stylesheetFile, String source, String output) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    Source xslt = new StreamSource(stylesheetFile);
    Transformer transformer = factory.newTransformer(xslt);

    File sourceFile = new File(source);
    File outputFile = new File(output);
    transformer.transform(new StreamSource(sourceFile), new StreamResult(outputFile));
  }
}
