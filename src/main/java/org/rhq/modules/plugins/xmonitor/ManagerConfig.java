package org.rhq.modules.plugins.xmonitor;
import java.io.IOException;
import java.util.LinkedList;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;


//it's a facade to manage xmonitorManager
public class ManagerConfig {
	private static Logger log = Logger.getLogger(ManagerConfig.class);
	
	//method
	public LinkedList<ObjectName> discover(){
		log.info("I'm discovering with this filter " + this.discoveryFilter);
		LinkedList<ObjectName> discoveredNow = new LinkedList<ObjectName>();
		try{
		if (this.bindingFilter != null) {
			// Datasources are discovered with a JMXQuery but are bounded
			// with a different JMXQuery --> bindingFilter manage this
			// exception
			discoveredNow = JMXServerFacade.discover(this.discoveryFilter,
					this.bindingFilter);

		} else {
			discoveredNow = JMXServerFacade.discover(this.discoveryFilter);
		}
		}
		catch(IOException ioex){
			log.error("No connection to JMXServer");
			
		}
		return discoveredNow;
		
		
		
	}
	
	public void configMe(Configuration config) throws IOException{
		this.discoveryFilter = config.getSimpleValue("discoveryFilter");
		this.frequency = Long.parseLong(config.getSimpleValue("frequency"));
		this.bindingFilter = config.getSimpleValue("bindingFilter");
		this.keyProperty = config.getSimpleValue("keyProperty");
		this.mapCode = config.getSimpleValue("mapCode");
		this.reduceCode = config.getSimpleValue("reduceCode");
		JMXServerFacade.prepareConnection(config);
		
		
	}
	
	
	
	//field, getter and setter
	private String discoveryFilter;
	public String getDiscoveryFilter() {
		return discoveryFilter;
	}
	public void setDiscoveryFilter(String discoveryFilter) {
		this.discoveryFilter = discoveryFilter;
	}
	public String getBindingFilter() {
		return bindingFilter;
	}
	public void setBindingFilter(String bindingFilter) {
		this.bindingFilter = bindingFilter;
	}
	public long getFrequency() {
		return frequency;
	}
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}
	public LinkedList<ObjectName> getMonitored() {
		return monitored;
	}
	public void setMonitored(LinkedList<ObjectName> monitored) {
		this.monitored = monitored;
	}
	public String getMapCode() {
		return mapCode;
	}
	public void setMapCode(String mapCode) {
		this.mapCode = mapCode;
	}
	public String getReduceCode() {
		return reduceCode;
	}
	public void setReduceCode(String reduceCode) {
		this.reduceCode = reduceCode;
	}
	public String getKeyProperty() {
		return keyProperty;
	}
	public void setKeyProperty(String keyProperty) {
		this.keyProperty = keyProperty;
	}
	private String bindingFilter;
	private long frequency;
	private LinkedList<ObjectName> monitored = new LinkedList<ObjectName>();
	private String mapCode;
	private String reduceCode;
	private String keyProperty;
	
}
