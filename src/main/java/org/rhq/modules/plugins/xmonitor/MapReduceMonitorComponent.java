package org.rhq.modules.plugins.xmonitor;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.modules.plugins.jbossas7.BaseComponent;
import org.rhq.modules.plugins.jbossas7.StandaloneASComponent;
import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;

public class MapReduceMonitorComponent extends StandaloneASComponent<BaseComponent<?>> implements MeasurementFacet, OperationFacet, Runnable {
	private ResourceContext context;
    private StandaloneASComponent parent;

    
	private Logger log = Logger.getLogger(MapReduceMonitorComponent.class.getName());
	long frequency = 0l;
	private int testTimes = 10;
	private Thread monitor = null;
	int count;
	private boolean running = false;
	private LinkedList<Double> dataStore;
	private double current;
	private AtomicBoolean stopManualInvoked= new AtomicBoolean(false);
	private ObjectName objectMonitored;
	private DecimalFormat nf = new DecimalFormat("#00.00");
	private String mapCode;
	private String reduceCode;
	private GroovyObject mapReducer;
	public static final char EOF = (char) 26;
	//On different classloader ? static reference doesn't work
	private JMXServerFacade jmxF = new JMXServerFacade();

	public MapReduceMonitorComponent() {

	}
	
	public void finalize(){
		log.info("Finalizing the  MapReducer of: " +this.objectMonitored);
	}
	/**
     * Return availability of this resource
     *  @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        // TODO supply real implementation
        return AvailabilityType.UP;
    }
    /**
     * Start the resource connection
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
    	 Configuration conf = context.getPluginConfiguration();
   	 JMXServerFacade.uname = conf.getSimpleValue("username");
     	  JMXServerFacade.pwd = conf.getSimpleValue("password");
     	  JMXServerFacade.host = conf.getSimpleValue("host");
     	  JMXServerFacade.port = conf.getSimpleValue("port");
        this.context = context;
       
        String onameStr = conf.getSimpleValue("objectName");
        int samplingTime = 1;
        try{
        	samplingTime = Integer.parseInt(conf.getSimpleValue("testTimes"));
        }
        catch(NumberFormatException ex){
        	//do nothing samplingTime is yet 1
        }
        
        this.setMapCode(conf.getSimpleValue("mapCode"));
		this.setReduceCode(conf.getSimpleValue("reduceCode"));
		this.setTestTimes(samplingTime);
		this.setObjectMonitored(new ObjectName(onameStr));
		this.init();
        

    }


    /**
     * Gather measurement data
     *  @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport, java.util.Set)
     */
    @Override
    public  void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

         for (MeasurementScheduleRequest req : metrics) {
        	 if(req.getName().equals("xvalue")){
        		 if(stopManualInvoked.get()){
        				return;
        			}
		        	if(!running){
		        		//set the interval and start
		        		this.setFrequency(req.getInterval()/this.testTimes);
		        		this.start();
		        	}
		        	else{
		        		 this.reduce();
		        		 report.addData(new MeasurementDataNumeric(req, this.current));
		        	 }
		        	
        	 } 
        	 
        	 
         }
    }


	public long getFrequency() {
		return frequency;
	}

	public int getTestTimes() {
		return testTimes;
	}

	public void run() {
		log.info("run method of: " + objectMonitored +" invoked");
		while (running) {
			try {
				log.info("running a map phase");
				Thread.currentThread().sleep(frequency);
				// Send a notification
				// Notification msg = new
				// Notification("monitor.IntervalElapsed", this,
				// count, now);
				// super.sendNotification(msg);
				try {
					//this is done by getValues() in RHQ plugin
//					if (count > this.testTimes) {
//						reduce();
//						this.dataStore.clear();
//						count = 0;
//
//					}
					// this.mapper.map(current,
					// this.dataStores,this.objectMonitored);
					map();

				} catch (Exception ex) {
					log.warn("Eccezione:" + ex.getMessage());
					ex.printStackTrace();

				}
				//count++;

			} catch (InterruptedException e) {
				e.printStackTrace();
				running = false;
			}
		}

	}

	public double getCurrent() {
		return this.current;
	}

	public ObjectName getObjectMonitored() {
		return objectMonitored;
	}

	public void setObjectMonitored(ObjectName objectMonitored) {
		this.objectMonitored = objectMonitored;
	}

	private void reduce() {
		Object[] params = { dataStore, this.current };
		this.mapReducer.invokeMethod("reduce", params);
		this.dataStore.clear();

	}

	private void map() {

		Object[] params = { dataStore, this.current };
		log.info(dataStore);
		this.mapReducer.invokeMethod("map", params);
		log.info(dataStore);
		// mapper.map(current, dataStores, objectMonitored);

	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public void setTestTimes(int testTimes) {
		this.testTimes = testTimes;
	}

	public void init() throws Exception {
		this.dataStore = new LinkedList<Double>();
		this.mapReducer = getMapReducer();
	}

	public void start() {
		
		this.running = true;
		// Create a new thread with this monitor
		monitor = new Thread(this, "JBossMonitor");
		// start the thread
		monitor.start();
	}

	public void stop() {
		this.running = false;
		Thread.currentThread().interrupt();
	}

	public String getMapCode() {
		return this.mapCode;
	}

	public void setMapCode(String mapCode) {
		this.mapCode = mapCode.replace(EOF, '\n');

	}

	public String getReduceCode() {
		return this.reduceCode;
	}

	public void setReduceCode(String reduceCode) {
		this.reduceCode = reduceCode.replace(EOF, '\n');

	}

	public GroovyObject getMapReducer() throws IOException,
			ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		

		ClassLoader parent = this.getClass().getClassLoader();
		GroovyClassLoader loader = new GroovyClassLoader(parent);
		String groovyCode = readFileAsString(
				loader.getResourceAsStream("MapReducerTemplate.groovy"))
				.replace("$$mapCode$$", this.mapCode).replace("$$reduceCode$$",
						this.reduceCode);
		Class GroovyClass = loader.parseClass(groovyCode);
		GroovyObject groovyMapper = (GroovyObject) GroovyClass.newInstance();
		groovyMapper.setProperty("objectMonitored", getObjectMonitored());
		groovyMapper.setProperty("ctx", this);
		groovyMapper.setProperty("jmxF", this.jmxF);
		return groovyMapper;

	}

	public static String readFileAsString(InputStream in)
			throws java.io.IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line, results = "";
		while ((line = reader.readLine()) != null) {
			results += line + "\r\n";
		}
		reader.close();
		return results;
	}
	
	public boolean isRunning(){
		return running;
	}
	 public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception{
		 if(name.equals("stop")){
			 this.running = false;
		 
		 OperationResult result = new OperationResult("stop invoked");
		 this.stopManualInvoked.set(true);
		 return result;
		 }
		 if(name.equals("start")){
		 OperationResult result = new OperationResult("start invoked");
		 this.stopManualInvoked.set(false);
		 return result;
		 }
		 return null;
	 }
	
}
