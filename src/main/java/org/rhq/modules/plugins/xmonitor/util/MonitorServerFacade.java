package org.rhq.modules.plugins.xmonitor.util;

import javax.management.ObjectName;

public class MonitorServerFacade extends JMXServerFacade{
	public MonitorServerFacade(ObjectName monitored){
		super();
		this.monitored = monitored;
		
	}
	
	private double executeSample(String property){
		try {
			return JMXServerFacade.executeSample(this.monitored,property);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0.0d;
	}
	private ObjectName monitored;

}
