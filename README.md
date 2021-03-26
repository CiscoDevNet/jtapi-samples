# jtapi-samples

## Overview

Sample Java programs demonstrating usage of the Cisco JTAPI API for call control.

Visit the [DevNet JTAPI Site](https://developer.cisco.com/site/jtapi)

## Available samples

- `makeCall` - basic make-call example.

- `sendData` - Opens a phone and performs a CiscoTerminal.sendData() request to send an [IP Phone Services](https://developer.cisco.com/site/ip-phone-services/) 'Hello World' message to the phone's display.

- `superProvider_deviceStateServer` - Demonstrates using CiscoProvider.createTerminal() to dynamically create a terminal by device name using the 'Superprovider' feature, then retrieves and monitors the device for device-side status changes using the 'Device State Server' feature.

- `dialViaOffice` - Implements a 'dial via office' senario, where a phone calls a CTI Route Point, which makes a new outbound call to a target DN, and then redirects both calls to a CTI Port which transfers the two calls together.

- `conference` - Implements a 3-way ad hoc conference scenario

## Requirements

- [OpenJDK](https://openjdk.java.net/) 11

- [Apache Maven](https://maven.apache.org/) 3.6.3

- [Visual Studio Code](https://code.visualstudio.com/)

- A working Cisco Unified Communications Manager environment:

    - An CUCM application-user or end-user username/password, with the following roles:

        - `Standard CTI Allow Control of Phones supporting Connected Xfer and conf`

        - `Standard CTI Allow Control of Phones supporting Rollover Mode`

        - `Standard CTI Enabled`

        - `Standard CTI Allow Control of all Devices`

    - One (or three, to try all sample scenarios) [CTI suported phone devices](https://developer.cisco.com/site/jtapi/documents/cti-tapi-jtapi-supported-device-matrix/) (includes Cisco IP Communicator and Jabber soft phones), each configured with one directory number, each associated to the CUCM user.

        >Note, ensure directory numbers have `Allow Control of Device from CTI` enabled

## Getting started

1. Make sure you have OpenJDK 11 installed, `java` is available in the path, and `$JAVA_HOME` points to the right directory:

    ```bash
    $ java -version
    openjdk 11.0.9.1 2020-11-04
    OpenJDK Runtime Environment (build 11.0.9.1+1-Ubuntu-0ubuntu1.20.10)
    OpenJDK 64-Bit Server VM (build 11.0.9.1+1-Ubuntu-0ubuntu1.20.10, mixed mode, sharing)
    ```

    ```bash
    $ echo $JAVA_HOME
    /usr/lib/jvm/java-1.11.0-openjdk-amd64
    ```

1. Open a terminal and use `git` to clone this repository:

    ```bash
    git clone https://github.com/CiscoDevNet/jtapi-samples.git
    ```

1. Open the Java project in [Visual Studio Code](https://code.visualstudio.com/):

    ```bash
    cd jtapi-samples
    code .
    ```

1. Edit rename `.env.example` to `.env`, and edit to specify environment variable config for the samples you wish to run.

1. Finally, to launch the sample in VS Code, select the **Run** panel, choose the desired `Launch...` option from the drop-down in the upper left, and click the green 'Start Debugging' arrow (or hit **F5**)

    ![Launch](images/launch.png)

## Notes

1. In this project, the 11.5 and 12.5 versions of the JTAPI Java library have been deployed to the project's local Maven repo (in `lib/`), with 12.5 being the configured version. 

    If you want to use 11.5 (or you deploy another version, as below), modify `pom.xml` to specify the desired JTAPI version dependency.  Modify `<version>`:

    ```xml
    <dependency>
        <groupId>com.cisco.jtapi</groupId>
        <artifactId>jtapi</artifactId>
        <version>12.5</version>
    </dependency>
    ```

1.  If  you want to use another JTAPI version in the project:

    * Download and install/extract the JTAPI plugin from CUCM (**Applications** / **Plugins**)

    * From this repository's root, use Maven to deploy the new version of `jtapi.jar` to the local repo.  You will need to identify the full path to the new `jtapi.jar` installed above:

        ```bash
        mvn deploy:deploy-file -DgroupId=com.cisco.jtapi -DartifactId=jtapi -Dversion={version} -Durl=file:./lib -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile={/path/to/jtapi.jar}
        ```

        >Note: be sure to update {version} and {/path/to/jtapi.jar} to your actual values

1. JTAPI configuration - e.g. trace log number/size/location and various timeouts - can be configured in `jtapi_config/jtapi.ini` (defined as a resource in `pom.xml`)

1. As of v12.5, the Cisco `jtapi.jar` does not implement the [Java Platform Module System](https://www.oracle.com/corporate/features/understanding-java-9-modules.html) (JPMS).  See this [issue](https://github.com/CiscoDevNet/jtapi-samples/issues/1) for more info.