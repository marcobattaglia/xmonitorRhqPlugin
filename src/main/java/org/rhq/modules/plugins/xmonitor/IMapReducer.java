package org.rhq.modules.plugins.xmonitor;

import java.util.Hashtable;
import java.util.LinkedList;
import javax.management.ObjectName;

public interface IMapReducer {
	public void map(double current, LinkedList<Double> dataStore, ObjectName monitored);
	public void reduce(double current, LinkedList<Double> dataStore, ObjectName monitored);
}
