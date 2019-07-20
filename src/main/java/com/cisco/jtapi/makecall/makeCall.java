package com.cisco.jtapi.makecall;

import javax.telephony.*;
import com.cisco.jtapi.extensions.*;

public class makeCall {

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
        CiscoAddress fromAddress = (CiscoAddress) provider.getAddress( System.getenv( "CALL_FROM" ) );
        
        fromAddress.addObserver( handler );

        // Wait for CiscoAddrInServiceEv
        System.out.println( "Awaiting CiscoAddrInServiceEv for: " + fromAddress.getName() + "..." );

        handler.addressInService.waitTrue();

        // We'll again use Handler to observe call events for the Address
        fromAddress.addCallObserver( handler );

        // Retrieve the first Terminal (phone) object for the Address
        // Could be multiple if it's a shared line
        CiscoTerminal fromTerminal = (CiscoTerminal) fromAddress.getTerminals()[ 0 ];

        fromTerminal.addObserver( handler );

        // Wait for CiscoTermInServiceEv
        System.out.println( "Awaiting CiscoTermInServiceEv for: " + fromTerminal.getName() + "..." );

        handler.terminalInService.waitTrue();

        // Create a new Call object from our provider
        CiscoCall call = (CiscoCall) provider.createCall ();

        // Place the call, specifying our phone, line and destination DN
        System.out.println( "Awaiting CallActiveEv for: " + call.toString() + "..." );

        call.connect( fromTerminal, fromAddress, System.getenv( "CALL_TO" ) );

        // Wait for CallActiveEv
        handler.callActive.waitTrue();

        // Wait 5 sec, then drop the call
        System.out.println( "Sleeping 5 seconds before dropping call: " + call.toString() + "..." );

        Thread.sleep( 5000 );

        System.out.println( "Dropping call: " + call.toString() + "..." );

        call.drop();

        System.exit( 0 );
	}
}
