import org.rhq.modules.plugins.xmonitor.util.JMXServerFacade;
import java.lang.Double;
class Map{

def objectMonitored;
def ctx;//you can use it to pass "this" as context, but be care to security issue.
JMXServerFacade jmxF; 

void map(ds, current){				
		$$mapCode$$}
		
void reduce(ds, current){
				
		$$reduceCode$$
}		


Double executeSample(metricName){
return jmxF.executeSample(objectMonitored, metricName);
}

Double executeSample(attribute, metricName){
return jmxF.executeSample(objectMonitored, attribute, metricName);
}

Double executeFullSample(objectMonitored, attribute){
return jmxF.executeSample(objectMonitored, attribute);
}

Double executeFullSample(objectMonitored, attribute, metricName){
return jmxF.executeSample(objectMonitored, attribute, metricName);
}



}