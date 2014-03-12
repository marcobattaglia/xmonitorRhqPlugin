package org.rhq.modules.plugins.xmonitor;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import jboss.as.xmonitor.compiler.CachedCompiler;
import jboss.as.xmonitor.util.XmonitorCompiler;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;


public class MapReduceMonitor implements
		Runnable {
	private Logger log = Logger.getLogger(MapReduceMonitor.class.getName());
	long frequency = 0l;
	private int testTimes = 10;
	private LinkedList<String> history = new LinkedList<String>();
	private int historyLength = 10;
	private Thread monitor = null;
	int count;
	private String categoryName;
	private boolean running = true;
	private String myName;
	private Hashtable<String, LinkedList<Double>> dataStores;
	private double current;
	private ObjectName objectMonitored;
	private DecimalFormat nf = new DecimalFormat("#00.00");
	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss:SSS");
	private String lastMeasures;// in caso la history length sia a zero;
	private String mapCode;
	private String reduceCode;
	private GroovyObject mapReducer;
//	private IMapper mapper;
	
	private String dsNames;
	private String shortMonitoredName;
	public static final char EOF = (char) 26;

	public String getShortMonitoredName() {
		return shortMonitoredName;
	}

	public void setShortMonitoredName(String shortMonitoredName) {
		this.shortMonitoredName = shortMonitoredName;
	}

	public MapReduceMonitor() {

	}

	public String getName() {
		return this.myName;
	}

	public int getHistoryLength() {
		return historyLength;
	}

	public String getMyName() {
		return myName;
	}

	public void setMyName(String myName) {
		this.myName = myName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public long getFrequency() {
		return frequency;
	}

	public LinkedList<String> getHistory() {
		return history;
	}

	public int getTestTimes() {
		return testTimes;
	}

	public String history() {
		StringBuffer report = new StringBuffer("<h1>Monitor History</h1>\n");
		report.append("<pre><br/>");
		for (int r = 0; r < history.size(); r++)
			report.append(history.get(r));
		report.append("</pre><br/>");
		return report.toString();
	}

	public void run() {

		while (running) {
			try {
				Thread.currentThread().sleep(frequency);
				// Send a notification
				// Notification msg = new
				// Notification("monitor.IntervalElapsed", this,
				// count, now);
				// super.sendNotification(msg);
				try {
					if (count > this.testTimes) {
						reduce();
						dataStoresClear();
						count = 0;

					}
					// this.mapper.map(current,
					// this.dataStores,this.objectMonitored);
					map();

				} catch (Exception ex) {
					log.warn("Eccezione:" + ex.getMessage());
					ex.printStackTrace();

				}
				count++;

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
		Object[] params = { dataStores, this.current };
		this.mapReducer.invokeMethod("reduce", params);

		this.history.add(lastMeasures);

		while (historyLength < history.size())
			history.removeFirst();
	}

	private void map() {

		Object[] params = { dataStores, this.current };
		this.mapReducer.invokeMethod("map", params);
//		mapper.map(current, dataStores, objectMonitored);

	}

	private void dataStoresClear() {
		synchronized (this.dataStores) {
			for (LinkedList<Double> ds : this.dataStores.values()) {
				ds.clear();
			}
		}

	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
		this.log = Logger.getLogger(categoryName);

	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public void setHistory(LinkedList<String> history) {
		this.history = history;
	}

	public void setHistoryLength(int length) {

		this.historyLength = length;
		if (historyLength < 0)
			historyLength = 0;
		while (historyLength < history.size())
			history.removeFirst();
	}

	public void setTestTimes(int testTimes) {
		this.testTimes = testTimes;
	}

	public void start() throws Exception {
		this.dataStores = new Hashtable<String, LinkedList<Double>>();
		// Prepare 3 datastores
		for (String ds : this.dsNames.split(",")) {
			dataStores.put(ds.trim(), new LinkedList<Double>());
		}
//		this.mapper = getJavaMapper();
		this.mapReducer = getMapReducer();
		// Create a new thread with this monitor
		monitor = new Thread(this, "JBossMonitor");
		// Set it as a daemon
		monitor.setDaemon(true);
		// start the thread
		monitor.start();
	}

	public void stop() {
		this.running = false;
		Thread.currentThread().interrupt();
	}

	public void setMonitored(String monitored) {

		try {
			this.objectMonitored = new ObjectName(monitored);
		} catch (MalformedObjectNameException e) {
			log.error("Impossible to define ObjectName of my monitored object");
			e.printStackTrace();
		} catch (NullPointerException e) {
			log.error("Impossible to define ObjectName of my monitored object");
			e.printStackTrace();
		}

	}

	public String getMonitored() {
		return this.objectMonitored.toString();
	}

	public String getMeasures() {

		return this.lastMeasures + "  - " + this.getShortMonitoredName();
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
				loader.getResourceAsStream("MapReducerTemplate.groovy")).replace(
				"$$mapCode$$", this.mapCode).replace("$$reduceCode$$", this.reduceCode);
		Class GroovyClass = loader.parseClass(groovyCode);
		GroovyObject groovyMapper = (GroovyObject) GroovyClass.newInstance();
		groovyMapper.setProperty("objectMonitored", getObjectMonitored());
		groovyMapper.setProperty("ctx", this);
		return groovyMapper;

	}
//	public IMapper getJavaMapper(){
//
//IMapper mapper = null;
//		CachedCompiler cc = new CachedCompiler(null, null);
//		Class aClass = null;
//		try {
//			String javaCode = readFileAsString(XmonitorCompiler.class.getClassLoader()
//					.getResourceAsStream("MapperTemplate"));
//			try {
//				aClass = cc.loadFromJava("Mapper", javaCode);
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			try {
//				mapper = (IMapper)aClass.newInstance();
//			} catch (IllegalAccessException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		return mapper;
//	}
//
//	
	
	public void toDebug(){
		//TRY
				//TRY
				
				ClassLoader cl = this.getClass().getClassLoader().getParent();
				Module addedModule = null;
				try {
					addedModule = ((ModuleClassLoader)this.getClass().getClassLoader()).getModule().getModuleLoader().loadModule(ModuleIdentifier.fromString("org.postgresql:main"));
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (ModuleLoadException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					Class<?> postgresClazz = Class.forName("org.postgresql.Driver", true,addedModule.getClassLoader() );
					Object o = postgresClazz.newInstance();	
					for(Method curr: postgresClazz.getMethods()){
						log.info(curr.getName());
						Class<?>[] params = curr.getParameterTypes();
						for(Class<?> param:params){
							log.info(param.toString());
						}
					}
					Method toInvoke = postgresClazz.getMethod("getLogLevel", new Class<?>[]{});
					Object ex = toInvoke.invoke(o,null);
					System.out.println(ex.toString());
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				
				//TEST!!!
				//TEST!!!
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

	public String getDSNames() {

		return this.dsNames;
	}

	public void setDSNames(String DSnames) {
		this.dsNames = DSnames;

	}
}
