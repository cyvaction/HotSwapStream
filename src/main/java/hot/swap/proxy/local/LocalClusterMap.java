/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hot.swap.proxy.local;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import hot.swap.proxy.stream.nimbus.ServiceHandler;
import hot.swap.proxy.stream.supervisor.Supervisor;
import hot.swap.proxy.stream.topserver.TopologyServer;
import hot.swap.proxy.utils.PathUtils;
import hot.swap.proxy.zk.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalClusterMap {

    public static Logger LOG = LoggerFactory.getLogger(LocalClusterMap.class);

    private ServiceHandler nimbus;

    private Factory zookeeper;

    private Map conf;

    private List<String> tmpDir;

    private Supervisor supervisor;

    private TopologyServer topologyServer;

    public ServiceHandler getNimbus() {
        return nimbus;
    }

    public void setNimbus(ServiceHandler nimbus) {
        this.nimbus = nimbus;
    }

    public Factory getZookeeper() {
        return zookeeper;
    }

    public void setZookeeper(Factory zookeeper) {
        this.zookeeper = zookeeper;
    }

    public Map getConf() {
        return conf;
    }

    public void setConf(Map conf) {
        this.conf = conf;
    }


    public TopologyServer getTopologyServer(){
        return topologyServer;
    }

    public void setTopologyServer(TopologyServer topologyServer){
        this.topologyServer = topologyServer;
    }

    public List<String> getTmpDir() {
        return tmpDir;
    }

    public void setTmpDir(List<String> tmpDir) {
        this.tmpDir = tmpDir;
    }

    public void setSupervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public void clean() {

        if (supervisor != null) {
            supervisor.shutdown();
        }

        if (nimbus != null) {
            nimbus.shutdown();
        }

        if (zookeeper != null)
            zookeeper.shutdown();

        // it will hava a problem:
        // java.io.IOException: Unable to delete file:
        // {TmpPath}\{UUID}\version-2\log.1
        if (tmpDir != null) {
            for (String dir : tmpDir) {
                try {
                    PathUtils.rmr(dir);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Fail to delete " + dir);
                }
            }
        }
    }
}
