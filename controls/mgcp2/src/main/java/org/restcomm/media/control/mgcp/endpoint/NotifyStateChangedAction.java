/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.endpoint;

import java.util.Set;

import org.apache.log4j.Logger;
import org.squirrelframework.foundation.fsm.AnonymousAction;

/**
 * Registers a connection in the endpoint. The connection becomes bound to the endpoint's media relay.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifyStateChangedAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(NotifyStateChangedAction.class);

    static final NotifyStateChangedAction INSTANCE = new NotifyStateChangedAction();

    NotifyStateChangedAction() {
        super();
    }

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event,
            MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        // Do not notify when passing to initial state
        if (from == null) {
            return;
        }

        // Gather required data from context
        MgcpEndpointContext globalContext = stateMachine.getContext();
        MgcpEndpoint endpoint = stateMachine.getEndpoint();
        Set<MgcpEndpointObserver> observers = globalContext.getEndpointObservers();

        if (log.isTraceEnabled()) {
            EndpointIdentifier endpointId = globalContext.getEndpointId();
            log.trace("Endpoint " + endpointId + "is notifying observers that state changed to " + to.name());
        }

        // Notify observers (if any)
        for (MgcpEndpointObserver observer : observers) {
            observer.onEndpointStateChanged(endpoint, to);
        }
    }

}