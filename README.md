## Feature Install Client
## ----------------------

Feature install client can be used to install or uninstall features from a WSO2 Carbon Server.

1. Configure installfeatures.xml / uninstallfeatures.xml files (File names are configurable)
        
Add the features you want to install in installfeatures.xml file with feaure id and version.
The installfeatures.xml file provided has all the features need when installing Key Manager (API Manager 1.8) features into IS (5.0.0)

If you want to uninstall features, configure a file with feature id and versions, which need to be uninstalled.

2. Configure conf.properties file
    
        keystore.path --> File path to keystore (wso2carbon.jks)
        keystore.password --> Key store password
        server.url --> Server URL into which the features need to be installed (https://localhost:9443)
        repo.path --> Point to a carbon server's <CARBON_HOME>/repository/deployment/client directory. If not, copy the content of that directory to
              a convenient location, and point to that.
        install.feature.list.file --> File path to the list of features need to be installed. Should be an xml file, with the same format as the provided one.
        uninstall.feature.list.file --> File path to the list of features need to be uninstalled. Should be an xml file, with the same format as the provided one.
        user.name --> Carbon admin user name. (This user should have the permissions to invoke Feature management admin service)
        user.password --> User password
        p2.repo.url --> P2 repo url
        p2.repo.name --> Name for the p2 repo
        p2.repo.local --> Whether you are pointing to a local repo or not  (true/false)

3. Execute the client

         ./bin.sh i --> To install features  
        
Note: After installing features, the server should be restarted.

         ./bin.sh u --> To uninstall features      

4. Logs will be recorded in logs/application.log
