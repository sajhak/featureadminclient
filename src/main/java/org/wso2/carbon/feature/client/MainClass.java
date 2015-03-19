package org.wso2.carbon.feature.client;

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
import org.apache.log4j.Logger;
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

	final static Logger log = Logger.getLogger(MainClass.class);

	public static void main(String[] args) throws Exception {
		
		
		PropertyConfigurator.configure(MainClass.class.getResource("/log4j.properties"));
		// Validate args
		boolean isArgsValid = false;
		boolean isFeatureInstallAction = false;
		boolean isFeatureUninstallAction = false;

		if (args != null && args.length == 1) {
			for (String arg : args) {
				if (arg.equals("i")) {
					log.info(" Feature installation is selected ");
					isArgsValid = true;
					isFeatureInstallAction=true;
				} else if (arg.equals("u")) {
					log.info(" Feature un-installation is selected ");
					isArgsValid = true;
					isFeatureUninstallAction=true;
				}
			}
		}
		
		if(!isArgsValid) {
			log.error(" Wrong arguments provided. Either provide './bin i' to install features"
					+ " or './bin u' to uninstall features ");
			return;
		}
		
		
		
		
		readProperties();
		String path = System.getProperty("keystore.path");
		System.setProperty("javax.net.ssl.trustStore", path);
		System.setProperty("javax.net.ssl.trustStorePassword", System.getProperty("keystore.password"));
		String backendServerURL = System.getProperty("server.url");
		String repoPath = System.getProperty("repo.path");
		String installFeatureListFile = System.getProperty("install.feature.list.file");
		String uninstallFeatureListFile = System.getProperty("uninstall.feature.list.file");
		String userName = System.getProperty("user.name");
		String password = System.getProperty("user.password");
		String repoURL = System.getProperty("p2.repo.url");
		String repoName = System.getProperty("p2.repo.name");
		boolean isLocalRepo = Boolean.parseBoolean(System.getProperty("p2.repo.local"));
		//String axis2ClientPath = "axis2_client.xml";
				
		LoginAdminServiceClient login = null;
		
		
		try {
			
			login = new LoginAdminServiceClient(backendServerURL);
			String sessionCookie = login.authenticate(userName, password);			
			
			
			ConfigurationContext ctx = ConfigurationContextFactory.
					createConfigurationContextFromFileSystem(repoPath, null);			
			
			if(isFeatureInstallAction) {
				addRepository(repoURL, repoName, isLocalRepo, sessionCookie,backendServerURL, ctx);			
				installFeatures(backendServerURL, installFeatureListFile, sessionCookie, ctx);
			} else if(isFeatureUninstallAction) {
				uninstallFeatures(backendServerURL, uninstallFeatureListFile, sessionCookie, ctx);
			}
			
			log.info("Login out");
			login.logOut();			
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw e;
		} finally {
			System.exit(0);
		}
		
	}

	private static void uninstallFeatures(String backendServerURL, String featureListFile, String sessionCookie,
			ConfigurationContext ctx) throws Exception {

		ProvisioningAdminClient provClient = new ProvisioningAdminClient(
				sessionCookie, backendServerURL, ctx);
		ProvisioningActionResultInfo uninstallActionResult;
		boolean proceedToNextStep;

		uninstallActionResult = provClient
				.reviewUninstallFeaturesAction(readFeaturesFromFile(featureListFile));
		if (uninstallActionResult == null) {
			throw new Exception("Failed to review the Uninstallation plan");
		}

		proceedToNextStep = uninstallActionResult.getProceedWithInstallation();

		FeatureInfo[] reviewedFeatures = uninstallActionResult
				.getReviewedUninstallableFeatures();
		if (reviewedFeatures == null || reviewedFeatures.length == 0) {
			log.info("No features to be uninstalled");
		}
		
		if (proceedToNextStep) {
			log.info("Uninstall feature review successful");
			log.info("Features being uninstalled....");
			provClient.performInstallation(OperationFactory.UNINSTALL_ACTION);
			log.info("Features uninstalled successfully");

		}

	}

	private static void installFeatures(String backendServerURL,
			String featureListFile, String sessionCookie,
			ConfigurationContext ctx) throws Exception {
		ProvisioningAdminClient provClient = new ProvisioningAdminClient(
				sessionCookie, backendServerURL, ctx);
		ProvisioningActionResultInfo installActionResult;
		boolean proceedToNextStep;

		installActionResult = provClient.reviewInstallFeaturesAction(readFeaturesFromFile(featureListFile));
		if (installActionResult == null) {
			throw new Exception("Failed to review the installation plan");
		}
		proceedToNextStep = installActionResult.getProceedWithInstallation();
		log.info("Install action result: " + proceedToNextStep);
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
	}

	private static void addRepository(String repoURL, String repoName,
			boolean isLocalRepo, String sessionCookie, String backendServerURL,
			ConfigurationContext ctx) throws Exception {
		
		boolean repoAlreadyAdded = false;
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
			if(log.isDebugEnabled()) {
			log.debug("Adding key "+ key +" and value "+ value);
			}
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