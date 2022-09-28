package com.cisco.jtapi.playmedia;

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

// Answer an inbound call on a CTI Port, then play an audio file using the GStreamer framework

// Devices used / requirements (configure these in .env):
//   * CTI_PORT_DN / CTI Port associated with JTAPI user
//   * Any other phone to make the call to the CTI Port

// Scenario:
// 1. A call is placed to the CTI Port
// 2. The CTI Port answers the call
// 3. The GStreamer framework is used to stream an audio file to the dynamic IP/port of the caller
// 3. CTI_PORT_DN drops the call

// Be sure to rename .env.example to .env and configure your CUCM/user/DN
//   details for the scenario.

// Tested using:

// Ubuntu Linux 22.04
// Oracle JDK 1.8
// OpenJDK 11.0.8
// CUCM 11.5 / 14

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

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.GstObject;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Bus;

import io.github.cdimascio.dotenv.Dotenv;

public class playMedia {

    public static CallCtlTermConnRingingEv ctipDialinCallEvent;
    public static CiscoRTPOutputStartedEv ctipRTPOutputStartedEvent;
    private static Pipeline pipeline;
    private static Bus bus;
    
    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss.SS"); 
    private static void log(String msg) {
        System.out.println(dtf.format(LocalDateTime.now()) + " " + msg);
    }

    public static void main(String[] args) throws

    JtapiPeerUnavailableException, ResourceUnavailableException, MethodNotSupportedException, InvalidArgumentException,
            PrivilegeViolationException, InvalidPartyException, InvalidStateException, InterruptedException, UnknownHostException, SocketException, CiscoRegistrationException {

        // Retrieve environment variables from .env, if present
        Dotenv dotenv=Dotenv.load();

        // Determine this PC's address and get an ephemeral port number
        // for registering RTP media for the CTI Port
        InetAddress ctipRtpAddress = InetAddress.getLocalHost();
        DatagramSocket sock1 = new DatagramSocket();
        int ctipRtpPort = sock1.getLocalPort();
        sock1.close();

        // Initialize the gstreamer-java framework Gst object
        Utils.configurePaths();
        Gst.init();

        // The Handler class provides observers for provider/address/terminal/call events
        CtiPortHandler handler = new CtiPortHandler();

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

        // Open the CTI_PORT_DN Address and wait for it to go in service
        log("Opening fromAddress DN: " + dotenv.get("CTI_PORT_DN"));
        CiscoAddress ctipAddress = (CiscoAddress) provider.getAddress(dotenv.get("CTI_PORT_DN"));
        ctipAddress.addObserver(handler);

        // Add and register the CTI Port media terminal
        CiscoMediaTerminal ctipTerminal = (CiscoMediaTerminal) ctipAddress.getTerminals()[0];
        ctipTerminal.addObserver(handler);
        ctipTerminal.register(ctipRtpAddress, ctipRtpPort,
                new CiscoMediaCapability[] { CiscoMediaCapability.G711_64K_30_MILLISECONDS });
        log("Awaiting CiscoTermInServiceEv for: " + ctipTerminal.getName() + "...");
        handler.ctipTerminalInService.waitTrue();
        log("Awaiting CiscoAddrInServiceEv for: " + ctipAddress.getName() + "...");
        handler.ctipAddressInService.waitTrue();
        // Enable auto accept for incoming offering calls
        ctipAddress.setAutoAcceptStatus(CiscoAddress.AUTOACCEPT_ON, ctipTerminal);
        // Add a call observer to receive call events
        ctipAddress.addCallObserver(handler);

        // Wait for an inbound call on the CTI Port
        log("Ready for dialin call at CTI Port DN: " + ctipAddress.getName());
        log("Awaiting dialin CallCtlTermConnRingingEv for: " + ctipTerminal.getName() + "...");
        handler.ctipCallRinging.waitTrue();
        
        // Via the newly populated ctipDialinCallEvent (see handler), drill/cast down to
        // a CallControlTerminalConnection so we can do some operations
        CallControlTerminalConnection ctipIncomingCctConnection = (CallControlTerminalConnection) ctipDialinCallEvent
                .getTerminalConnection();

        // Answer the dialin call on the CTI Port
        log("Answering dialin call from DN: " + ctipDialinCallEvent.getCallingAddress().getName());
        ctipIncomingCctConnection.answer();
        // Wait for the RTP output started event to indicate we can begin streaming audio
        log("Awaiting CiscoRTPOutputStartedEv for: " + ctipIncomingCctConnection.getTerminal().getName());
        handler.ctipRTPOutputStarted.waitTrue();

        // Extract the caller's IP/port for sending RTP audio media
        String destHost = ctipRTPOutputStartedEvent.getRTPOutputProperties().getRemoteAddress().getHostAddress();
        Integer destPort = ctipRTPOutputStartedEvent.getRTPOutputProperties().getRemotePort();
        // Create the GStreamer pipeline string to send audio from a file to the caller's phone; 30ms RTP packet size (in nanoseconds!)
        String pipelineDescription = String.format("filesrc location=media/g711.wav ! wavparse ! mulawenc ! rtppcmupay max-ptime=30000000 ! udpsink host=%s port=%s", destHost, destPort);
        // Instantiate the GStreamer pipline from the string
        pipeline = (Pipeline) Gst.parseLaunch(pipelineDescription);
        
        // Add a GStreamer message bus event listener, triggered when the file is finished playing
        bus = (Bus) pipeline.getBus();
        bus.connect(new Bus.EOS() {
            public void endOfStream(GstObject source) {
                Gst.quit();
            }
        });
        // Start the RTP stream
        pipeline.play();
        // Keep this thread alive until Gst.quit() is called in the EOS event handler
        Gst.main();

        log("Dropping call: " + ctipDialinCallEvent.getCall().toString());
        ((CallControlCall) ctipDialinCallEvent.getCall()).drop();

        System.exit(0);
    }
}
