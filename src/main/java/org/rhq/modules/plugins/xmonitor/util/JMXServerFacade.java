package org.rhq.modules.plugins.xmonitor.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.modules.plugins.xmonitor.XMonitorDiscovery;


public class JMXServerFacade {
     static Log log = LogFactory.getLog(JMXServerFacade.class);
     static public String host="127.0.0.1";
//     static public String port="9999";
     static public String port;
     static public String uname="admin";
     static public String pwd="pippo";
     static MBeanServerConnection  server;
     static AtomicBoolean initialized = new AtomicBoolean(false);
     static AtomicBoolean connYetResetted= new AtomicBoolean(false);
     static public Configuration pluginConfiguration;
     static{
    	 log.info("Mi sto inizializzando");
//    	 ResourceDiscoveryContext discoveryContext = XMonitorDiscovery.staticContext;
//    	 JMXServerFacade.uname = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("user");
//      	  JMXServerFacade.pwd = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("password");
//      	  JMXServerFacade.host = discoveryContext.getParentResourceContext().getPluginConfiguration().getSimpleValue("hostname");
//      	  JMXServerFacade.port = discoveryContext.getDefaultPluginConfiguration().getSimpleValue("port");
    	 
    	 try{
    		 throw new Exception("Stack Trace");
    	 }
    	 catch(Exception ex){
    		 ex.printStackTrace();
    	 }
     }
     
     
     
     
     public static MBeanServerConnection getConn() throws Exception{
    	 MBeanServerConnection connection= null;
    	if(server==null){
	    	String urlString = "service:jmx:remoting-jmx://" + host + ":" + port;
	     	JMXServiceURL serviceURL = new JMXServiceURL(urlString);
	  		Hashtable h = new Hashtable();	
	  		String[] credentials = new String[] { uname.trim(), pwd.trim() };
	  		h.put("jmx.remote.credentials", credentials);
	  		JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, h);
	  		connection = jmxConnector
	  				.getMBeanServerConnection();
	  		server = connection;
    	}
    	
    	
  		return server;
     }
     
     
     public static void prepareConnection(Configuration...config) throws IOException{
    	if (server!= null){
     		return;
     	}
    	if(config!=null && config.length!=0 && config[0]!=null){
    		JMXServerFacade.pluginConfiguration = config[0];
    	}
    	if(JMXServerFacade.pluginConfiguration==null){
    		return;
    	}
    	
    	String userName = JMXServerFacade.pluginConfiguration.getSimpleValue("username");
  		String password = JMXServerFacade.pluginConfiguration.getSimpleValue("password");
  		String host = JMXServerFacade.pluginConfiguration.getSimpleValue("host");
  		String port = JMXServerFacade.pluginConfiguration.getSimpleValue("port");
    	String urlString = "service:jmx:remoting-jmx://" + host + ":" + port;
    	
    	JMXServiceURL serviceURL = new JMXServiceURL(urlString);
 		Hashtable h = new Hashtable();	
 		String[] credentials = new String[] { userName.trim(), password.trim() };
 		h.put("jmx.remote.credentials", credentials);
 		JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceURL, h);
 		MBeanServerConnection connection = jmxConnector
 				.getMBeanServerConnection();
 	    JMXServerFacade.server = connection;
     }
    
       
     static public double executeSample(String objectName, String measure, String...path)throws Exception{
        return executeSample(new ObjectName(objectName), measure, path);    	 
     }
     
     
     static synchronized void resetConnection() throws IOException{
    	 if(connYetResetted.get()){
    		 throw new IOException("I cannot reset connection to JMX Server");
    	 }
    	 prepareConnection();
    	 Executors.newSingleThreadScheduledExecutor().schedule(new Runnable(){
    		 public void run(){
    			 JMXServerFacade.connYetResetted.set(false);
    		 }
    	 },3, TimeUnit.SECONDS);
    	 
     }
     
     
     
     static public double executeSample(ObjectName objectName, String measure, String...path)throws Exception{
    	 Date startTime = new Date();    
    	 Object value = null;
    	 server = JMXServerFacade.getConn();
    	 try{
    		 value = server.getAttribute(objectName, measure.trim());
    	 }
    	 catch(NullPointerException NPE){
    		  log.error("Impossible retrieving data from jboss");
    	 }
    	 //log.warn("2) querying ObjectName:" +objectName+ " attribute:" + measure + " path: "+path[0]);
    	 double toReturn = 0d;
    	 try{
             toReturn = Double.parseDouble(dump(value, null,path));
         }
         catch(SpeedException spe){
             log.trace(" Sampling duration: " + (new Date().getTime() - startTime.getTime()));
             log.trace("data: " + spe.getMessage());
             return Double.parseDouble(spe.getMessage());
         }
    	 log.trace(" Sampling duration: " + (new Date().getTime() - startTime.getTime()));
    	 log.trace("data: " + toReturn);
    	 return toReturn;
    	 
     }
      
     static public String getMeasures(ObjectName objectName)throws Exception{
    	 Date startTime = new Date();  
    	 server = JMXServerFacade.getConn();
    	 Object r = server.getAttribute(objectName, "Measures");
    	 log.info(" Object: " + objectName +" value: " + r.toString()+ " Sampling duration: " + (new Date().getTime() - startTime.getTime()));
    	 return r.toString();
    	 
     }
     
     
	 
	 public static LinkedList<ObjectName> discover(String filter,  String...criteria) throws Exception{
		    log.info("I'm discovering with this filter: " + filter);
		    
		    //added a double container to manage multiple filter separated by ;	    
		    //LinkedList<LinkedList<ObjectName>> toReturn_outer = new LinkedList<LinkedList<ObjectName>>();
		    LinkedList<ObjectName> toReturn_inner = new LinkedList<ObjectName>();
	    	ObjectName objectFilter = null;
	    	server = JMXServerFacade.getConn();
	    	//you can have multiple filters separated by ;
	    	for(String currentFilter : filter.split(";")){
	    	    //LinkedList<ObjectName> toReturn_inner = new LinkedList<ObjectName>();
				try {
					objectFilter = new ObjectName(currentFilter);
					Set<ObjectName> mbeans = server.queryNames(objectFilter, null);
					log.info("Number of object discovered to monitor is: " + mbeans.size());
		        
					//cycle for each single filter
    		        for (ObjectName on : mbeans) {
    		        	if(criteria.length>0 ){
    		        		String property = criteria[0].split("\\{\\$")[1].split("\\}")[0];;
    		        		String objectName = criteria[0].replaceAll("\\{\\$[A-z]*\\}", on.getKeyProperty(property));
    			        	on = new ObjectName(objectName);
    		        	}
    		        	 
    		            toReturn_inner.add(on);
    		        }
		    	} catch (MalformedObjectNameException e) {
					log.error("Impossible to discover ");
					e.printStackTrace();
				} catch (NullPointerException e) {
					log.error("Impossible to discover");
					e.printStackTrace();
				} 
				//toReturn_outer.add(toReturn_inner);
				catch (IOException e) {
						resetConnection();
				}
	    	}
	    	
	 
	    	
	    	
	    	return toReturn_inner;
	    }
	 

    @SuppressWarnings("unchecked")
	public static <T> LinkedList<T> discover(String filter, Class<T> c, String...criteria) throws Exception{
    	LinkedList<T> toReturn = new LinkedList<T>();
    	server = JMXServerFacade.getConn();
   	 
    	ObjectName objectFilter = null;
			try {
				objectFilter = new ObjectName(filter);
				Set<ObjectName> mbeans = server.queryNames(objectFilter, null);
	        
	        for (ObjectName on : mbeans) {
	        	
	            T mcp = (T)MBeanServerInvocationHandler.newProxyInstance(server, on, c, false);
	            
	            toReturn.add(mcp);
	        }
	    	} catch (MalformedObjectNameException e) {
				log.error("Impossible to discover classes like: " + c.getClass().getName());
				e.printStackTrace();
			} catch (NullPointerException e) {
				log.error("Impossible to discover classes like: " + c.getClass().getName());
				e.printStackTrace();
			} catch (IOException e) {
				resetConnection();
			} 
    	
    	
    	return toReturn;
    }
	
    public static String dump(Object obj, String initialPath, String...requestedPath) throws SpeedException {
        StringBuffer strBuf = new StringBuffer("");
        String path = null;
        
            
        if (obj instanceof CompositeDataSupport) {
            
            CompositeDataSupport data = (CompositeDataSupport) obj;
            CompositeType compositeType = data.getCompositeType();
            Set keys = compositeType.keySet();
            for (Iterator iter = keys.iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                if(initialPath != null ){
                    path = initialPath + ((key.length()!=0)? key + "/" : "");
                }
                else{
                    path = (key.length()!=0)? key + "/" : "";
                }
                //strBuf.append("\n" + path );
                Object value = data.get(key);
                OpenType type = compositeType.getType(key);
                if (type instanceof SimpleType) {
                    if(requestedPath!=null && requestedPath[0]!=null && requestedPath.length>0 && requestedPath[0].equals(path)){
                        throw new SpeedException(value.toString());
                    }
                    strBuf.append("\n" +key + ":" + value );
                    
                } else {
                    if (value instanceof CompositeData
                            || value instanceof TabularData) {
                        //strBuf.append();
                        if(requestedPath!=null && requestedPath[0]!=null && requestedPath.length>0 && requestedPath[0].equals(path)){
                            throw new SpeedException("\n"  + key + ":" + dump(value,path,requestedPath).replaceAll("\\n","\\\n\\\t"));
                        }
                        strBuf.append("\n"  + key + ":" + dump(value,path,requestedPath).replaceAll("\\n","\\\n\\\t"));
                    }

                }
                
                
            }
            return strBuf.toString();

        } else if (obj instanceof TabularData) {
            if(initialPath != null){
                if(!initialPath.endsWith("/")){
                    path = initialPath+ "/";
                }
                else{
                    path = initialPath;
                }
            }
            
            TabularData mapToDump = (TabularData) obj;
            Collection<?> values = mapToDump.values();
            for (Object value : values) {
                if (value instanceof CompositeDataSupport) {
                    CompositeDataSupport compData = (CompositeDataSupport) value;
                    if(requestedPath!=null && requestedPath[0]!=null && requestedPath.length>0 && requestedPath[0].equals(path)){
                        throw new SpeedException(dump(compData,path,requestedPath));
                    }
                    strBuf.append(dump(compData,path,requestedPath));
                    
                    return strBuf.toString();
                }
            }

        } 

        return  obj.toString();

    }
class ScheduledResetter implements Runnable{
		

		@Override
		public void run() {
			JMXServerFacade.connYetResetted.set(false);
			
		}
		
	}
}
