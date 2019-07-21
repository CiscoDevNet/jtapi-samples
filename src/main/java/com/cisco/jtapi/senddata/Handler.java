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