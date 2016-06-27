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
package com.sonar.orchestrator.build;

import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.locator.MavenLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * NOTE FOR IDE
 * Check that zip files are correctly loaded in IDE classpath during execution of tests
 */
public class ScannerForMSBuildInstallerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private FileSystem fileSystem;
  private ScannerForMSBuildInstaller installer;

  @Before
  public void init() {
    fileSystem = mock(FileSystem.class);
    installer = spy(new ScannerForMSBuildInstaller(fileSystem));
  }

  @Test
  public void install_embedded_version() throws Exception {
    File toDir = temp.newFolder();
    File script = installer.install(Version.create(ScannerForMSBuild.DEFAULT_SCANNER_VERSION), toDir, true);

    assertThat(script).isFile().exists();
    assertThat(script.getName()).contains("MSBuild.SonarQube.Runner.exe");
    assertThat(script.getParentFile().getName()).isEqualTo("sonar-scanner-msbuild-" + ScannerForMSBuild.DEFAULT_SCANNER_VERSION);

    verify(fileSystem, never()).locate(any(MavenLocation.class));
  }

  @Test
  public void do_not_install_twice() throws Exception {
    File toDir = temp.newFolder();

    installer.install(Version.create(ScannerForMSBuild.DEFAULT_SCANNER_VERSION), toDir, true);
    installer.install(Version.create(ScannerForMSBuild.DEFAULT_SCANNER_VERSION), toDir, true);

    verify(installer, times(1)).doInstall(Version.create(ScannerForMSBuild.DEFAULT_SCANNER_VERSION), toDir);
  }

  @Test
  public void maven_location() {
    MavenLocation location = ScannerForMSBuildInstaller.mavenLocation(Version.create("2.1-SNAPSHOT"));
    assertThat(location.getPackaging()).isEqualTo("zip");
    assertThat(location.version()).isEqualTo(Version.create("2.1-SNAPSHOT"));
    assertThat(location.getGroupId()).isEqualTo("org.sonarsource.scanner.msbuild");
    assertThat(location.getArtifactId()).isEqualTo("sonar-scanner-msbuild");
  }

  @Test
  public void corrupted_zip() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Fail to unzip scanner for MSBuild");
    installer.install(Version.create("corrupted"), temp.newFolder(), true);
  }
}
