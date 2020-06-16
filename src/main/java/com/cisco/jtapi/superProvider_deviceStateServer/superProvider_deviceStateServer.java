package com.cisco.jtapi.superProvider_deviceStateServer;

// Copyright (c) 2019 Cisco and/or its affiliates.
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

// Demonstrates using CiscoProvider.createTerminal() to dynamically create a 
// terminal by device name using the Cisco JTAPI 'Superprovider' feature.  Then
// retrieves and monitors the device for device-side status changes using the 
// 'Device State Server' feature.

// Be sure to edit .vscode/launch.json and enter your CUCM/user/device details
// into the appropriate environment variable items

// Note, the JTAPI user must be configured with the additional permission group:

//      Standard CTI Allow Control of All Devices

// Tested using:

// Ubuntu Linux 19.04
// Java 1.8u201
// CUCM 11.5

import javax.telephony.*;
import java.util.*;
import com.cisco.jtapi.extensions.*;

public class superProvider_deviceStateServer {

  public static void main ( String [] args ) throws
  
    JtapiPeerUnavailableException, 
    ResourceUnavailableException, 
    MethodNotSupportedException, 
    InvalidArgumentException, 
    PrivilegeViolationException, 
    InvalidPartyException, 
    InvalidStateException, 
    InterruptedException {

        // Our Handler class provides observers for provider/address/terminal/call events
        Handler handler = new Handler();

        // Create the JtapiPeer object, representing the JTAPI library
        System.out.println("Initializing Jtapi");
        CiscoJtapiPeer peer = (CiscoJtapiPeer) JtapiPeerFactory.getJtapiPeer( null );
        
        // Create and open the Provider, representing a JTAPI connection to CUCM CTI Manager
        String providerString = String.format( "%s;login=%s;passwd=%s", 
            System.getenv( "CUCM" ), System.getenv( "USER_NAME" ), System.getenv( "PASSWORD" ) );
        
        System.out.println( "Connecting Provider: " + providerString );

        CiscoProvider provider = (CiscoProvider) peer.getProvider( providerString );

        provider.addObserver( handler );
    
        System.out.println( "Awaiting ProvInServiceEv..." );

        // Wait for ProvInServiceEv
        handler.providerInService.waitTrue();

        // Dynamically create a terminal by device name via 'superprovider' feature
        CiscoTerminal terminal = (CiscoTerminal) provider.createTerminal( System.getenv( "MONITOR_DEVICE_NAME" ) );

        terminal.addObserver( handler );

        System.out.println( "Awaiting CiscoTermInServiceEv for: " + terminal.getName() + "...");

        // Wait for CiscoTermInServiceEv
        handler.terminalInService.waitTrue();

        // Create a hash map to get friendly names for the device state
        Map<Integer, String> stateName = new HashMap<Integer, String>();
        stateName.put( CiscoTerminal.DEVICESTATE_IDLE, "IDLE" );
        stateName.put( CiscoTerminal.DEVICESTATE_ACTIVE, "ACTIVE" );
        stateName.put( CiscoTerminal.DEVICESTATE_ALERTING, "ALERTING" );
        stateName.put( CiscoTerminal.DEVICESTATE_HELD, "HELD" );
        stateName.put( CiscoTerminal.DEVICESTATE_UNKNOWN, "UNKNOWN" );
        stateName.put( CiscoTerminal.DEVICESTATE_WHISPER, "WHISPER" );

        // Check the current device state
        int state = terminal.getDeviceState();

        System.out.println( "\nInitial device state: " + stateName.get( state ) );

        // Enable filters to receive various device state events
        CiscoTermEvFilter termFilter = terminal.getFilter();

        termFilter.setDeviceStateActiveEvFilter( true );
        termFilter.setDeviceStateAlertingEvFilter( true );
        termFilter.setDeviceStateHeldEvFilter( true );
        termFilter.setDeviceStateIdleEvFilter( true );

        terminal.setFilter( termFilter );

        // The handler thread will run idefinitely, printing any received events
        System.out.println( "Monitoring device for state changes..." );
	}
}
