/*
 * Sonar Cxx Plugin, open source software quality management tool.
 * Copyright (C) 2010 - 2011, Neticoa SAS France - Tous droits reserves.
 * Author(s) : Franck Bonin, Neticoa SAS France.
 *
 * Sonar Cxx Plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar Cxx Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar Cxx Plugin; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.cxx.externalrules;


import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.Violation;
import org.sonar.plugins.cxx.TestUtils;

public class CxxExternalRulesSensorTest {
  private CxxExternalRulesSensor sensor;
  private SensorContext context;
  private Project project;

  public static RuleFinder mockRuleFinder(){
    Rule ruleMock = Rule.create("cxxexternal", "cxxexternal-unusedFunction", "unusedFunction");
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey((String) anyObject(),
        (String) anyObject())).thenReturn(ruleMock);
    when(ruleFinder.find((RuleQuery) anyObject())).thenReturn(ruleMock);
    return ruleFinder;
  }
  
  @Before
  public void setUp() {
    project = TestUtils.mockProject();
    RuleFinder ruleFinder = TestUtils.mockRuleFinder();
    RulesProfile profile = mock(RulesProfile.class);
    sensor = new CxxExternalRulesSensor(ruleFinder, new Settings(), profile);
    context = mock(SensorContext.class);
    Resource resourceMock = mock(Resource.class);
    when(context.getResource((Resource)anyObject())).thenReturn(resourceMock);
  }

  @Test
  public void shouldReportCorrectViolations() {
    sensor.analyse(project, context);
    verify(context, times(2)).saveViolation(any(Violation.class));
  }
}
