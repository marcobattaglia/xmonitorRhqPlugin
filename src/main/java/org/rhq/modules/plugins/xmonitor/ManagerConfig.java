package org.rhq.modules.plugins.xmonitor;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;


//it's a facade to manage xmonitorManager
public class ManagerConfig {
	private static Logger log = Logger.getLogger(ManagerConfig.class);
	static ConcurrentHashMap<String, ManagerConfig> managers = new ConcurrentHashMap<String, ManagerConfig>();
	
	//method
	public LinkedList<ObjectName> discover() throws Exception{
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
			throw ioex;
		}
		this.monitored = discoveredNow;
		//prepare HashMap 
		for(ObjectName on : discoveredNow){
			if(!this.monitors.containsKey(on)){
				MapReduceMonitorComponent monitor = this.initMapReduceMonitor(on);
				this.monitors.put(on, monitor);
			}
			
		}
		//TODO implementare la rimozione
		
		for(ObjectName on: this.monitors.keySet()){
			if(!discoveredNow.contains(on)){
				MapReduceMonitorComponent monitorToStop = monitors.get(on);
				monitorToStop.stop();
				monitors.remove(on);
			}
		}
		
		return discoveredNow;
		
		
		
	}
	
	//initialize the real monitor 
	//it collects data and expose values for getValues
	public MapReduceMonitorComponent initMapReduceMonitor(ObjectName oname) throws Exception{
		MapReduceMonitorComponent monitor = new MapReduceMonitorComponent();
		monitor.setMapCode(mapCode);
		monitor.setReduceCode(reduceCode);
		monitor.setObjectMonitored(oname);
		
		return monitor;
		
		
	}
	
	public void configMe(Configuration config) throws IOException{
		
		this.discoveryFilter = config.getSimpleValue("discoveryFilter");
		if(!ManagerConfig.managers.contains(this.discoveryFilter)){
			ManagerConfig.managers.put(this.discoveryFilter, this);
		}
		try{
		this.testTimes = Long.parseLong(config.getSimpleValue("testTimes"));
		}
		catch(java.lang.NumberFormatException nfe){
			this.testTimes = 10;
		}
		this.bindingFilter = config.getSimpleValue("bindingFilter");
		this.keyProperty = config.getSimpleValue("keyProperty");
		this.mapCode = config.getSimpleValue("mapCode");
		this.reduceCode = config.getSimpleValue("reduceCode");
		
		
		
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
	public long getTestTimes() {
		return testTimes;
	}
	public void setTestTimes(long testTimes) {
		this.testTimes = testTimes;
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
	private long testTimes;
	private HashMap<ObjectName, MapReduceMonitorComponent> monitors= new HashMap<ObjectName, MapReduceMonitorComponent>();
	private LinkedList<ObjectName> monitored = new LinkedList<ObjectName>();
	private String mapCode;
	private String reduceCode;
	private String keyProperty;
	
}
