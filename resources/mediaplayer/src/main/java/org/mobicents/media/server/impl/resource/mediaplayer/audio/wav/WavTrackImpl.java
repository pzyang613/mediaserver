/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.mobicents.media.server.impl.resource.mediaplayer.Track;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class WavTrackImpl implements Track {

    /** audio stream */
    private transient AudioInputStream stream = null;
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
//    private long timestamp;
    private long duration;
    
    private boolean first = true;
    private SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss,SSS");

    public WavTrackImpl(URL url) throws UnsupportedAudioFileException, IOException {
        stream = AudioSystem.getAudioInputStream(url);
        
        //measure in nanoseconds
        duration = (long)(stream.getFrameLength()/stream.getFormat().getFrameRate() * 1000L) * 1000000L;
        
        format = getFormat(stream);
        if (format == null) {
            throw new UnsupportedAudioFileException();
        }

        frameSize = (int) (period * format.getChannels() * format.getSampleSize() *
                format.getSampleRate() / 8000);
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() *
                format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return 0;// timestamp * 1000000L;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setMediaTime(long timestamp) {
//        this.timestamp = timestamp/1000000L;
//        try {
//            long offset = frameSize * (timestamp / period);
//            byte[] skip = new byte[(int)offset];
//            stream.read(skip);
//        } catch (IOException e) {
//        }
    }
    
    private void skip(long timestamp) {
        try {
            long offset = frameSize * (timestamp / period/ 1000000L);
            byte[] skip = new byte[(int)offset];
            stream.read(skip);
        } catch (IOException e) {
        }
    }
    
    private AudioFormat getFormat(AudioInputStream stream) {
        Encoding encoding = stream.getFormat().getEncoding();
        if (encoding == Encoding.ALAW) {
            return FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        } else if (encoding == Encoding.ULAW) {
            return FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
        } else if (encoding == Encoding.PCM_SIGNED) {
            int sampleSize = stream.getFormat().getSampleSizeInBits();
            int sampleRate = (int) stream.getFormat().getSampleRate();
            int channels = stream.getFormat().getChannels();
            return FormatFactory.createAudioFormat("linear", sampleRate, sampleSize, channels);
        }
        return null;
    }

    /**
     * Reads packet from currently opened stream.
     * 
     * @param packet
     *            the packet to read
     * @param offset
     *            the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws java.io.IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        int length = 0;
        try {
            while (length < psize) {
                int len = stream.read(packet, offset + length, psize - length);
                if (len == -1) {                	
                    return length;
                }
                length += len;
            }
            return length;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = 0;
        }
    }
    
    public Frame process(long timestamp) throws IOException {
        if (first) {
            if (timestamp > 0) {
                skip(timestamp);
            }
            first = false;
        }
        
        Frame frame = Memory.allocate(frameSize);
        byte[] data =frame.getData();
        if (data == null) {
            data = new byte[frameSize];
        }
        
        int len = readPacket(data, 0, frameSize);
        if (len == 0) {
            eom = true;
        }

        if (len < frameSize) {
            padding(data, frameSize - len);
            eom = true;
        }

        frame.setOffset(0);
        frame.setLength(frameSize);
        frame.setEOM(eom);
        frame.setDuration(period* 1000000L);
        frame.setFormat(format);
        
        return frame;
    }

    public void close() {
        try {
            stream.close();
        } catch (Exception e) {
        }
    }

    public Format getFormat() {
        return format;
    }
}
