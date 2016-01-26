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
package com.sonar.orchestrator.container;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.config.FileSystem;
import com.sonar.orchestrator.util.NetworkUtils;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * Note for executing these tests in IDE: be sure that zip files are included in classpath (see Preferences>Compiler
 * in Intellij Idea)
 */
public class SonarDownloaderTest {

  private SonarDownloader downloader;
  private FileSystem fileSystem;
  private File workspace;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(NetworkUtils.getNextAvailablePort());

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void initDirectories() throws IOException {
    File root = FileUtils.toFile(getClass().getResource("/com/sonar/orchestrator/container/SonarDownloaderTest/"));
    workspace = temp.newFolder();
    fileSystem = new FileSystem(Configuration.builder()
      .setProperty("maven.localRepository", new File(root, "maven"))
      .setProperty("orchestrator.sonarInstallsDir", new File(root, "user"))
      .setProperty("orchestrator.workspaceDir", workspace)
      .build());
    downloader = new SonarDownloader(fileSystem, Orchestrator.builderEnv().setSonarVersion("DEV").build().getConfiguration());
    Zips.upToDateSnapshots.clear();
  }

  /**
   * The maven local repository is supposed to contain the most up-to-date Sonar distribution.
   */
  @Test
  public void shouldSearchInMavenLocalRepository() {
    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.0")));

    assertThat(zip).exists().isFile();
    assertThat(zip.getName()).isEqualTo("sonarqube-5.0.zip");
  }

  @Test
  public void shouldAddReleasesToUserCache() {
    // download from maven repository then copy to user cache
    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.0")));

    assertThat(zip).exists();
    assertThat(zip.getName()).isEqualTo("sonarqube-5.0.zip");
    assertThat(zip.getParentFile()).isEqualTo(fileSystem.sonarInstallsDir());
  }

  @Test
  public void shouldSearchReleasesInUserCache() {
    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.0")));

    assertThat(zip).isFile().exists();
    assertThat(zip.getName()).isEqualTo("sonarqube-5.0.zip");
    assertThat(zip.getParentFile()).isEqualTo(fileSystem.sonarInstallsDir());
  }

  @Test
  public void shouldFailIfZipNotFound() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can not find sonarqube-dummy.zip");

    downloader.downloadZip(new SonarDistribution(Version.create("dummy")));
  }

  @Test
  public void shouldFailDownloadOfSnapshot() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Can not find sonarqube-dummy-SNAPSHOT.zip");

    downloader.downloadZip(new SonarDistribution(Version.create("dummy-SNAPSHOT")));
  }

  @Test
  public void shouldNotAddSnapshotsToUserCache() {
    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.2-SNAPSHOT")));

    assertThat(zip).exists();
    assertThat(new File(fileSystem.sonarInstallsDir(), "sonarqube-5.2-SNAPSHOT.zip")).doesNotExist();
  }

  @Test
  public void shouldAddSnapshotsToWorkspaceCache() {
    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.2-SNAPSHOT")));

    assertThat(zip.getName()).isEqualTo("sonarqube-5.2-SNAPSHOT.zip");
    assertThat(zip.getParentFile()).isEqualTo(workspace);

    assertThat(new File(fileSystem.workspace(), "sonarqube-5.2-SNAPSHOT.zip")).exists();
  }

  @Test
  public void shouldDropOldSnapshots() throws IOException {
    File snapshotFile = new File(workspace, "sonarqube-5.2-SNAPSHOT.zip");
    FileUtils.writeStringToFile(snapshotFile, "old version of zip");

    File zip = downloader.downloadZip(new SonarDistribution(Version.create("5.2-SNAPSHOT")));

    assertThat(zip).isEqualTo(snapshotFile);
    assertThat(zip).exists();
    assertThat(FileUtils.readFileToString(zip)).isNotEqualTo("old version of zip");
  }

  @Test
  public void shouldNotDownloadWithIncorrectMD5() {
    SonarDistribution sonar45999 = new SonarDistribution(Version.create("4.5.999"));

    downloader = spy(downloader);
    int port = wireMockRule.port();
    doReturn(String.format("http://localhost:%d/%s", port, sonar45999.zipFilename())).when(downloader).getDownloadUrl(Mockito.any(SonarDistribution.class));

    stubFor(get(urlEqualTo("/" + sonar45999.zipFilename())).willReturn(aResponse().withBody("A")));
    stubFor(get(urlEqualTo("/" + sonar45999.zipFilename() + ".md5")).willReturn(aResponse().withBody("aa")));

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("File downloaded has an incorrect MD5 checksum.");

    downloader.downloadZip(sonar45999);
  }
}
