# PaxToolsAgent

Java servlet to convert  SBGN-ML files into BioPAX Level 3 format. The tool depends on Paxtools (https://biopax.github.io/Paxtools/) libraries . 


## Installation

Install Maven and Apache Tomcat 8 first.  Make sure the port 8080 is open for Tomcat. 

- https://www.digitalocean.com/community/tutorials/how-to-install-apache-tomcat-8-on-ubuntu-16-04
- To install Maven: http://www.vogella.com/tutorials/ApacheMaven/article.html


```
git clone https://github.com/fdurupinar/PaxToolsAgent.git
cd PaxToolsAgent
mvn clean install

```


## Deploying to Tomcat 8 Manually
Ubuntu
```
mkdir /opt/tomcat/paxtools
cd <Path to PaxToolsAgent>/PaxToolsAgent/out/artifacts/PCArtifact_war_exploded/WEB-INF
cp -R . /opt/tomcat/webapps/paxtools/WEB-INF <Path to PaxToolsAgent>/PaxToolsAgent/out/artifacts/PCArtifact_war_exploded/WEB-INF
```

** In WEB-INF there is a classes directory with necessary classes and libraries and a web.xml file which includes the name of the server “PaxtoolsServlet".
** You may need to put slf4j.jar additionally into the out/artifacts/PCArtifact_war_exploded/web-inf/lib folder. 


## Running the Servlet
The servlet runs at: localhost/paxtools/PaxtoolsServlet. You can POST http requests to "http://localhost/paxtools/PaxtoolsServlet".

The form parameters for the POST request are: {reqType:"biopax", content:<BioPax model>}. The servlet will then return the SBGN model.
 
 ## Using the Converter
 
 You can use the converter separately if you don't need to provide this as a service. E.g. run the following piece of code to convert the sample SBGN file under 
 src/main/resources:
 
 ```
  File fIn = new File("src/main/resources/testFile.xml");
  InputStream in = new FileInputStream(fIn);
  OutputStream out = new ByteArrayOutputStream();

  try {
      SBGNPDToL3Converter conv = new SBGNPDToL3Converter(); //get a new converter
      conv.writeL3(in, out);
  }
  catch (JAXBException e) {
      e.printStackTrace();
  }

  resultStr = out.toString();
  
  ```
  
  You can see the output of texFile.xml in  testOut.owl. 

