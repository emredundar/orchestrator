/*
 * Orchestrator
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package com.sonar.orchestrator.util;

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipUtilsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File zip = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/util/ZipUtilsTest/shouldUnzipFile.zip"));

  @Test
  public void shouldUnzipFile() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.unzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void shouldUnzipTwice() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.unzip(zip, toDir);
    ZipUtils.unzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }

  @Test
  public void shouldUnzipJava() throws IOException {
    File toDir = temp.newFolder();

    ZipUtils.javaUnzip(zip, toDir);

    assertThat(toDir.list()).hasSize(3);
  }
}
