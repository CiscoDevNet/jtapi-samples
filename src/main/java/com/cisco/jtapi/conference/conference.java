package com.cisco.jtapi.conference;

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

// Implements a 3-way ad hoc conference senario.

// Devices used / requirements (configure these in .env):
//   * ALICE_DN / any phone
//   * BOB_DN / any phone
//   * CHARLIE_DN / any phone

// Scenario:
// 1. ALICE_DN makes a call to BOB_DN
// 2. BOB_DN answers the call
// 3. CHARLIE_DN makes a call to ALICE_DN
// 4. ALICE_DN answers the call
// 5. The calls are conferenced at ALICE_DN

// Be sure to rename .env.example to .env and configure your CUCM/user/DN
//   details for the scenario.

// Tested using:
//   Ubuntu Linux 20.10
//   OpenJDK 11.0.9
//   CUCM 12.5

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.telephony.*;

import com.cisco.jtapi.extensions.*;

import io.github.cdimascio.dotenv.Dotenv;

public class conference {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS");

    public static CiscoAddress fromAddress;
    public static CiscoAddress toAddress;
    public static CiscoAddress confAddress;
    public static CiscoTerminal fromTerminal;
    public static CiscoTerminal toTerminal;
    public static CiscoTerminal confTerminal;

    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    public static void main(String[] args)
            throws JtapiPeerUnavailableException, InvalidArgumentException, ResourceUnavailableException,
            MethodNotSupportedException, PrivilegeViolationException, InvalidPartyException, InvalidStateException {

        // Retrieve environment variables from .env, if present
        Dotenv dotenv = Dotenv.load();

        // The handler class provide observers for provider/address/terminal/call
        // events
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
        fromAddress = (CiscoAddress) provider.getAddress(dotenv.get("ALICE_DN"));
        log("Awaiting CiscoAddrInServiceEv for: " + fromAddress.getName() + "...");
        fromAddress.addObserver(handler);
        handler.fromAddressInService.waitTrue();
        // Add a call observer to receive call events
        fromAddress.addCallObserver(handler);
        // Get/open the first Terminal for the Address. Could be multiple
        // if it's a shared line
        fromTerminal = (CiscoTerminal) fromAddress.getTerminals()[0];
        log("Awaiting CiscoTermInServiceEv for: " + fromTerminal.getName() + "...");
        fromTerminal.addObserver(handler);
        handler.fromTerminalInService.waitTrue();

        // Open the BOB_DN Address and wait for it to go in service
        log("Opening toAddress DN: " + dotenv.get("BOB_DN"));
        toAddress = (CiscoAddress) provider.getAddress(dotenv.get("BOB_DN"));
        log("Awaiting CiscoAddrInServiceEv for: " + toAddress.getName() + "...");
        toAddress.addObserver(handler);
        handler.toAddressInService.waitTrue();
        // Add a call observer to receive call events
        toAddress.addCallObserver(handler);
        // Get/open the first Terminal for the Address. Could be multiple
        // if it's a shared line
        toTerminal = (CiscoTerminal) toAddress.getTerminals()[0];
        log("Awaiting CiscoTermInServiceEv for: " + toTerminal.getName() + "...");
        toTerminal.addObserver(handler);
        handler.toTerminalInService.waitTrue();

        // Open the CHARLIE_DN Address and wait for it to go in service
        log("Opening confAddress DN: " + dotenv.get("CHARLIE_DN"));
        confAddress = (CiscoAddress) provider.getAddress(dotenv.get("CHARLIE_DN"));
        log("Awaiting CiscoAddrInServiceEv for: " + confAddress.getName() + "...");
        confAddress.addObserver(handler);
        handler.confAddressInService.waitTrue();
        // Add a call observer to receive call events
        confAddress.addCallObserver(handler);
        // Get/open the first Terminal for the Address. Could be multiple
        // if it's a shared line
        confTerminal = (CiscoTerminal) confAddress.getTerminals()[0];
        log("Awaiting CiscoTermInServiceEv for: " + confTerminal.getName() + "...");
        confTerminal.addObserver(handler);
        handler.confTerminalInService.waitTrue();

        // Make a call from ALICE to BOB

        // Create a new Call object from our provider
        CiscoCall origCall = (CiscoCall) provider.createCall();

        log("Connecting original call from " + dotenv.get("ALICE_DN") + " to DN: " + dotenv.get("BOB_DN"));
        log("Awaiting CallCtlTermConnRingingEv for Call: " + origCall.toString() + "...");
        origCall.connect(fromTerminal, fromAddress, toAddress.getName());
        // Wait for the call to ring
        handler.origCallRinging.waitTrue();

        // Answer the ALICE->BOB call
        log("Answering original call from DN: " + fromAddress.getName());
        toTerminal.getTerminalConnections()[0].answer();
        log("Awaiting CallCtlTermConnTalkingEv for original Call: " + origCall.toString() + "...");
        handler.origCallTalking.waitTrue();

        // Make a call from CHARLIE to ALICE

        // Create a new Call object from our provider
        CiscoCall secondCall = (CiscoCall) provider.createCall();

        log("Connecting second call from " + confAddress.getName() + " to DN: " + fromAddress.getName());
        log("Awaiting CallCtlTermConnRingingEv for second Call: " + secondCall.toString() + "...");
        secondCall.connect(confTerminal, confAddress, fromAddress.getName());
        // Wait for the call to ring
        handler.secondCallRinging.waitTrue();

        // Answer the CHARLIE->ALICE call
        log("Answering second call from DN: " + confAddress.getName());
        fromTerminal.getTerminalConnections()[1].answer();
        log("Awaiting CallCtlTermConnTalkingEv for second Call: " + secondCall.toString() + "...");
        handler.secondCallTalking.waitTrue();

        // Conference the calls at ALICE
        log("Conferencing calls");
        origCall.conference(secondCall);

        log("Done.");
        System.exit(0);
    }

}
