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

package org.restcomm.media.rtp;

import java.net.SocketAddress;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface RtpSession {
    
    long getSsrc();
    
    MediaType getMediaType();
    
    SocketAddress getRtpAddress();
    
    RTPFormats getSupportedFormats();
    
    ConnectionMode getMode();
    
    void open(SocketAddress address, FutureCallback<Void> callback);
    
    void negotiate(MediaDescriptionField sdp, FutureCallback<Void> callback);

    void close(FutureCallback<Void> callback);

    void updateMode(ConnectionMode mode, FutureCallback<Void> callback);

    void incomingRtp(RtpPacket packet);

    void outgoingRtp(RtpPacket packet);
    
    boolean isActive();
    
    /**
     * Gets the in-band audio component of the connection.
     * 
     * @return The in-band media component
     */
    AudioComponent getAudioComponent();

    /**
     * Gets the out-of-band audio component of the connection.
     * 
     * @return The out-of-band media component
     */
    OOBComponent getOOBComponent();

}