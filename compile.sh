mvn -Pdev -DskipTests
cd ./target
mkdir ./temp
cp *.jar ./temp
cd temp
jar -xf groovy-all-2.1.5.jar
jar -xf rhq-plugin-cunstom-xmonitor-4.10.0-SNAPSHOT.jar
cp ../classes/META-INF/*.* ./META-INF/
rm *.jar
jar -cMvf  rhq-plugin-cunstom-xmonitor-4.10.0-SNAPSHOT.jar .
cp ./rhq-plugin-cunstom-xmonitor-4.10.0-SNAPSHOT.jar /home/marco/Prog/RHQ/rootDir/dev-container/rhq-server/modules/org/rhq/server-startup/main/deployments/rhq.ear/rhq-downloads/rhq-plugins/
cp ./rhq-plugin-cunstom-xmonitor-4.10.0-SNAPSHOT.jar ../
cd ../
rm -rf ./temp
cd ../../

