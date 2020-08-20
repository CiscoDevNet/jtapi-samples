package com.cisco.jtapi.dialviaoffice;

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

// Implements a 'dial via office' senario, where a phone calls a CTI Route Point,
// which makes a new outbound call to a target DN, and then redirects both calls
// to a CTI Port which transfers the two calls together.

// Devices used / requirements (configure these in .env):
//   * ALICE_DN / any phone
//   * CTI_ROUTE_POINT / associated with JTAPI user
//   * CTI-PORT / associated with JTAPI user
//   * BOB_DN / any phone

// Scenario:
// 1. ALICE_DN makes a manual dialin call to CTI_ROUTE_POINT
// 2. CTI_ROUTE_POINT answers/holds the incoming call
// 3. CTI_ROUTE_POINT creates/connects a new dialout call to ALICE_DN
// 4. BOB_DN manually answers the incoming call
// 5. CTI_ROUTE_POINT redirects the held dialin call to CTI_PORT_DN
// 6. CTI_PORT_DN answers/holds the incoming dialin call
// 7. CTI_ROUTE_POINT redirects the dialout call to CTI_PORT_DN
// 8. CTI_PORT_DN answers the incoming dialout call
// 9. CTI_PORT_DN transfers the dialin call to the dialout call

// Be sure to rename .env.example to .env and configure your CUCM/user/DN
//   details for the scenario.

// Tested using:
//   Ubuntu Linux 20.04
//   OpenJDK 11.0.8
//   CUCM 11.5

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.telephony.*;
import javax.telephony.callcontrol.*;
import javax.telephony.callcontrol.CallControlTerminalConnection;
import javax.telephony.callcontrol.events.CallCtlTermConnRingingEv;

import com.cisco.jtapi.extensions.*;
import com.cisco.cti.util.Condition;

import io.github.cdimascio.dotenv.Dotenv;

public class dialViaOffice {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS");

    public static CallCtlTermConnRingingEv rpDialinCallEvent;
    public static CallCtlTermConnRingingEv ctipInboundCallEvent;
    public static CiscoMediaOpenLogicalChannelEv rpOpenLogicalChannelEvent;
    public static CiscoAddress rpAddress;
    public static CiscoAddress dialoutAddress;

    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    public static void main(String[] args) throws

