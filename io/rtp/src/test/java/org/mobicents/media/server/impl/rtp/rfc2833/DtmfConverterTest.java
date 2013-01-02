/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.rtp.rfc2833;

import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.JitterBuffer;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;

/**
 *
 * @author kulikov
 */
public class DtmfConverterTest {
    
	Clock clock = new DefaultClock();
	private RtpClock rtpClock = new RtpClock(clock);

    private int period = 20;
    private int jitter = 40;

    private DtmfConverter dtmfConverter;
    private Scheduler scheduler;
    
    private RtpPacket[] packets = new RtpPacket[] {
        new RtpPacket(16, false), new RtpPacket(16, false), new RtpPacket(16, false)
    };
    
    private byte[] event1 = new byte[] {0x03, 0x0a, 0x00, (byte)0xa0};
    private byte[] event2 = new byte[] {0x03, 0x0a, 0x01, (byte)0x40};
    private byte[] event3 = new byte[] {0x03, (byte)0x8a, 0x01, (byte)0xe0};
    
    private final static double dt = 1.0 / 8000;    
    private byte[] tone = new byte[320 * 3];
    
    private final static short A = Short.MAX_VALUE / 2;
    private final int f1 = 697;
    private final int f2 = 1477;
    
    public DtmfConverterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        rtpClock.setClockRate(8000);
        
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        dtmfConverter = new DtmfConverter(scheduler, rtpClock);
        
        packets[0].wrap(true, 101, 1, 160, 1, event1, 0, 4);
        packets[1].wrap(false, 101, 2, 160, 1, event2, 0, 4);
        packets[2].wrap(false, 101, 3, 160, 1, event3, 0, 4);        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of process method, of class DtmfConverter.
     */
//    @Test
    public void testProcess() throws Exception {
    	dtmfConverter.activate();
    	
    	long start = System.nanoTime();
    	dtmfConverter.reset();
    	dtmfConverter.write(packets[0]);
    	dtmfConverter.write(packets[1]);
    	dtmfConverter.write(packets[2]);
        
    	Thread.sleep(40);
        Frame frame0 = dtmfConverter.getAudioInput().poll();
        Thread.sleep(20);
        Frame frame1 = dtmfConverter.getAudioInput().poll(); 
        Thread.sleep(20);
        Frame frame2 = dtmfConverter.getAudioInput().poll();
        Thread.sleep(40);
        
        //lets read 2 extra packets generated , should have 5 frames
        dtmfConverter.getAudioInput().poll();
        dtmfConverter.getAudioInput().poll();
        dtmfConverter.deactivate();        
        
        long finish = System.nanoTime();                        
        		
        for (int i = 0; i < 320; i++) {
        	assertEquals("Pos " + i, DtmfTonesData.buffer[3][i], frame0.getData()[i]);
        }
        
        for (int i = 0; i < 320; i++) {
            assertEquals("Pos " + i, DtmfTonesData.buffer[3][i + 320], frame1.getData()[i]);
        }
        
        for (int i = 0; i < 320; i++) {
            assertEquals("Pos " + i, DtmfTonesData.buffer[3][i + 640], frame2.getData()[i]);
        }
        
        System.out.println("Conversation duration=" + (finish - start));
    }
    
    @Test
    public void testRepeat() {
    	try
    	{
        for (int i = 0; i < 10; i++) {
            System.out.println("Test #" + i);         
            this.testProcess();            
        }
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
}
