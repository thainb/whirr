/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.cli.command;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.service.Cluster;
import org.apache.whirr.service.ClusterSpec;
import org.apache.whirr.service.Service;
import org.apache.whirr.service.ServiceFactory;
import org.apache.whirr.ssh.KeyPair;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.matchers.StringContains;

public class LaunchClusterCommandTest {

  private ByteArrayOutputStream outBytes;
  private PrintStream out;
  private ByteArrayOutputStream errBytes;
  private PrintStream err;

  @Before
  public void setUp() {
    outBytes = new ByteArrayOutputStream();
    out = new PrintStream(outBytes);

    errBytes = new ByteArrayOutputStream();
    err = new PrintStream(errBytes);
  }
  @Test
  public void testInsufficientArgs() throws Exception {
    LaunchClusterCommand command = new LaunchClusterCommand();
    int rc = command.run(null, null, err, Collections.<String>emptyList());
    assertThat(rc, is(-1));
    assertThat(errBytes.toString(), containsUsageString());
  }
  
  private Matcher<String> containsUsageString() {
    return StringContains.containsString("Usage: whirr launch-cluster [OPTIONS]");
  }
  
  @Test
  public void testAllOptions() throws Exception {
    
    ServiceFactory factory = mock(ServiceFactory.class);
    Service service = mock(Service.class);
    Cluster cluster = mock(Cluster.class);
    when(factory.create((String) any())).thenReturn(service);
    when(service.launchCluster((ClusterSpec) any())).thenReturn(cluster);
    
    LaunchClusterCommand command = new LaunchClusterCommand(factory);
    Map<String, File> keys = KeyPair.generateTemporaryFiles();
    
    int rc = command.run(null, out, null, Lists.newArrayList(
        "--service-name", "test-service",
        "--cluster-name", "test-cluster",
        "--instance-templates", "1 role1+role2,2 role3",
        "--provider", "rackspace",
        "--identity", "myusername", "--credential", "mypassword",
        "--private-key-file", keys.get("private").getAbsolutePath(),
        "--version", "version-string"
        ));
    
    assertThat(rc, is(0));

    Configuration conf = new PropertiesConfiguration();
    conf.addProperty("whirr.version", "version-string");

    ClusterSpec expectedClusterSpec = ClusterSpec.withNoDefaults(conf);
    expectedClusterSpec.setInstanceTemplates(Lists.newArrayList(
        new ClusterSpec.InstanceTemplate(1, ImmutableSet.of("role1", "role2")),
        new ClusterSpec.InstanceTemplate(2, ImmutableSet.of("role3"))
    ));
    expectedClusterSpec.setServiceName("test-service");
    expectedClusterSpec.setProvider("rackspace");
    expectedClusterSpec.setIdentity("myusername");
    expectedClusterSpec.setCredential("mypassword");
    expectedClusterSpec.setClusterName("test-cluster");
    expectedClusterSpec.setPrivateKey(keys.get("private"));
    expectedClusterSpec.setPublicKey(keys.get("public"));
    
    verify(factory).create("test-service");
    
    verify(service).launchCluster(expectedClusterSpec);
    
    assertThat(outBytes.toString(), containsString("Started cluster of 0 instances"));
    
  }
}