    JtapiPeerUnavailableException, ResourceUnavailableException, MethodNotSupportedException, InvalidArgumentException,
            PrivilegeViolationException, InvalidPartyException, InvalidStateException, InterruptedException,
            CiscoRegistrationException, UnknownHostException, SocketException {

        // Retrieve environment variables from .env, if present
        Dotenv dotenv = Dotenv.load();

        // Determine this PC's address and get an ephemeral port number
        // for registering RTP media for the Route Point
        InetAddress rpRtpAddress = InetAddress.getLocalHost();
        InetAddress ctipRtpAddress = rpRtpAddress;
        DatagramSocket sock1 = new DatagramSocket();
        int rpRtpPort = sock1.getLocalPort();
        DatagramSocket sock2 = new DatagramSocket();
        int ctipRtpPort = sock2.getLocalPort();
        sock1.close();
        sock2.close();

        // The handler classes provide observers for provider/address/terminal/call
        // events for CTI_ROUTE_POINT and CTI_PORT
        Handler handler = new Handler();
        CtiPortHandler ctipHandler = new CtiPortHandler();

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

        // Get/open the CTI_ROUTE_POINT Address
        log("Opening/registering CTI Route Point DN: " + dotenv.get("CTI_ROUTE_POINT_DN"));
        rpAddress = (CiscoAddress) (provider.getAddress(dotenv.get("CTI_ROUTE_POINT_DN")));
        rpAddress.addObserver(handler);
        
        CiscoRouteTerminal rpTerminal = (CiscoRouteTerminal) rpAddress.getTerminals()[0];
        rpTerminal.addObserver(handler);
        rpTerminal.register(new CiscoMediaCapability[] { CiscoMediaCapability.G711_64K_30_MILLISECONDS },
                CiscoRouteTerminal.DYNAMIC_MEDIA_REGISTRATION);
        log("Awaiting CiscoTermInServiceEv for: " + rpTerminal.getName() + "...");
        handler.rpTerminalInService.waitTrue();
        log("Awaiting CiscoAddrInServiceEv for: " + rpAddress.getName() + "...");
        handler.rpAddressInService.waitTrue();
        // Enable auto accept for incoming offering calls
        rpAddress.setAutoAcceptStatus(CiscoAddress.AUTOACCEPT_ON, rpTerminal);
        // Add a call observer to receive call events
        rpAddress.addCallObserver(handler);

        log("Opening/registering CTI Port DN: " + dotenv.get("CTI_PORT_DN"));
        // Retrieve and open the Address (line) object for the CTI Port
        CiscoAddress ctipAddress = (CiscoAddress) provider.getAddress(dotenv.get("CTI_PORT_DN"));
        ctipAddress.addObserver(ctipHandler);
        // Register the CTI Port media terminal
        CiscoMediaTerminal ctipTerminal = (CiscoMediaTerminal) ctipAddress.getTerminals()[0];
        ctipTerminal.addObserver(ctipHandler);
        ctipTerminal.register(ctipRtpAddress, ctipRtpPort,
                new CiscoMediaCapability[] { CiscoMediaCapability.G711_64K_30_MILLISECONDS });
        log("Awaiting CiscoTermInServiceEv for: " + ctipTerminal.getName() + "...");
        ctipHandler.ctipTerminalInService.waitTrue();
        log("Awaiting CiscoAddrInServiceEv for: " + ctipAddress.getName() + "...");
        ctipHandler.ctipAddressInService.waitTrue();
        // Enable auto accept for incoming offering calls
        ctipAddress.setAutoAcceptStatus(CiscoAddress.AUTOACCEPT_ON, ctipTerminal);
        // Add a call observer to receive call events
        ctipAddress.addCallObserver(ctipHandler);

        // Wait for an inbound call on the CTI Route Point
        log("Ready for dialin call at CTI Route Point DN: " + rpAddress.getName());
        log("Awaiting dialin CallCtlTermConnRingingEv for: " + rpTerminal.getName() + "...");
        handler.rpDialinCallRinging.waitTrue();

        // Via the newly populated rpInboundCallEvent (see handler), drill/cast down to
        // a
        // CallControlTerminalConnection so we can do some operations
        CallControlTerminalConnection rpIncomingCctConnection = (CallControlTerminalConnection) rpDialinCallEvent
                .getTerminalConnection();

        // Answer the dialin call on the CTI Route Point
        log("Answering dialin call from DN: " + rpDialinCallEvent.getCallingAddress().getName());
        rpIncomingCctConnection.answer();
        // Wait for the OLC event and provide dynamic RTP media details
        log("Awaiting dialin CiscoMediaOpenLogicalChannelEv for: " + rpIncomingCctConnection.getTerminal().getName());
        handler.rpOpenLogicalChannel.waitTrue();
        ((CiscoRouteTerminal) rpOpenLogicalChannelEvent.getTerminal()).setRTPParams(
                rpOpenLogicalChannelEvent.getCiscoRTPHandle(), new CiscoRTPParams(rpRtpAddress, rpRtpPort));

        log("Awaiting dialin CallCtlTermConnTalkingEv for: " + rpIncomingCctConnection.getTerminal().getName() + "...");
        handler.rpDialinCallTalking.waitTrue();
        log("Holding dialin call on: " + rpIncomingCctConnection.getConnection().getAddress().getName());
        rpIncomingCctConnection.hold();
        log("Awaiting dialin CallCtlTermConnHeldEv for: " + rpIncomingCctConnection.getTerminal().getName() + "...");
        handler.rpDialinCallHeld.waitTrue();

        // Create a new outbound call for the dialout leg
        log("Making dialout call to DN: " + dotenv.get("BOB_DN"));
        Call dialoutCall = provider.createCall();
        Connection[] dialoutConnections = dialoutCall.connect(rpTerminal, rpAddress, dotenv.get("BOB_DN"));
        if (dialoutConnections[0].getAddress().getName().equals(dotenv.get("BOB_DN"))) {
            dialoutAddress = (CiscoAddress) dialoutConnections[0].getAddress();
        } else {
            dialoutAddress = (CiscoAddress) dialoutConnections[1].getAddress();
        }
        // Wait for the dialout call to be answered
        log("Awaiting dialout CallCtlConnEstablishedEv for DN: " + dialoutAddress.getName() + "...");
        handler.dialoutCallEstablished.waitTrue();

        log("Redirecting dialin call to CTI Port DN: " + ctipAddress.getName());
        ((CallControlConnection) rpIncomingCctConnection.getConnection()).redirect(ctipAddress.getName());
        log("Awaiting CTI Port dialin CallCtlTermConnRingingEv for: " + ctipTerminal.getName() + "...");
        ctipHandler.ctipCallRinging.waitTrue();

        // Via the newly populated ctipInboundCallEvent (see handler), drill/cast down
        // to a
        // CallControlTerminalConnection so we can do some operations
        CallControlTerminalConnection ctipIncomingCctConnection = (CallControlTerminalConnection) ctipInboundCallEvent
                .getTerminalConnection();

        log("Answering CTI Port dialin call from DN: " + ctipInboundCallEvent.getCallingAddress().getName());
        ctipIncomingCctConnection.answer();
        log("Awaiting CTI Port dialin CallCtlTermConnTalkingEv for: "
                + ctipIncomingCctConnection.getTerminal().getName() + "...");
        ctipHandler.ctipCallTalking.waitTrue();
        log("Holding CTI Port dialin call on DN: " + ctipIncomingCctConnection.getConnection().getAddress().getName());
        ctipIncomingCctConnection.hold();
        log("Awaiting CTI Port dialin CallCtlTermConnHeldEv for: " + ctipIncomingCctConnection.getTerminal().getName()
                + "...");

        // Retrieve the Call object for the CTI Port dialin inbound call
        CallControlCall ctipInboundCallLeg = (CallControlCall) ctipIncomingCctConnection.getConnection().getCall();
        log("Redirecting dialout call to CTI Port DN: " + ctipAddress.getName());
        ((CallControlConnection) dialoutConnections[0]).redirect(ctipAddress.getName());
        log("Awaiting CTI Port dialout CallCtlTermConnRingingEv for: " + ctipTerminal.getName() + "...");
        ctipHandler.ctipCallRinging = new Condition();
        ctipHandler.ctipCallRinging.waitTrue();

        // Via the newly populated ctipInboundCallEvent, drill/cast down to a
        // CallControlTerminalConnection so we can do some operations
        ctipIncomingCctConnection = (CallControlTerminalConnection) ctipInboundCallEvent.getTerminalConnection();

        log("Answering CTI Port dialout call from DN: " + ctipInboundCallEvent.getCallingAddress().getName());
        ctipIncomingCctConnection.answer();
        log("Awaiting CTI Port dialout CallCtlTermConnTalkingEv for: "
                + ctipIncomingCctConnection.getTerminal().getName() + "...");
        ctipHandler.ctipCallTalking = new Condition();
        ctipHandler.ctipCallTalking.waitTrue();

        // Retrieve the Call object for the CTI Port dialout inbound call
        CallControlCall ctipOutboundCallLeg = (CallControlCall) ctipIncomingCctConnection.getConnection().getCall();

        log("Transfering dialin call to dialout call");
        ctipInboundCallLeg.transfer(ctipOutboundCallLeg);
        log("Awaiting CTI Port dialin CiscoTransferEndEv...");
        ctipHandler.ctipTransferCompleted.waitTrue();

        log("Done.");
        System.exit(0);
    }

}
