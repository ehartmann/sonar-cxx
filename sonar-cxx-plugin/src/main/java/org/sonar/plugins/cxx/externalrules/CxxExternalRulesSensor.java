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
import org.sonar.plugins.cxx.utils.StaxParser;

/**
 * Custom Rule Import, all static analysis are supported.
 *
 * @author jorge costa
 */
public class CxxExternalRulesSensor extends CxxReportSensor {
  public static final Logger LOG = Loggers.get(CxxExternalRulesSensor.class);
  public static final String REPORT_PATH_KEY = "sonar.cxx.other.reportPath";
  private static final String TRANSFORM_XSLT_SCRIPT_KEY = "sonar.cxx.other.transformXsltScript";
  private static final String SONAR_WORKING_DIRECTORY_KEY = "sonar.working.directory";
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

    String transformXsltScript = FilenameUtils.normalize(settings.getString(TRANSFORM_XSLT_SCRIPT_KEY));
    if (transformXsltScript != null) {
      File transformFile = new File(transformXsltScript);
      if (transformFile.isAbsolute()) {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(transformFile);
        Transformer transformer = factory.newTransformer(xslt);

        File transformedReport = new File(SONAR_WORKING_DIRECTORY_KEY + "/other_transformed.xml");
        transformer.transform(new StreamSource(report), new StreamResult(transformedReport));
        report = transformedReport;
      }
    }

    parser.parse(report);
  }
}
