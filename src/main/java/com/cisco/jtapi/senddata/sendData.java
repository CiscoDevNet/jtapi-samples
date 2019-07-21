package com.cisco.jtapi.senddata;

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

// Opens a phone and performs CiscoTerminal.sendData() method to send a
// 'Hello World' message to the phone's display.

// Be sure to edit .vscode/launch.json and enter your CUCM/user/line details
// into the environment variable items

// Tested using:

// Ubuntu Linux 19.04
// Java 1.8u201
// CUCM 11.5

import javax.telephony.*;
import com.cisco.jtapi.extensions.*;

public class sendData {

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
        JtapiPeer peer = JtapiPeerFactory.getJtapiPeer( null );
        
        // Create and open the Provider, representing a JTAPI connection to CUCM CTI Manager
        String providerString = String.format( "%s;login=%s;passwd=%s", 
            System.getenv( "CUCM" ), System.getenv( "USER_NAME" ), System.getenv( "PASSWORD" ) );
        
        System.out.println( "Connecting Provider: " + providerString );

        Provider provider = peer.getProvider( providerString );

        provider.addObserver( handler );
    
        // Wait for ProvInServiceEv
        System.out.println( "Awaiting ProvInServiceEv..." );

        handler.providerInService.waitTrue();
    
        // Retrieve and open the Address (line) object for the 'from' DN specified in the environment
        CiscoAddress address = (CiscoAddress) provider.getAddress( System.getenv( "LINE" ) );
        
        address.addObserver(handler);

        // Wait for CiscoAddrInServiceEv
        System.out.println( "Awaiting CiscoAddrInServiceEv for: " + address.getName() + "...");

        handler.addressInService.waitTrue();

        // We'll again use Handler to observe call events for the Address
        address.addCallObserver(handler);

        // Retrieve the first Terminal (phone) object for the Address.
        // Could be multiple if it's a shared line
        CiscoTerminal terminal = (CiscoTerminal) address.getTerminals()[0];

        terminal.addObserver(handler);

        // Wait for CiscoTermInServiceEv
        System.out.println( "Awaiting CiscoTermInServiceEv for: " + terminal.getName() + "...");

        handler.terminalInService.waitTrue();

        // Send an IP Phone Services API text object to the phone's display
        System.out.println( "Sending <CiscoIPPhoneText> object to: "  + terminal.getName() );
        
        terminal.sendData( "<CiscoIPPhoneText><Text>Hello World</Text></CiscoIPPhoneText>" );

        // Wait 5 seconds, then clear the phone display
        System.out.println(( "Sleeping 5 seconds..." ) );

        Thread.sleep( 5000 );

        System.out.println( "Sending <CiscoIPPhoneExecute> object to: " + terminal.getName() );

        terminal.sendData( "<CiscoIPPhoneExecute><ExecuteItem URL='Init:Services' /></CiscoIPPhoneExecute>" );

        // End the program
        System.exit( 0 );
	}
}
