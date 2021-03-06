/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.docker;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.AuthConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class AbstractDockerMojoTest {

  private static final String DOCKER_HOST = "testhost";
  private static final String DOCKER_CERT_PATH = "src/test/resources/certs";
  private static final String SERVER_ID = "testId";
  private static final String REGISTRY_URL = "https://my.docker.reg";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String CONFIGURATION_PROPERTY = "configuration";
  private static final String EMAIL_PROPERTY = "email";
  private static final String EMAIL = "user@host.domain";
  private static final String AUTHORIZATION_EXCEPTION =
      "Incomplete Docker registry authorization credentials.";
  private static final String DEFAULT_REGISTRY = "https://index.docker.io/v1/";

  @Mock
  private MavenSession session;

  @Mock
  private MojoExecution execution;

  @Mock
  private Settings settings;

  @Mock
  private DefaultDockerClient.Builder builder;

  @Captor
  private ArgumentCaptor<AuthConfig> authConfigCaptor;

  @InjectMocks
  private AbstractDockerMojo sut = new AbstractDockerMojo() {
    @Override
    protected void execute(DockerClient dockerClient) throws Exception {

    }

    @Override
    protected DefaultDockerClient.Builder getBuilder() throws DockerCertificateException {
      return builder;
    }
  };

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testDockerHostSet() throws Exception {
    ReflectionTestUtils.setField(sut, "dockerHost", DOCKER_HOST);
    ReflectionTestUtils.setField(sut, "dockerCertPath", DOCKER_CERT_PATH);

    sut.execute();

    verify(builder).uri(DOCKER_HOST);
    verify(builder).dockerCertificates(any(DockerCertificates.class));
 
  }

  @Test
  public void testSettingsNoUsername() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);

    final Server server = mockServer();

    server.setUsername(null);

    when(settings.getServer(SERVER_ID)).thenReturn(server);

    Throwable cause = null;

    try {
      sut.execute();
    } catch (MojoExecutionException exception) {
      cause = exception.getCause();
    }

    assertThat(cause).isNotNull().isExactlyInstanceOf(MojoExecutionException.class).
      hasMessageStartingWith(AUTHORIZATION_EXCEPTION);
  }

  @Test
  public void testSettingsNoPassword() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);

    final Server server = mockServer();

    server.setPassword(null);

    when(settings.getServer(SERVER_ID)).thenReturn(server);

    Throwable cause = null;

    try {
      sut.execute();
    } catch (MojoExecutionException exception) {
      cause = exception.getCause();
    }

    assertThat(cause).isNotNull().isExactlyInstanceOf(MojoExecutionException.class).
      hasMessageStartingWith(AUTHORIZATION_EXCEPTION);
  }


  @Test
  public void testSettingsNoConfiguration() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);

    final Server server = mockServer();

    server.setConfiguration(null);

    when(settings.getServer(SERVER_ID)).thenReturn(server);

    Throwable cause = null;

    try {
      sut.execute();
    } catch (MojoExecutionException exception) {
      cause = exception.getCause();
    }

    assertThat(cause).isNotNull().isExactlyInstanceOf(MojoExecutionException.class).
      hasMessageStartingWith(AUTHORIZATION_EXCEPTION);
  }


  @Test
  public void testSettingsNoEmail() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);

    final Server server = mockServer();

    server.setConfiguration(new Xpp3Dom(CONFIGURATION_PROPERTY));

    when(settings.getServer(SERVER_ID)).thenReturn(server);

    Throwable cause = null;

    try {
      sut.execute();
    } catch (MojoExecutionException exception) {
      cause = exception.getCause();
    }

    assertThat(cause).isNotNull().isExactlyInstanceOf(MojoExecutionException.class).
      hasMessageStartingWith(AUTHORIZATION_EXCEPTION);
  }

  @Test
  public void testAuthorizationConfiguration() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);

    when(settings.getServer(SERVER_ID)).thenReturn(mockServer());
    when(builder.authConfig(authConfigCaptor.capture())).thenReturn(builder);

    sut.execute();

    final AuthConfig authConfig = authConfigCaptor.getValue();
    assertThat(authConfig).isNotNull();
    assertThat(authConfig.email()).isEqualTo(EMAIL);
    assertThat(authConfig.password()).isEqualTo(PASSWORD);
    assertThat(authConfig.username()).isEqualTo(USERNAME);
    assertThat(authConfig.serverAddress()).isEqualTo(DEFAULT_REGISTRY);
  }

  @Test
  public void testAuthorizationConfigurationWithServerAddress() throws Exception {
    ReflectionTestUtils.setField(sut, "serverId", SERVER_ID);
    ReflectionTestUtils.setField(sut, "registryUrl", REGISTRY_URL);

    when(settings.getServer(SERVER_ID)).thenReturn(mockServer());
    when(builder.authConfig(authConfigCaptor.capture())).thenReturn(builder);

    sut.execute();

    final AuthConfig authConfig = authConfigCaptor.getValue();
    assertThat(authConfig).isNotNull();
    assertThat(authConfig.serverAddress()).isEqualTo(REGISTRY_URL);
  }

  private Server mockServer() {
    final Server server = new Server();
    server.setUsername(USERNAME);
    server.setPassword(PASSWORD);

    final Xpp3Dom email = new Xpp3Dom(EMAIL_PROPERTY);
    email.setValue(EMAIL);

    final Xpp3Dom configuration = new Xpp3Dom(CONFIGURATION_PROPERTY);
    configuration.addChild(email);

    server.setConfiguration(configuration);

    return server;
  }
}
