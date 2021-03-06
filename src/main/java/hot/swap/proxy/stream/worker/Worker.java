package hot.swap.proxy.stream.worker;

import hot.swap.proxy.base.SComponent;
import hot.swap.proxy.base.Shutdown;
import hot.swap.proxy.cluster.Cluster;
import hot.swap.proxy.cluster.HuskaZkCluster;
import hot.swap.proxy.message.IConnection;
import hot.swap.proxy.message.LocalConnection;
import hot.swap.proxy.message.MessageCenter;
import hot.swap.proxy.message.QueueManager;
import hot.swap.proxy.smanager.SwapManager;
import hot.swap.proxy.smodule.SwapModule;
import hot.swap.proxy.sproxy.SwapProxy;
import hot.swap.proxy.stream.DynamicTopology;
import hot.swap.proxy.stream.Topology;
import hot.swap.proxy.utils.Utils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by leeshine on 4/11/17.
 */

public class Worker implements Runnable,Shutdown {
    private static Logger LOG = LoggerFactory.getLogger(Worker.class);

    private Map conf;
    private WorkerData workerData;
    private HuskaZkCluster huskaZkCluster;
    private String topologyName;

    private String workerName;
    private Thread thread;

    private PathChildrenCache dynamicCache;
    private static final String DYNAMIC_PATH = Cluster.DYNAMIC_SUBTREE;

    private PathChildrenCache pluginCache;
    private static final String PLUGIN_PATH = Cluster.PLUGINS_SUBTREE;

    private Map<String,SComponent> componentMap;
    private SwapManager swapManager;

    public Worker(WorkerData workerData) throws Exception{
        this.conf = workerData.getConf();
        this.workerData = workerData;
        //this.huskaZkCluster = workerData.getHuskaZkCluster();
        this.componentMap = new HashMap<String, SComponent>();
        init();
    }

    public void init() throws Exception{
        initZK();

        dynamicCache = new PathChildrenCache(huskaZkCluster.getClient(),DYNAMIC_PATH,true);
        dynamicCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        dynamicCache.getListenable().addListener(new DynamicListener());

        pluginCache = new PathChildrenCache(huskaZkCluster.getClient(),PLUGIN_PATH,true);
        pluginCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        pluginCache.getListenable().addListener(new PluginListener());
    }



    public SwapManager getSwapManager(){
        return swapManager;
    }

    public void startNewTopology(Topology topology){
        this.topologyName = topology.getTopologyName();

        List<String> class_names = topology.getComponentNames();
        try {
            for (String class_name : class_names) {
                SComponent component = huskaZkCluster.getSComponentByClassName(class_name);
                componentMap.put(class_name,component);
            }
        }catch (Exception e){
            LOG.error(e.getMessage());
        }

        QueueManager queueManager = new QueueManager();
        MessageCenter messageCenter = new MessageCenter(queueManager);
        swapManager = new SwapManager(messageCenter);

        Map<String,List<String>> inputList = topology.getInputList();

        messageCenter.handleInputList(inputList);

        for(Map.Entry<String,SComponent> entry : componentMap.entrySet()){
            SComponent component = entry.getValue();
            component.init(queueManager,messageCenter);

            //to do order by start thread
            // name stategy
            if(component.checkSwapable()){//swapable
                String proxyName = component.getId(); //proxyname = modulename ??
                IConnection connection = new LocalConnection(proxyName,queueManager,messageCenter);
                SwapProxy swapProxy = new SwapProxy(proxyName,(SwapModule)component,connection);
                swapProxy.startRun();
                swapManager.addProxy(component.getId(),swapProxy);
            }else{
                component.init(queueManager,messageCenter);
                component.startRun();
            }
        }

        ///???
        /*thread = new Thread(this);
        workerName = "worker_"+RandomUtil.RandomString(3);
        thread.setName(workerName);
        thread.start();*/
    }

    class DynamicListener implements PathChildrenCacheListener{
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            PathChildrenCacheEvent.Type eventType = pathChildrenCacheEvent.getType();
            ChildData childData = pathChildrenCacheEvent.getData();
            byte[] data = childData.getData();

            if(eventType.equals(PathChildrenCacheEvent.Type.CHILD_ADDED)){
                updateCurrentTopology(data);
            }
        }
    }

    class PluginListener implements  PathChildrenCacheListener{
        public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
            PathChildrenCacheEvent.Type eventType = pathChildrenCacheEvent.getType();
            ChildData childData = pathChildrenCacheEvent.getData();
            byte[] data = childData.getData();

            if(eventType.equals(PathChildrenCacheEvent.Type.CHILD_ADDED)){
                swapComponent(data);
            }
        }
    }

    public void updateCurrentTopology(byte[] data){
        DynamicTopology dynamicTopology = (DynamicTopology)Utils.deserialize(data);
    }

    public void swapComponent(byte[] data){
    }

    public void stopCurrentTopology(){

    }

    private void initZK() throws Exception{
        try {
            huskaZkCluster = new HuskaZkCluster(conf);
        }catch (Exception e){
            LOG.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        workerData.setHuskaZkCluster(huskaZkCluster);
    }

    public String getWorkerName(){
        return workerName;
    }

    private Topology getTopology(){
        Topology topology = null;
        try{
            topology = huskaZkCluster.getTopology(topologyName);
        }catch (Exception e){
            LOG.error(e.getMessage());
        }
        return topology;
    }

    public void run() {
    }

    public void shutdown() {
        huskaZkCluster.close();
    }
}
