/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.transport.jms;

import org.apache.cxf.service.model.EndpointInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class OldConfigTest extends AbstractJMSTester {

    @BeforeClass
    public static void createAndStartBroker() throws Exception {
        startBroker(new JMSBrokerSetup("tcp://localhost:" + JMS_PORT));
    }

    @Test
    public void testUsernameAndPassword() throws Exception {
        EndpointInfo ei = setupServiceInfo("http://cxf.apache.org/hello_world_jms", "/wsdl/jms_test.wsdl",
                "HelloWorldService", "HelloWorldPort");
        JMSOldConfigHolder holder = new JMSOldConfigHolder();
        JMSConfiguration config = holder.createJMSConfigurationFromEndpointInfo(bus, ei, target, false);
        String username = config.getJndiConfig().getConnectionUserName();
        String password = config.getJndiConfig().getConnectionPassword();
        Assert.assertEquals("User name does not match." , "testUser", username);
        Assert.assertEquals("Password does not match." , "testPassword", password);
    }
}
