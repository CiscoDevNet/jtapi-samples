package com.cisco.jtapi.makecall;

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

// Basic make call example.

// Devices used / requirements (configure these in .env):
//   * ALICE_DN / CTI supported phone,associated with JTAPI user
//   * BOB_DN / any phone

// Scenario:
// 1. ALICE_DN creates/connects a call to BOB_DN
// 2. The call rings for 5 seconds (optionally it can be manually answered)
// 3. ALICE_DN drops the call

// Be sure to rename .env.example to .env and configure your CUCM/user/DN
//   details for the scenario.

// Tested using:

// Ubuntu Linux 20.04
// OpenJDK 11.0.8
// CUCM 11.5

import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;  

import javax.telephony.*;
import com.cisco.jtapi.extensions.*;

import io.github.cdimascio.dotenv.Dotenv;

public class makeCall {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS"); 

    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    public static void main(String[] args) throws

    JtapiPeerUnavailableException, ResourceUnavailableException, MethodNotSupportedException, InvalidArgumentException,
            PrivilegeViolationException, InvalidPartyException, InvalidStateException, InterruptedException {

        // Retrieve environment variables from .env, if present
        Dotenv dotenv=Dotenv.load();

        // The Handler class provides observers for provider/address/terminal/call events
        Handler handler = new Handler();

        // Create the JtapiPeer object, representing the JTAPI library
        log("Initializing Jtapi");
        CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory.getJtapiPeer(null);

        // Create and open the Provider, representing a JTAPI connection to CUCM CTI
        // Manager
        String providerString = String.format(
            "%s;login=%s;passwd=%s",
            dotenv.get("CUCM_ADDRESS"),
            dotenv.get("JTAPI_USERNAME"),
            dotenv.get("JTAPI_PASSWORD"));
        log("Connecting Provider: " + providerString);
        CiscoProvider provider=(CiscoProvider) peer.getProvider(providerString);
        log("Awaiting ProvInServiceEv...");
        provider.addObserver(handler);
        handler.providerInService.waitTrue();

        // Open the ALICE_DN Address and wait for it to go in service
        log("Opening fromAddress DN: " + dotenv.get("ALICE_DN"));
        CiscoAddress fromAddress = (CiscoAddress) provider.getAddress(dotenv.get("ALICE_DN"));
        log("Awaiting CiscoAddrInServiceEv for: " + fromAddress.getName() + "...");
        fromAddress.addObserver(handler);
        handler.fromAddressInService.waitTrue();
        // Add a call observer to receive call events
        fromAddress.addCallObserver(handler);
        // Get/open the first Terminal for the Address.  Could be multiple
        //   if it's a shared line
        CiscoTerminal fromTerminal = (CiscoTerminal) fromAddress.getTerminals()[0];
        log("Awaiting CiscoTermInServiceEv for: " + fromTerminal.getName() + "...");
        fromTerminal.addObserver(handler);
        handler.fromTerminalInService.waitTrue();

        // Create a new Call object from our provider
        CiscoCall call = (CiscoCall) provider.createCall();

        log("Creating/connecting call to DN: "+dotenv.get("BOB_DN"));
        log("Awaiting CallActiveEv for Call: " + call.toString() + "...");
        call.connect(fromTerminal, fromAddress, dotenv.get("BOB_DN"));
        handler.callActive.waitTrue();

        // Wait 5 sec, then drop the call
        log("Sleeping 5 seconds...");
        Thread.sleep(5000);

        log("Dropping call: " + call.toString());
        call.drop();

        System.exit(0);
    }
}
