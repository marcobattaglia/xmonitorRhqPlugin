package org.rhq.modules.plugins.xmonitor;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;





import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;

import javax.management.ObjectName;

/**
 * Discovery class
 */
@SuppressWarnings("unused")
public class XMonitorDiscovery implements ResourceDiscoveryComponent<StandaloneASComponent<?>>

{

    private final Log log = LogFactory.getLog(this.getClass());
    private final ManagerConfig mc = new ManagerConfig();
    public static Configuration pluginConf;
    public static ResourceDiscoveryContext staticContext;
    /**
     * Run the auto-discovery
     */
    public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext discoveryContext) throws Exception {
       log.info("discoverResources is called");
       
       Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
       Collection<String> parentProperties = discoveryContext.getParentResourceContext().getPluginConfiguration().getNames();
       for(String prop:parentProperties){
    	   try{
    	   log.info("Parent property name: " + prop+"="+discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue(prop));
    	   }
    	   catch(Exception ex){}
       }
      Configuration conf = discoveryContext.getDefaultPluginConfiguration();
      mc.configMe(conf);
      staticContext = discoveryContext;
      
      if(JMXServerFacade.port == null){
    	  JMXServerFacade.uname = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("user");    	  
    	  JMXServerFacade.pwd = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("password");
    	  JMXServerFacade.host = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("hostname");
    	  JMXServerFacade.port = conf.getSimpleValue("port");
    	  
    	  conf.setSimpleValue("host", JMXServerFacade.host);
    	  conf.setSimpleValue("password", JMXServerFacade.pwd);
    	  conf.setSimpleValue("username", JMXServerFacade.uname);
    	  JMXServerFacade.getConn();
      }
       LinkedList<ObjectName> onResources =   mc.discover();
       
       for(ObjectName on : onResources){
    	   DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                   discoveryContext.getResourceType(), // ResourceType
                   on.getCanonicalName(),
                   on.getCanonicalName(),
                   "",
                   "XMonitor on " + on.getCanonicalName(),
                   discoveryContext.getDefaultPluginConfiguration(),
                   null
               );
    	   discoveredResources.add(detail);
    	   detail.getPluginConfiguration().setSimpleValue("objectName", on.getCanonicalName());
//    	   Configuration resConf = detail.getPluginConfiguration();
//    	   resConf.setSimpleValue("discoveryFilter", mc.getDiscoveryFilter());
//    	   resConf.setSimpleValue("bindingFilter", mc.getBindingFilter());
//    	   resConf.setSimpleValue("keyProperty", mc.getKeyProperty());
//    	   resConf.setSimpleValue("mapCode", mc.getMapCode());
//    	   resConf.setSimpleValue("reduceCode", mc.getReduceCode());
//    	   resConf.setSimpleValue("frequency", ""+mc.getFrequency());
       }
        //  only discover if the home path contains /var/lib/openshift
         return discoveredResources;

        }

}