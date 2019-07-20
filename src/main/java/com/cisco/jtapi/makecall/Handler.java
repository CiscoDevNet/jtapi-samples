package com.cisco.jtapi.makecall;

import javax.telephony.*;
import javax.telephony.events.*;
import javax.telephony.callcontrol.*;
import com.cisco.jtapi.extensions.*;
import com.cisco.cti.util.Condition;

public class Handler implements

    ProviderObserver, 
    TerminalObserver, 
    AddressObserver, 
    CallControlCallObserver {

	public Condition providerInService = new Condition();
	public Condition terminalInService = new Condition();
	public Condition addressInService = new Condition();
	public Condition callActive = new Condition();

	public void providerChangedEvent( ProvEv[] events ) {

		if ( events != null ) {

			for ( int i = 0; i < events.length; i++ ) {

                System.out.println( "-->Received " + events[ i ] + " for: " + events[ i ].getProvider().getName() );

                switch ( events[ i ].getID() ) {

                    case ProvInServiceEv.ID:

                        providerInService.set();

                        break;
				}
			}
		}
	}

    public void terminalChangedEvent( TermEv[] events) {

		for ( int i = 0; i < events.length; i++ ) {

            System.out.println( "-->Received " + events[ i ] + " for: " + events[ i ].getTerminal() );
            
			switch ( events[ i ].getID() ) {

                case CiscoTermInServiceEv.ID:
                
                    terminalInService.set();
                    
                    break;
            }
		}
	}

    public void addressChangedEvent( AddrEv[] events ) {

		for ( int i = 0; i < events.length; i++ ) {

            System.out.println( "-->Received " + events[ i ] + " for: " + events[ i ].getAddress().getName() );
            
			switch ( events[ i ].getID() ) {

                case CiscoAddrInServiceEv.ID:
                
                    addressInService.set();
                    
                    break;
            }    
		}        
    }

    public void callChangedEvent( CallEv[] events ) {

		for ( int i = 0; i < events.length; i++ ) {

            System.out.println( "-->Received " + events[ i ] + " for: " + events[ i ].getCall() );
            
			switch ( events[ i ].getID() ) {

                case CallActiveEv.ID:
                
                    callActive.set();
                    
                    break;
            }    
		}        

    }
}