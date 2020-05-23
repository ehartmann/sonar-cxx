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
package org.sonar.cxx.sensors.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Utility class holding various, well, utilities
 */
public final class CxxUtils {

  public static final Pattern EOL_PATTERN = Pattern.compile("\\R");
  private static final Logger LOG = Loggers.get(CxxUtils.class);

  private CxxUtils() {
    // only static methods
  }

  /**
   * <p>
   * Gets the stack trace from a Throwable as a String.</p>
   *
   * @param throwable the <code>Throwable</code> to be examined
   * @return the stack trace as generated by the exception's printStackTrace(PrintWriter) method
   */
  public static String getStackTrace(final Throwable throwable) {
    return ExceptionUtils.getStackTrace(throwable);
  }

  /**
   * validateRecovery
   *
   * @param ex
   * @param config
   */
  public static void validateRecovery(String msg, Exception ex, Configuration config) {
    var message = msg;
    var cause = ex.getCause();
    if (cause != null) {
      message += ", cause='" + cause + "'";
    }
    Optional<Boolean> recovery = config.getBoolean(CxxReportSensor.ERROR_RECOVERY_KEY);
    if (recovery.isPresent() && recovery.get()) {
      LOG.error(message + ", skipping");
      return;
    }
    LOG.info("Error recovery is disabled");
    LOG.error(message + ", stop analysis");
    throw new IllegalStateException(ex.getMessage(), ex.getCause());
  }

  /**
   * Utility method mainly used to resolve location of reports.
   *
   * IMPORTANT: Method supports also Ant patterns in filename
   *
   * @param baseDir base directory used to make relative Ant pattern absolute
   * @param antPattern relative or absolute path (could also be Ant pattern)
   * @return normalized absolute path (or null in case of error)
   */
  @CheckForNull
  public static String resolveAntPath(final String baseDir, @Nullable final String antPattern) {
    if (antPattern != null && !antPattern.isBlank()) {
      String path;
      if (new File(antPattern).isAbsolute()) {
        path = antPattern;
      } else {
        path = baseDir + File.separator + antPattern;
      }
      return PathUtils.sanitize(path);
    }
    return null;
  }

  /**
   * Use the given context object in order to get a list of Ant patterns referenced by key reportPathKey. Apply
   * context.fileSystem().baseDir() in order to make relative Ant patterns to absolute ones. Resolve Ant patterns and
   * returns the list of existing files.
   *
   * @param context sensor context
   * @param reportPathKey configuration key for files (CSV list of Ant patterns)
   *
   * @return List<File> matching file list
   */
  public static List<File> getFiles(SensorContext context, String reportPathKey) {
    String[] reportPaths = context.config().getStringArray(reportPathKey);
    if (reportPaths == null || reportPaths.length == 0) {
      LOG.info("Undefined report path value for key '{}'", reportPathKey);
      return Collections.emptyList();
    }

    var normalizedReportPaths = new ArrayList<String>();
    for (var reportPath : reportPaths) {
      String normalizedPath = resolveAntPath(context.fileSystem().baseDir().getAbsolutePath(), reportPath.trim());
      if (normalizedPath != null) {
        normalizedReportPaths.add(normalizedPath);
      } else {
        LOG.debug("Not a valid report path '{}'", reportPath);
      }
    }
    LOG.debug("Search report(s) in path(s): '{}'", String.join(", ", normalizedReportPaths));

    // Includes array cannot contain null elements
    var directoryScanner = new DirectoryScanner();
    directoryScanner.setIncludes(normalizedReportPaths.toArray(new String[normalizedReportPaths.size()]));
    directoryScanner.scan();
    String[] existingReportPaths = directoryScanner.getIncludedFiles();

    if (existingReportPaths.length == 0) {
      LOG.warn("Property '{}': cannot find any files matching the Ant pattern(s) '{}'", reportPathKey,
               String.join(", ", normalizedReportPaths));
      return Collections.emptyList();
    }

    LOG.info("Import '{}' report file(s)", existingReportPaths.length);
    return Arrays.stream(existingReportPaths).map(File::new).collect(Collectors.toList());
  }

}
