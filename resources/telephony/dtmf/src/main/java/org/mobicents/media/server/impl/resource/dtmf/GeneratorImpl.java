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

package org.mobicents.media.server.impl.resource.dtmf;

import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.mobicents.media.server.spi.resource.DtmfGenerator;

/**
 * InbandGenerator generates Inband DTMF Tone only for uncompressed LINEAR
 * codec. After creating instance of InbandGenerator, it needs to be initialized
 * so that all the Tones are generated and kept ready for transmission once
 * start is called.
 * 
 * By default the Tone duration is 80ms. This is suited for Tone Detector who
 * has Tone duration of greater than 40 and less than 80ms. For Tone Detector
 * who's Tone duration is set greater than 80ms may fail to detect Tone
 * generated by InbandGenerator(with duration 80ms). In that case increase the
 * duration here too.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public class GeneratorImpl extends AbstractSource implements DtmfGenerator {

    private final static AudioFormat linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * linear.getSampleRate()/1000 * linear.getSampleSize() / 8;    

    private final static Formats formats = new Formats();
    static {
        formats.add(linear);
    }
    
    public final static String[][] events = new String[][]{
        {"1", "2", "3", "A"},
        {"4", "5", "6", "B"},
        {"7", "8", "9", "C"},
        {"*", "0", "#", "D"}
    };
    private int[] lowFreq = new int[]{697, 770, 852, 941};
    private int[] highFreq = new int[]{1209, 1336, 1477, 1633};
    private String digit = null;    // Min duration = 40ms and max = 500ms
    private int duration = 50;
    private short A = Short.MAX_VALUE / 2;
    private int volume = 0;
    private int f1,  f2;
    private double dt;
    private int pSize;
    private double time = 0;

    private AudioInput input;
    
    public GeneratorImpl(String name, Scheduler scheduler) {
        super(name, scheduler,scheduler.INPUT_QUEUE);
        dt = 1.0 / linear.getSampleRate();
        
        this.input=new AudioInput(ComponentType.DTMF_GENERATOR.getType(),packetSize);
        this.connect(this.input);   
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    @Override
    public void activate() {
        if (digit == null) {
            return;
        }
        
        time = 0;
        start();
    }
    
    public void setDigit(String digit) {
        this.digit = digit;
        this.time=0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (events[i][j].equalsIgnoreCase(digit)) {
                    f1 = lowFreq[i];
                    f2 = highFreq[j];
                }
            }
        }
    }

    public String getDigit() {
        return this.digit;
    }

    public void setToneDuration(int duration) {
        if (duration < 40) {
            throw new IllegalArgumentException("Duration cannot be less than 40ms");
        }
        this.duration = duration;
    }

    public int getToneDuration() {
        return duration;
    }

    public int getVolume() {
        return this.volume;
    }

    public void setVolume(int volume) {
        if (volume > 0) {
            throw new IllegalArgumentException("Volume has to be negative value expressed in dBm0");
        }
        this.volume = volume;
        A = (short) (Math.pow(Math.pow(10, volume), 0.1) * (Short.MAX_VALUE / 2));
    }

    private short getValue(double t) {
        return (short) (A * (Math.sin(2 * Math.PI * f1 * t) + Math.sin(2 * Math.PI * f2 * t)));
    }

    public Formats getNativeFormats() {
        return formats;
    }

    @Override
    public Frame evolve(long timestamp) {
    	if(time > (double) duration / 1000.0)
    		return null;
    	
    	int k = 0;
        int frameSize = (int) ((double) 20 / 1000.0 / dt);
        Frame frame = Memory.allocate(2* frameSize);
        byte[] data = frame.getData();
        for (int i = 0; i < frameSize; i++) {
            short v = getValue(time + dt * i);
            data[k++] = (byte) v;
            data[k++] = (byte) (v >> 8);
        }
        frame.setOffset(0);
        frame.setLength(2* frameSize);
        frame.setTimestamp(getMediaTime());
        frame.setDuration(20000000L);

        if (time == 0) {
            //TODO add key frame
//            buffer.setFlags(Buffer.FLAG_KEY_FRAME);
        }
        
        time += ((double) 20) / 1000.0;
        return frame;
    }

    @Override
    public void deactivate() {
        stop();
    }    
}

