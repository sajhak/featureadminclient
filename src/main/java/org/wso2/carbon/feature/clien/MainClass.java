package org.wso2.carbon.feature.clien;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.feature.mgt.core.operations.OperationFactory;
import org.wso2.carbon.feature.mgt.stub.prov.data.FeatureInfo;
import org.wso2.carbon.feature.mgt.stub.prov.data.ProvisioningActionResultInfo;
import org.wso2.carbon.feature.mgt.stub.prov.data.RepositoryInfo;

public class MainClass {

	private static final Log log = LogFactory.getLog(MainClass.class);

	public static void main(String[] args) throws Exception {

		PropertyConfigurator.configure(MainClass.class.getResource("/log4j.properties"));
		readProperties();
		String path = System.getProperty("keystore.path");
		System.setProperty("javax.net.ssl.trustStore", path);
		System.setProperty("javax.net.ssl.trustStorePassword", System.getProperty("keystore.password"));
		String backendServerURL = System.getProperty("server.url");
		String repoPath = System.getProperty("repo.path");
		String featureListFile = System.getenv("feature.list.file");
		String userName = System.getProperty("user.name");
		String password = System.getProperty("user.password");
		String repoURL = System.getProperty("p2.repo.url");
		String repoName = System.getProperty("p2.repo.name");
		boolean isLocalRepo = Boolean.parseBoolean(System.getProperty("p2.repo.local"));
		//String axis2ClientPath = "axis2_client.xml";
		
		LoginAdminServiceClient login = null;
		boolean repoAlreadyAdded = false;
		
		try {
			
			// Read prop file
				    
		    
			login = new LoginAdminServiceClient(backendServerURL);
			String sessionCookie = login.authenticate(userName, password);
			
			ConfigurationContext ctx = ConfigurationContextFactory.createConfigurationContextFromFileSystem(
			repoPath, null);
			RepositoryAdminServiceClient repoClient = 
					new RepositoryAdminServiceClient(sessionCookie, backendServerURL, ctx);
			
			RepositoryInfo[] repositories = repoClient.getAllRepositories();
			if (repositories != null) {
				for (RepositoryInfo repositoryInfo : repositories) {
					if (repositoryInfo.getNickName().equals(repoName)) {
						repoAlreadyAdded = true;
						log.info("Repository [" + repoName + "] is already added");
					}
				}
			}
			
			// Add repository
			if(!repoAlreadyAdded){
				repoClient.addRepository(repoURL, repoName, isLocalRepo);
				log.info("Repository ["+repoName + "] added successfully");
			}
			
			ProvisioningAdminClient provClient = new ProvisioningAdminClient(
					sessionCookie, backendServerURL, ctx);
			ProvisioningActionResultInfo installActionResult;
			boolean proceedToNextStep;

			installActionResult = provClient.reviewInstallFeaturesAction(readFeaturesFromFile(featureListFile));
			if (installActionResult == null) {
				throw new Exception("Failed to review the installation plan");
			}
			proceedToNextStep = installActionResult.getProceedWithInstallation();
			System.out.println("Install action result: " + proceedToNextStep);
			FeatureInfo[] reviewedFeatures = installActionResult.getReviewedInstallableFeatures();
			if (reviewedFeatures == null || reviewedFeatures.length == 0) {
				log.info("No features to be installed");
			}

			if (proceedToNextStep) {
				log.info("Install feature review successful.");

//				LicenseInfo[] licenseInfo = provClient.getLicensingInformation();
//				for (LicenseInfo licenseInfo2 : licenseInfo) {
//					System.out.println(licenseInfo2.getBody());
//				}

				log.info("Features being installed....");
				provClient.performInstallation(OperationFactory.INSTALL_ACTION);

				log.info("Features installed successfully");

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Login out");
		login.logOut();
		
		System.exit(0);		

	}

	private static void readProperties() throws FileNotFoundException,
			IOException {
		Properties prop = new Properties();
		FileInputStream file; 
		String confPath = "./conf.properties";
		file = new FileInputStream(confPath);
		prop.load(file);
		
		Enumeration<?> e = prop.propertyNames();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			String value = prop.getProperty(key);
			System.setProperty(key, value);
		}
	}

	private static FeatureInfo[] readFeaturesFromFile(String featureListFile) {

		List<FeatureInfo> featureInfiList = new ArrayList<FeatureInfo>();

		try {
			File fXmlFile = new File(featureListFile);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("feature");

			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					FeatureInfo info = new FeatureInfo();
					info.setFeatureID(eElement.getElementsByTagName("id")
							.item(0).getTextContent());
					info.setFeatureVersion(eElement
							.getElementsByTagName("version").item(0)
							.getTextContent());
					featureInfiList.add(info);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return featureInfiList.toArray(new FeatureInfo[featureInfiList.size()]);
	}
}