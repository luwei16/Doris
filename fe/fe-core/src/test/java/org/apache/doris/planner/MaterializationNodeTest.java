// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.planner;

import org.apache.doris.resource.Tag;
import org.apache.doris.resource.computegroup.ComputeGroup;
import org.apache.doris.system.Backend;
import org.apache.doris.system.BackendHbResponse;
import org.apache.doris.thrift.TNodeInfo;
import org.apache.doris.thrift.TPaloNodesInfo;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

public class MaterializationNodeTest {
    @Test
    public void testCreateQueryAvailableNodesInfo() {
        Backend queryAvailableBackend = createBackend(1, "127.0.0.1", 8061);

        Backend queryDisabledBackend = createBackend(2, "127.0.0.2", 8062);
        queryDisabledBackend.setQueryDisabled(true);

        Backend shutdownBackend = createBackend(3, "127.0.0.3", 8060);
        BackendHbResponse shutdownHeartbeat = new BackendHbResponse(
                shutdownBackend.getId(), 9060, 8040, shutdownBackend.getBrpcPort(),
                System.currentTimeMillis(), System.currentTimeMillis(), "test", Tag.VALUE_MIX,
                0, 0, true, 8070);
        shutdownBackend.handleHbResponse(shutdownHeartbeat, false);

        Backend deadBackend = createBackend(4, "127.0.0.4", 8063);
        deadBackend.setAlive(false);

        ComputeGroup computeGroup = Mockito.mock(ComputeGroup.class);
        Mockito.when(computeGroup.getBackendList()).thenReturn(Arrays.asList(
                queryAvailableBackend, queryDisabledBackend, shutdownBackend, deadBackend));

        TPaloNodesInfo nodesInfo = MaterializationNode.createQueryAvailableNodesInfo(computeGroup);

        Assert.assertEquals(1, nodesInfo.getNodesSize());
        TNodeInfo nodeInfo = nodesInfo.getNodes().get(0);
        Assert.assertEquals(queryAvailableBackend.getId(), nodeInfo.getId());
        Assert.assertEquals(queryAvailableBackend.getHost(), nodeInfo.getHost());
        Assert.assertEquals(queryAvailableBackend.getBrpcPort(), nodeInfo.getAsyncInternalPort());
    }

    private static Backend createBackend(long id, String host, int brpcPort) {
        Backend backend = new Backend(id, host, 9050);
        backend.setAlive(true);
        backend.setBrpcPort(brpcPort);
        return backend;
    }
}
