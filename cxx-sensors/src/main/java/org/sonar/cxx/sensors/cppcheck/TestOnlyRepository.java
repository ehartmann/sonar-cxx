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
package org.sonar.cxx.sensors.cppcheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.cxx.sensors.utils.RuleRepository;

/**
 * {@inheritDoc}
 */
public class TestOnlyRepository extends RuleRepository {

  public static final String KEY = "testonly";
  private static final String NAME = "TestOnly";
  private static final String FILE = "/cppcheck.xml";

  /**
   * {@inheritDoc}
   */
  public TestOnlyRepository(ServerFileSystem fileSystem, RulesDefinitionXmlLoader xmlRuleLoader) {
    super(fileSystem, xmlRuleLoader, KEY, NAME, FILE);
  }

  @Override
  public void define(Context context) {
    Charset charset = StandardCharsets.UTF_8;
    NewRepository repository = context.createRepository(repositoryKey, "xxx").setName(repositoryName); // @todo unknown language

    var xmlLoader = new RulesDefinitionXmlLoader();
    if (!"".equals(repositoryFile)) {
      InputStream xmlStream = getClass().getResourceAsStream(repositoryFile);
      xmlLoader.load(repository, xmlStream, charset);

      for (var userExtensionXml : getExtensions(repositoryKey, "xml")) {
        try ( InputStream input = java.nio.file.Files.newInputStream(userExtensionXml.toPath())) {
          xmlRuleLoader.load(repository, input, charset);
        } catch (IOException | IllegalStateException e) {
          LOG.info("Cannot Load XML '{}'", e);
        }
      }
    }

    repository.done();
  }

}