package com.cisco.jtapi.sendData;

// Copyright (c) 2020 Cisco and/or its affiliates.
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// Opens a phone and performs a CiscoTerminal.sendData() request to send an
// IP Phone Services XML 'Hello World' message to the phone's display.

// Devices used / requirements (configure these in .env):
//   * ALICE_DN / CTI supported phone,IPPS supported phone,associated with JTAPI user

// Scenario:
// 1. ALICE_DN requests CiscoTerminal.sendData()

// Be sure to rename .env.example to .env and configure your CUCM/user/DN
//   details for the scenario.

// Tested using:
//   Ubuntu Linux 20.04
//   OpenJDK 11.0.8
//   CUCM 11.5

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.telephony.*;
import com.cisco.jtapi.extensions.*;

import io.github.cdimascio.dotenv.Dotenv;

public class sendData {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS");

    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    public static void main(String[] args) throws

    JtapiPeerUnavailableException, ResourceUnavailableException, MethodNotSupportedException, InvalidArgumentException,
            PrivilegeViolationException, InvalidPartyException, InvalidStateException, InterruptedException {

        // Retrieve environment variables from .env, if present
        Dotenv dotenv = Dotenv.load();
        
        // The handler class provids observers for provider/address/terminal/call
        // events ALICE_DN
        Handler handler = new Handler();

        // Create the JtapiPeer object, representing the JTAPI library
        log("Initializing Jtapi");
        CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory.getJtapiPeer(null);

        // Create and open the Provider, representing a JTAPI connection to CUCM CTI
        // Manager
        String providerString = String.format("%s;login=%s;passwd=%s", dotenv.get("CUCM_ADDRESS"),
                dotenv.get("JTAPI_USERNAME"), dotenv.get("JTAPI_PASSWORD"));
        log("Connecting Provider: " + providerString);
        CiscoProvider provider = (CiscoProvider) peer.getProvider(providerString);
        provider.addObserver(handler);
        log("Awaiting ProvInServiceEv...");
        handler.providerInService.waitTrue();
        
        // Open the ALICE_DN Address and wait for it to go in service
        log("Opening fromAddress DN: " + dotenv.get("ALICE_DN"));
        CiscoAddress phoneAddress = (CiscoAddress) provider.getAddress(dotenv.get("ALICE_DN"));
        log("Awaiting CiscoAddrInServiceEv for: " + phoneAddress.getName() + "...");
        phoneAddress.addObserver(handler);
        handler.phoneAddressInService.waitTrue();
        // Add a call observer to receive call events
        phoneAddress.addCallObserver(handler);

        // Get/open the first Terminal for the Address.  Could be multiple
        //   if it's a shared line
        CiscoTerminal phoneTerminal = (CiscoTerminal) phoneAddress.getTerminals()[0];
        log("Awaiting CiscoTermInServiceEv for: " + phoneTerminal.getName() + "...");
        phoneTerminal.addObserver(handler);
        handler.fromTerminalInService.waitTrue();

        // Send an IP Phone Services XML object to the phone's display
        log("Sending <CiscoIPPhoneText> object to: " + phoneTerminal.getName());

        phoneTerminal.sendData("<CiscoIPPhoneText><Text>Hello World</Text></CiscoIPPhoneText>");

        // Wait 5 seconds, then clear the phone display
        log(("Sleeping 5 seconds..."));
        Thread.sleep(5000);

        log("Sending <CiscoIPPhoneExecute> object to: " + phoneTerminal.getName());
        phoneTerminal.sendData("<CiscoIPPhoneExecute><ExecuteItem URL='Init:Services' /></CiscoIPPhoneExecute>");

        log("Done.");
        System.exit(0);
    }
}
