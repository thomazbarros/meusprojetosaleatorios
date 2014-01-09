package net.java.dev.netbeansspot;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;

import java.io.*;
import java.util.Hashtable;
import javax.microedition.io.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * A simple MIDlet that uses the left switch (SW1) to broadcast a message
 * to set the color of the LEDs of any receiving SPOTs and the right
 * switch (SW2) to count in binary in its LEDs.
 *
 * Messages received from the other SPOTs control the LEDs of this SPOT.
 */
public class Application extends MIDlet implements ISwitchListener {

    private static final int CHANGE_COLOR = 1;
    private static final int CHANGE_COUNT = 2;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
    private int count = -1;
    private int color = 0;
    private LEDColor[] colors = {LEDColor.RED, LEDColor.GREEN, LEDColor.BLUE};
    private RadiogramConnection tx = null, rx = null;
    private Radiogram xdg, rdg;
    private Hashtable um = null, dois = null, tres = null, quatro = null;
    private String myaddress, sink, destination;
    //private IRadioPolicyManager rpm = (IRadioPolicyManager)Resources.lookup(IRadioPolicyManager.class);
    //private IEEEAddress myAddr = new IEEEAddress(rpm.getIEEEAddress());

    private void showCount(int count, int color) {
        for (int i = 7, bit = 1; i >= 0; i--, bit <<= 1) {
            if ((count & bit) != 0) {
                leds.getLED(i).setColor(colors[color]);
                leds.getLED(i).setOn();
            } else {
                leds.getLED(i).setOff();
            }
        }
    }

    private void showColor(int color) {
        leds.setColor(colors[color]);
        leds.setOn();
    }

    protected void startApp() throws MIDletStateChangeException {

      myaddress = IEEEAddress.toDottedHex(RadioFactory.getRadioPolicyManager().getIEEEAddress());
      System.out.println("*******AQUI********:"+myaddress);

        um = new Hashtable();
        um.put("7f00.0101.0000.1002", "7f00.0101.0000.1002");
        um.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");
        um.put("7f00.0101.0000.1004", "7f00.0101.0000.1003");

        dois = new Hashtable();
        dois.put("7f00.0101.0000.1001", "7f00.0101.0000.1001");
        dois.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");
        dois.put("7f00.0101.0000.1004", "7f00.0101.0000.1003");

        tres = new Hashtable();
        tres.put("7f00.0101.0000.1001", "7f00.0101.0000.1001");
        tres.put("7f00.0101.0000.1002", "7f00.0101.0000.1002");
        tres.put("7f00.0101.0000.1004", "7f00.0101.0000.1004");

        quatro = new Hashtable();
        quatro.put("7f00.0101.0000.1001", "7f00.0101.0000.1003");
        quatro.put("7f00.0101.0000.1002", "7f00.0101.0000.1003");
        quatro.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");

        System.out.println("Broadcast Counter MIDlet");
        showColor(color);
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);

        Receive receive = new Receive();
        Thread t1 = new Thread(receive);
        t1.start();

    }

    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        // Only called if startApp throws any exception other than MIDletStateChangeException
    }

    public void switchReleased(SwitchEvent evt) {
        int cmd;
        if (evt.getSwitch() == sw1) {
            cmd = CHANGE_COLOR;
            if (++color >= colors.length) {
                color = 0;
            }
            count = -1;
        } else {
            cmd = CHANGE_COUNT;
            count++;
        }
        try {
            xdg.reset();
            xdg.writeInt(cmd);
            xdg.writeInt(count);
            xdg.writeInt(color);
          //  xdg.writeUTF(myaddress);
            tx.send(xdg);
        } catch (IOException ex) {
            System.out.println("Error sending packet: " + ex);
            ex.printStackTrace();
        }
    }

    public void switchPressed(SwitchEvent evt) {
    }

    public class Send implements Runnable {

        public void run() {
        }
    }

    public class Receive implements Runnable {

        public void run() {

            try {
                tx = (RadiogramConnection) Connector.open("radiogram://broadcast:123");
                xdg = (Radiogram) tx.newDatagram(20);
                rx = (RadiogramConnection) Connector.open("radiogram://:123");
                rdg = (Radiogram) rx.newDatagram(20);
                while (true) {
                    try {
                        rx.receive(rdg);
                        int cmd = rdg.readInt();
                        int newCount = rdg.readInt();
                        int newColor = rdg.readInt();

                        //destination = rdg.readUTF();
                        if (cmd == CHANGE_COLOR) {
                            System.out.println("Received packet from " + rdg.getAddress());
                           // System.out.println("MYADDRESS: "+rdg.readUTF());
                            showColor(newColor);
                        } else {
                            showCount(newCount, newColor);
                        }
                    } catch (IOException ex) {
                        System.out.println("Error receiving packet: " + ex);
                        ex.printStackTrace();
                    }
                }
            } catch (IOException ex) {
                System.out.println("Error opening connections: " + ex);
                ex.printStackTrace();
            }

        }
    }
}
