package com.test.feature.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.feature.mgt.stub.ProvisioningAdminServiceCallbackHandler;
import org.wso2.carbon.feature.mgt.stub.RepositoryAdminServiceCallbackHandler;
import org.wso2.carbon.feature.mgt.stub.RepositoryAdminServiceStub;
import org.wso2.carbon.feature.mgt.stub.prov.data.ProfileHistory;
import org.wso2.carbon.feature.mgt.stub.prov.data.RepositoryInfo;
import org.wso2.carbon.utils.CarbonUtils;

public class RepositoryAdminServiceClient {

    private static final Log log = LogFactory.getLog(RepositoryAdminServiceClient.class);

    private boolean isComplete = false;
    private boolean isError = false;
    private Exception exception;
    private String defaultRepositoryURL;
    public RepositoryAdminServiceStub repositoryAdminServiceStub;
    
	public RepositoryAdminServiceClient(String cookie, String backendServerURL, ConfigurationContext ctx) throws Exception {
		try {
			String serviceURL = backendServerURL + "/services/" +  "RepositoryAdminService";
			repositoryAdminServiceStub = new RepositoryAdminServiceStub(ctx, serviceURL);
			ServiceClient client = repositoryAdminServiceStub
					._getServiceClient();
			Options option = client.getOptions();
			option.setManageSession(true);
			option.setProperty(
					org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
					cookie);
		} catch (Exception e) {
			handleException(e.getMessage(), e);
		}
	}
	
	public RepositoryInfo[] getAllRepositories() throws Exception {
		return repositoryAdminServiceStub.getAllRepositories();		
	}
	
	private void handleException(String msg, Exception e) throws Exception {
		log.error(msg, e);
		throw new Exception(msg, e);
	}
	
	public void addRepository(String repoURL, String nickName, boolean localRepo) throws Exception {
        //validating inputs
        if (nickName == null || nickName.length() == 0) {


            throw new Exception("Repo name is missing");
        }

        if (repoURL == null || repoURL.length() == 0) {
            throw new Exception("Repo location is missing");
        } else {
        	repoURL = repoURL.trim();
        }

        URI uri = null;
        if (localRepo) {
            //Removing all whitespaces
            repoURL = repoURL.replaceAll("\\b\\s+\\b", "%20");

            //Replacing all "\" with "/"
            repoURL = repoURL.replace('\\', '/');

            if (!repoURL.startsWith("file:") && repoURL.startsWith("/")) {
                repoURL = "file://" + repoURL;
            } else if (!repoURL.startsWith("file:")) {
                repoURL = "file:///" + repoURL;
            }
        } else {
            try {
                uri = new URI(repoURL);
                String scheme = uri.getScheme();
                if (!scheme.equals("http") && !scheme.equals("https") && !scheme.equals("file")) {
                    throw new Exception(MessageFormat.format("Invalid URL protocol", scheme));
                }
            } catch (URISyntaxException e) {
                throw new Exception(MessageFormat.format("Invalid repo location", ""));
            }
        }

        try {
               // add repository as synchronous call      	
         	   repositoryAdminServiceStub.addRepository(repoURL, nickName);	
        	
//        		ServiceClient client = repositoryAdminServiceStub._getServiceClient();
//                client.engageModule("addressing"); // IMPORTANT
//                Options options = client.getOptions();
//                options.setUseSeparateListener(true);
//                options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
//                repositoryAdminServiceStub.startaddRepository(repoURL, nickName, callback);
//                handleCallback();
        	
        } catch (Exception e) {
                handleException(e.getMessage(),e);
        }
    }
	
	private void handleCallback() throws Exception {
        int i = 0;
        while (!isComplete && !isError) {
            Thread.sleep(500);
            i++;
            if (i > 120 * 2400) {
                throw new Exception("Response not received within 4 hours");
            }
        }

        if (isError) {
            isError = false;
            throw exception;
        } else {
            isComplete = false;
        }
    }

    RepositoryAdminServiceCallbackHandler callback = new RepositoryAdminServiceCallbackHandler() {
        @Override
        public void receiveResultaddRepository(boolean result) {
            isComplete = true;
        }

        @Override
        public void receiveErroraddRepository(Exception e) {
            isError = true;
            exception = e;            
        }
        
        @Override
        public void receiveResultaddDefaultRepository(String result) {
        	defaultRepositoryURL = result;
        	isComplete = true;
        }
        
        @Override
        public void receiveErroraddDefaultRepository(Exception e) {
        	isError = true;
        	exception = e;
        }
    };
}
