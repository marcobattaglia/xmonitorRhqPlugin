package org.rhq.modules.plugins.xmonitor;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;

public class ServerDiscovery  implements ResourceDiscoveryComponent<StandaloneASComponent<?>> {

	@Override
	public Set<DiscoveredResourceDetails> discoverResources(
			ResourceDiscoveryContext<StandaloneASComponent<?>> context)
			throws InvalidPluginConfigurationException, Exception {
		 Set<DiscoveredResourceDetails> discoveredResources = new HashSet<DiscoveredResourceDetails>();
		 DiscoveredResourceDetails detail = new DiscoveredResourceDetails(
                 context.getResourceType(), // ResourceType
                 "XMonitor",
                 "XMonitor",
                 "",
                 "XMonitor",
                 context.getDefaultPluginConfiguration(),
                 null
             );
  	   discoveredResources.add(detail);
  	   return discoveredResources;
	}

}
