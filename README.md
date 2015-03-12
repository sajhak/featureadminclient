## Feature Install Client
## ----------------------

1. Configure features.xml file
        
Add the features you want to install in features.xml file with feaure id and version.
The features.xml file provided has all the features need when installing Key Manager (API Manager 1.8) features into IS (5.0.0)

2. Configure conf.properties file
    
        keystore.path --> File path to keystore (wso2carbon.jks)
        keystore.password --> Key store password
        server.url --> Server URL into which the features need to be installed (https://localhost:9443)
        repo.path --> Point to a carbon server's <CARBON_HOME>/repository/deployment/client directory. If not, copy the content of that directory to
              a convenient location, and point to that.
        feature.list.file --> File path to the list of features need to be installed. Should be an xml file, with the same format as the provided one.
        user.name --> Carbon admin user name. (This user should have the permissions to invoke Feature management admin service)
        user.password --> User password
        p2.repo.url --> P2 repo url
        p2.repo.name --> Name for the p2 repo
        p2.repo.local --> Whether you are pointing to a local repo or not  (true/false)

3. Execute the client
         ./bin.sh      

4. Logs will be recorded in logs/application.log
