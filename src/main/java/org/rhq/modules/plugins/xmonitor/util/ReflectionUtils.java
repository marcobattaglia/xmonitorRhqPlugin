package org.rhq.modules.plugins.xmonitor.util;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.logging.Logger;

public class ReflectionUtils {
	private static Logger log = Logger.getLogger(ReflectionUtils.class);
	
	  public static void setProperty(String name, Object target, Object value) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException 
	     {
	         Method method = null;
	          for(Method curr: target.getClass().getMethods()){
	        		 if(curr.getName().equals("set"+name)){
	        			 
	        			 Class<?>[] params = curr.getParameterTypes();

	        			 if(params[0].isPrimitive()){
	        				 if(params[0].getName().equals("int")){
	        					 int val = Integer.parseInt(((String)value));
	        					 curr.invoke(target, new Object[]{val});
	        				 }
	        				 if(params[0].getName().equals("long")){
	        					 long val = Long.parseLong(((String)value));
	        					 curr.invoke(target, new Object[]{val});
	        				 }
	        			 }
	        			 else{
	        				 //string
	        				 curr.invoke(target, new Object[] {value});
	        			 }
	        		 }
	        	 }
	        	 
	        
	        
	     }
	  public static Object getProperty(String name, Object target) 
	     {
	         Method method = null;
	         Object result = null;
	          
	         try {
	        	 method = target.getClass().getMethod("get" + name, null);
	         }
	         catch (NoSuchMethodException e) { }
	  
	         if (method != null)
	             try {
	            	result = method.invoke(target, null);
	             }
	             catch (Exception ecc) { }
	         return result;
	     }
	     
	     
	    
	 } 

