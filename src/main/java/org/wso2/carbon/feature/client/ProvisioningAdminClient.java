/**
 * 
 */
package org.wso2.carbon.feature.client;

import java.util.Arrays;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.log4j.Logger;
import org.wso2.carbon.feature.mgt.core.operations.OperationFactory;
import org.wso2.carbon.feature.mgt.stub.ProvisioningAdminServiceCallbackHandler;
import org.wso2.carbon.feature.mgt.stub.ProvisioningAdminServiceStub;
import org.wso2.carbon.feature.mgt.stub.prov.data.FeatureInfo;
import org.wso2.carbon.feature.mgt.stub.prov.data.LicenseInfo;
import org.wso2.carbon.feature.mgt.stub.prov.data.ProfileHistory;
import org.wso2.carbon.feature.mgt.stub.prov.data.ProvisioningActionInfo;
import org.wso2.carbon.feature.mgt.stub.prov.data.ProvisioningActionResultInfo;
import org.wso2.carbon.utils.CarbonUtils;

public class ProvisioningAdminClient {

	public ProvisioningAdminServiceStub provAdminStub;
	private static final Logger log = Logger.getLogger(ProvisioningAdminClient.class);
	private boolean isComplete = false;
	private boolean isError = false;
	private Exception exception;
	private ProfileHistory[] profileHistories = new ProfileHistory[]{};

	public ProvisioningAdminClient(String sessionCookie, String backendServerURL, ConfigurationContext ctx)
			throws Exception {

		try {
			String serviceURL = backendServerURL + "/services/" +  "ProvisioningAdminService";			
			provAdminStub = new ProvisioningAdminServiceStub(ctx, serviceURL);
			ServiceClient client = provAdminStub._getServiceClient();
			Options option = client.getOptions();
			option.setManageSession(true);
			option.setProperty(
					org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING,
					sessionCookie);
		} catch (Exception e) {
			handleException(e.getMessage(), e);
		}
	}

	private void handleException(String msg, Exception e) throws Exception {
		log.error(msg, e);
		throw new Exception(msg, e);
	}
	

	public ProvisioningActionResultInfo reviewInstallFeaturesAction(
			FeatureInfo[] features) throws Exception {
		ProvisioningActionResultInfo provisioningActionResultInfo = null;
		try {
			ProvisioningActionInfo provisioningActionInfo = new ProvisioningActionInfo();
			provisioningActionInfo.setFeaturesToInstall(features);
			provisioningActionInfo
					.setActionType(OperationFactory.INSTALL_ACTION);
			provisioningActionResultInfo = provAdminStub
					.reviewProvisioningAction(provisioningActionInfo);
		} catch (AxisFault e) {
			handleException(e.getMessage(), e);
		}
		return provisioningActionResultInfo;
	}

    public ProvisioningActionResultInfo reviewUninstallFeaturesAction(FeatureInfo[] features) throws Exception {
        ProvisioningActionResultInfo provisioningActionResultInfo = null;
        try {
            ProvisioningActionInfo provisioningActionInfo = new ProvisioningActionInfo();
            provisioningActionInfo.setFeaturesToUninstall(features);
            provisioningActionInfo.setActionType(OperationFactory.UNINSTALL_ACTION);
            return provAdminStub.reviewProvisioningAction(provisioningActionInfo);
        } catch (AxisFault e) {
			handleException(e.getMessage(), e);
		}
        return provisioningActionResultInfo;
    }
    
	public LicenseInfo[] getLicensingInformation() throws Exception {
		LicenseInfo[] licenseInfo = null;
		try {
			licenseInfo = provAdminStub.getLicensingInformation();
		} catch (AxisFault e) {
			handleException(e.getMessage(), e);
		}
		return licenseInfo;
	}

	public void performInstallation(String actionType) throws Exception {
		try {
			if (CarbonUtils.isRunningOnLocalTransportMode()) {
				provAdminStub.performProvisioningAction(actionType);
			} else {
				ServiceClient client = provAdminStub._getServiceClient();
				client.engageModule("addressing"); // IMPORTANT
				Options options = client.getOptions();
				options.setUseSeparateListener(true);
				options.setTransportInProtocol(Constants.TRANSPORT_HTTP);
				provAdminStub.startperformProvisioningAction(actionType,
						callback);
				handleCallback();
			}
		} catch (AxisFault e) {
			handleException(e.getMessage(), e);
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

	ProvisioningAdminServiceCallbackHandler callback = new ProvisioningAdminServiceCallbackHandler() {
		@Override
		public void receiveResultperformProvisioningAction(boolean result) {
			isComplete = true;
		}

		@Override
		public void receiveErrorperformProvisioningAction(Exception e) {
			isError = true;
			exception = e;
		}

		@Override
		public void receiveResultremoveAllConsoleFeatures(boolean result) {
			isComplete = true;
		}

		@Override
		public void receiveErrorremoveAllConsoleFeatures(Exception e) {
			isError = true;
			exception = e;
		}

		@Override
		public void receiveResultremoveAllServerFeatures(boolean result) {
			isComplete = true;
		}

		@Override
		public void receiveErrorremoveAllServerFeatures(Exception e) {
			isError = true;
			exception = e;
		}

		@Override
		public void receiveResultgetProfileHistory(ProfileHistory[] result) {
			profileHistories = Arrays.copyOf(result, result.length);
			isComplete = true;
		}

		@Override
		public void receiveErrorgetProfileHistory(Exception e) {
			isError = true;
			exception = e;
		}
	};

}
