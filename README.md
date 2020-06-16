# jtapi-samples

## Overview

Sample Java programs demonstrating usage of the Cisco JTAPI API for call control.

Visit the [DevNet JTAPI Site](https://developer.cisco.com/site/jtapi)

## Available samples

- `makeCall` - simple A->B make call example.

- `sendData` - use CiscoTerminal.sendData() to send an [IP Phone Services](https://developer.cisco.com/site/ip-phone-services/) XML display object to a phone.

- `superProvider_deviceStateServer` - use CiscoProvider.createTerminal() to dynamically create a terminal by device name using the Cisco JTAPI 'Superprovider' feature.  Then retrieves and monitors the device for device-side status changes using the 'Device State Server' feature.

## Requirements

- [Oracle Java](https://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8

- [Apache Maven](https://maven.apache.org/) 3.6.0

- [Visual Studio Code](https://code.visualstudio.com/)

- A working Cisco Unified Communications Manager environment:

    - An CUCM application-user or end-user username/password, with the following roles:

        - `Standard CTI Allow Control of Phones supporting Connected Xfer and conf`

        - `Standard CTI Allow Control of Phones supporting Rollover Mode`

        - `Standard CTI Enabled`

    - A [CTI controllable phone device](https://developer.cisco.com/site/jtapi/documents/cti-tapi-jtapi-supported-device-matrix/) (such as a Jabber Windows/Mac client) configured with at least one directory number, associated to the CUCM user

        >Note, ensure at least one directory number has `Allow Control of Device from CTI` enabled

    - Ideally a destination phone number for testing (though you could have the phone call its own number)

## Getting started

1. Make sure you have Oracle Java SE 1.8 installed, and `java` is available in the path

    ```bash
    $ java -version
    java version "1.8.0_201"
    Java(TM) SE Runtime Environment (build 1.8.0_201-b09)
    Java HotSpot(TM) 64-Bit Server VM (build 25.201-b09, mixed mode)
    ```

1. Open a terminal window and use `git` to clone this repository:

    ```bash
    git clone https://github.com/CiscoDevNet/jtapi-samples.git
    ```

1. Open the Java project in [Visual Studio Code](https://code.visualstudio.com/):

    ```bash
    cd jtapi-samples
    code .
    ```

1. Edit `.vscode/launch.json` to specify environment variable config for the samples you wish to run

1. Finally, to launch the sample in VS Code, select the **Run** panel, choose the desired `Debug (Launch) ...` option, and click the green 'Start Debugging' arrow:

        ![Launch](images/launch.png)

## Notes

1. In this project, the 11.5 version of the JTAPI Java libraries have been deployed to the local Maven repo in `lib/`.  If  you want to use a different version in the project:

    - Download and install the JTAPI plugin from CUCM (**Applications** / **Plugins**)

    - From this repository's root, use Maven to deploy the new version of `jtapi.jar` to the local repo.  You will need to identify the full path to the `jtapi.jar` installed above:

        ```bash
        mvn deploy:deploy-file -DgroupId=com.cisco.jtapi -DartifactId=jtapi -Dversion={version} -Durl=file:./lib -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile={/path/to/jtapi.jar}
        ```

        >Note: be sure to update {version} and {/path/to/jtapi.jar} in the command

    - Modify `pom.xml` to specify the new JTAPI version dependency.  Modify `<version>`:

        ```xml
        <dependency>
            <groupId>com.cisco.jtapi</groupId>
            <artifactId>jtapi</artifactId>
            <version>11.5</version>
        </dependency>
        ```

1. JTAPI configuration - e.g. trace log number/size/location and various timeouts - can be configured in `jtapi_config/jtapi.ini` (defined as a resource in `pom.xml`)