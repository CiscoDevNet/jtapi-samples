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

import javax.telephony.*;
import javax.telephony.events.*;
import javax.telephony.callcontrol.*;
import com.cisco.jtapi.extensions.*;
import com.cisco.cti.util.Condition;

public class Handler implements

        ProviderObserver, TerminalObserver, AddressObserver, CallControlCallObserver {

    public Condition providerInService = new Condition();
    public Condition fromTerminalInService = new Condition();
    public Condition fromAddressInService = new Condition();
    public Condition callActive = new Condition();

    public void providerChangedEvent(ProvEv[] events) {
        for (ProvEv ev : events) {
            System.out.println("    Received--> Provider/" + ev);
            switch (ev.getID()) {
                case ProvInServiceEv.ID:
                    providerInService.set();
                    break;
            }
        }
    }

    public void terminalChangedEvent(TermEv[] events) {
        for (TermEv ev : events) {
            System.out.println("    Received--> Terminal/"+ev);
            switch (ev.getID()) {
                case CiscoTermInServiceEv.ID:
                    fromTerminalInService.set();
                    break;
            }
        }
    }

    public void addressChangedEvent(AddrEv[] events) {
        for (AddrEv ev : events) {
            System.out.println("    Received--> Address/"+ev);
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID:
                    fromAddressInService.set();
                    break;
            }
        }
    }

    public void callChangedEvent(CallEv[] events) {
        for (CallEv ev : events) {
            System.out.println("    Received--> Call/"+ev);
            switch (ev.getID()) {
                case CallActiveEv.ID:
                    callActive.set();
                    break;
            }
        }

    }
}