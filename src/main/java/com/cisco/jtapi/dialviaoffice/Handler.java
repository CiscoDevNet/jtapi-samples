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

import javax.telephony.*;
import javax.telephony.events.*;
import javax.telephony.callcontrol.*;
import javax.telephony.callcontrol.events.CallCtlConnEstablishedEv;
import javax.telephony.callcontrol.events.CallCtlTermConnHeldEv;
import javax.telephony.callcontrol.events.CallCtlTermConnRingingEv;
import javax.telephony.callcontrol.events.CallCtlTermConnTalkingEv;

import com.cisco.jtapi.extensions.*;
import com.cisco.jtapi.extensions.CiscoMediaOpenLogicalChannelEv;

import com.cisco.cti.util.Condition;

public class Handler implements ProviderObserver, TerminalObserver, AddressObserver, CallControlCallObserver {

    public Condition providerInService = new Condition();
    public Condition rpTerminalInService = new Condition();
    public Condition rpAddressInService = new Condition();
    public Condition rpDialinCallRinging;
    public Condition dialoutCallEstablished;
    public Condition rpDialinCallTalking;
    public Condition rpDialinCallHeld;
    public Condition rpOpenLogicalChannel;

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
            System.out.println("    Received--> Terminal/" + ev);
            switch (ev.getID()) {
                case CiscoTermInServiceEv.ID:
                    rpTerminalInService.set();
                    break;
                case CiscoMediaOpenLogicalChannelEv.ID:
                    dialViaOffice.rpOpenLogicalChannelEvent = (CiscoMediaOpenLogicalChannelEv) ev;
                    rpOpenLogicalChannel.set();
                    break;
            }
        }
    }

    public void addressChangedEvent(AddrEv[] events) {
        for (AddrEv ev : events) {
            System.out.println("    Received--> Address/" + ev);
            switch (ev.getID()) {
                case CiscoAddrInServiceEv.ID:
                    rpAddressInService.set();
                    break;
            }
        }
    }

    public void callChangedEvent(CallEv[] events) {
        for (CallEv ev : events) {
            System.out.println("    Received--> Call/" + ev);
            switch (ev.getID()) {
                case CallCtlTermConnRingingEv.ID:
                    if (((CallCtlTermConnRingingEv) ev).getCalledAddress().equals(dialViaOffice.rpAddress)) {
                        dialViaOffice.rpDialinCallEvent = (CallCtlTermConnRingingEv) ev;
                        rpDialinCallRinging.set();
                    }
                    break;
                case CallCtlConnEstablishedEv.ID:
                    if (((CallCtlConnEstablishedEv) ev).getConnection().getAddress()
                            .equals(dialViaOffice.dialoutAddress)) {
                        dialoutCallEstablished.set();
                    }
                    break;
                case CallCtlTermConnTalkingEv.ID:
                    if (((CallCtlTermConnTalkingEv) ev).getTerminalConnection().getConnection().getAddress()
                            .equals(dialViaOffice.rpAddress)) {
                        rpDialinCallTalking.set();
                    }
                    break;
                case CallCtlTermConnHeldEv.ID:
                    rpDialinCallHeld.set();
                    break;
            }
        }
    }

}
