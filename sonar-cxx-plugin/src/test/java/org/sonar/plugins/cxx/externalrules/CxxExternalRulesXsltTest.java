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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import static org.fest.assertions.Assertions.assertThat;
import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.apache.commons.io.FileUtils;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.cxx.TestUtils;

public class CxxExternalRulesXsltTest {

  private FileSystem fs;

  @Before
  public void setUp() {
    fs = TestUtils.mockFileSystem();
  }

  @Test
  public void shouldReportNothingWhenNoReportFound() {
    SensorContextTester context = SensorContextTester.create(fs.baseDir());
    Settings settings = new Settings();
    settings.setProperty(CxxExternalRulesSensor.REPORT_PATH_KEY, "notexistingpath");
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.STYLESHEET_KEY, "notexistingpath");
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.SOURCE_KEY, "notexistingpath");
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.OUTPUT_KEY, "notexistingpath");
    CxxExternalRulesSensor sensor = new CxxExternalRulesSensor(settings);

    sensor.execute(context);

    File reportAfter = new File("notexistingpath");
    Assert.assertFalse("The output file does exist!", reportAfter.exists() && reportAfter.isFile());
  }

  @Test
  public void transformReport_shouldTransformReport()
    throws java.io.IOException, javax.xml.transform.TransformerException {
    System.out.print("Starting transformReport_shouldTransformReport");
    String stylesheetFile = "externalrules-reports" + File.separator + "externalrules-xslt-stylesheet.xslt";
    String inputFile = "externalrules-reports" + File.separator + "externalrules-xslt-input.xml";
    String outputFile = "externalrules-reports" + File.separator + "externalrules-xslt-output.xml";

    Settings settings = new Settings();
    settings.setProperty(CxxExternalRulesSensor.REPORT_PATH_KEY, "externalrules-xslt-output.xml");
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.STYLESHEET_KEY, stylesheetFile);
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.SOURCE_KEY, inputFile);
    settings.setProperty(CxxExternalRulesSensor.SONAR_CXX_OTHER_XSLT_KEY + "1" + CxxExternalRulesSensor.OUTPUT_KEY, outputFile);
    CxxExternalRulesSensor sensor = new CxxExternalRulesSensor(settings);

    sensor.transformFiles(fs.baseDir());

    File reportBefore = new File(fs.baseDir() + "/" + inputFile);
    File reportAfter = new File(fs.baseDir() + "/" + outputFile);
    Assert.assertTrue("The output file does not exist!", reportAfter.exists() && reportAfter.isFile());
    Assert.assertTrue("The input and output file is equal!", !FileUtils.contentEquals(reportBefore, reportAfter));
  }
}
