package net.java.dev.netbeansspot;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.io.j2me.radiogram.*;
//import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;

import java.io.*;
import java.util.Hashtable;
import java.util.Vector;
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
    private static final int PEACH = 3;
    private static final int DELAY = 100;
    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch) Resources.lookup(ISwitch.class, "SW2");
    private int count = -1;
    private int color = 0;
    private LEDColor[] colors = {LEDColor.RED, LEDColor.GREEN, LEDColor.BLUE};
    private RadiogramConnection tx = null, tx2 = null, rx = null;
    private Radiogram xdg, xdg2, rdg;
    private Hashtable um = null, dois = null, tres = null, quatro = null;
    private Hashtable umCost = null, doisCost = null, tresCost = null, quatroCost = null;
    private String myaddress = null, mysink = null, mydestination = null, mynexthop = null, mypayload = null, toSend = null;
    private String rcvsink = null, rcvnexthop = null, rcvpayload = null;
    private String clusterHead = null;
    private Vector cluster = null;
    private long now, mydelay;
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
        System.out.println("*******AQUI********:" + myaddress);
        um = new Hashtable();
        um.put("7f00.0101.0000.1002", "7f00.0101.0000.1002");
        um.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");
        um.put("7f00.0101.0000.1004", "7f00.0101.0000.1003");

        umCost = new Hashtable();
        umCost.put("7f00.0101.0000.1001", new Integer(0));
        umCost.put("7f00.0101.0000.1002", new Integer(1));
        umCost.put("7f00.0101.0000.1003", new Integer(1));
        umCost.put("7f00.0101.0000.1004", new Integer(1000));


        dois = new Hashtable();
        dois.put("7f00.0101.0000.1001", "7f00.0101.0000.1001");
        dois.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");
        dois.put("7f00.0101.0000.1004", "7f00.0101.0000.1003");

        doisCost = new Hashtable();
        doisCost.put("7f00.0101.0000.1001", new Integer(1));
        doisCost.put("7f00.0101.0000.1002", new Integer(0));
        doisCost.put("7f00.0101.0000.1003", new Integer(1));
        doisCost.put("7f00.0101.0000.1004", new Integer(1000));


        tres = new Hashtable();
        tres.put("7f00.0101.0000.1001", "7f00.0101.0000.1001");
        tres.put("7f00.0101.0000.1002", "7f00.0101.0000.1002");
        tres.put("7f00.0101.0000.1004", "7f00.0101.0000.1004");

        tresCost = new Hashtable();
        tresCost.put("7f00.0101.0000.1001", new Integer(1));
        tresCost.put("7f00.0101.0000.1002", new Integer(1));
        tresCost.put("7f00.0101.0000.1003", new Integer(0));
        tresCost.put("7f00.0101.0000.1004", new Integer(1));



        quatro = new Hashtable();
        quatro.put("7f00.0101.0000.1001", "7f00.0101.0000.1003");
        quatro.put("7f00.0101.0000.1002", "7f00.0101.0000.1003");
        quatro.put("7f00.0101.0000.1003", "7f00.0101.0000.1003");

        quatroCost = new Hashtable();
        quatroCost.put("7f00.0101.0000.1001", new Integer(1000));
        quatroCost.put("7f00.0101.0000.1002", new Integer(1000));
        quatroCost.put("7f00.0101.0000.1003", new Integer(1));
        quatroCost.put("7f00.0101.0000.1004", new Integer(1000));

        System.out.println("Início!");
        showColor(color);
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);

        Receive receive = new Receive();
        Thread t1 = new Thread(receive);
        t1.start();


        if (myaddress.equals("7f00.0101.0000.1001")) {
            System.out.println("ENVIANDO PACOTE DE TESTE");
            mynexthop = "7f00.0101.0000.1003";
            mysink = "7f00.0101.0000.1004";
            mypayload = "DADO DO NODE UM!";
            clusterHead = mynexthop;
            cluster.addElement(myaddress);
            cluster.addElement(mynexthop);
            Send send = new Send();
            Thread t2 = new Thread(send);
            t2.start();
        }
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
            //xdg.writeUTF();
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
            try {
                xdg2.reset();
                xdg2.writeInt(PEACH);
                xdg2.writeInt(999);
                xdg2.writeInt(888);
                if (mydestination != null) {
                    xdg2.writeUTF(mydestination);
                } else {
                    xdg2.writeUTF("VAZIO");
                }
                if (mynexthop != null) {
                    xdg2.writeUTF(mynexthop);
                } else {
                    xdg2.writeUTF("VAZIO");
                }
                if (mysink != null) {
                    xdg2.writeUTF(mysink);
                } else {
                    xdg2.writeUTF("VAZIO");
                }

                tx2.send(xdg2);
            } catch (IOException ex) {
                System.out.println("Error sending packet: " + ex);
                ex.printStackTrace();
            }
        }
    }

    public class Receive implements Runnable {

        public void run() {

            try {

                System.out.println("RECEBENDO PACOTE!!!...");
                tx = (RadiogramConnection) Connector.open("radiogram://broadcast:123");
                xdg = (Radiogram) tx.newDatagram(20);
                tx2 = (RadiogramConnection) Connector.open("radiogram://broadcast:123");
                xdg2 = (Radiogram) tx2.newDatagram(20);
                rx = (RadiogramConnection) Connector.open("radiogram://:123");
                rdg = (Radiogram) rx.newDatagram(20);
                while (true) {
                    try {
                        rx.receive(rdg);
                        int cmd = rdg.readInt();
                        int newCount = rdg.readInt();
                        int newColor = rdg.readInt();
                        rcvnexthop = rdg.readUTF();
                        rcvsink = rdg.readUTF();
                        rcvpayload = rdg.readUTF();

                        if (cmd == CHANGE_COLOR) {
                            System.out.println("Received packet from " + rdg.getAddress());
                            // System.out.println("MYADDRESS: "+rdg.readUTF());
                            showColor(newColor);
                        } else if (cmd == PEACH) {
                            System.out.println("CONSUMINDO PACOTE!");
                            System.out.println("Sou: " + myaddress);
                            System.out.println("Destino final: " + rcvsink);
                            System.out.println("Origem: ");
                            System.out.println("newCount: " + newCount);
                            System.out.println("newColor: " + newColor);
                            System.out.println("Next Hop: " + rcvnexthop);
                            System.out.println("Sink: " + rcvsink);
                            System.out.println("Conteúdo: " + rcvpayload);
                            System.out.println("##### FIM #####");

                            location_unware();
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

    public void location_unware() {
        if (clusterHead == null) {
            clusterHead = rcvnexthop;
            mysink = rcvsink;
            cluster.addElement(rdg.getAddress());
            cluster.addElement(clusterHead);
        } else if (!rcvnexthop.equals(clusterHead)) {
            return;
        }


        if (clusterHead.equals(myaddress)) {
            mynexthop = rcvsink;
            formCluster(DELAY);
            if (mynexthop.equals(rcvsink)) {
                mynexthop = getRoute(mysink);
            }
            mysink = rcvsink;
            mydestination = mysink;
            Send send = new Send();
            Thread t3 = new Thread(send);
            t3.start();
            //enviar pacote
        } else {
            joinCluster();
            if (toSend != null) {
                mypayload = toSend;
                Send send2 = new Send();
                Thread t3 = new Thread(send2);
                t3.start();
            }

        }
    }

    public void formCluster(int delay) {

        now = System.currentTimeMillis();

        while ((System.currentTimeMillis() - now) <= delay) {

            if (clusterHead.equals(myaddress)) {
                toSend += rcvpayload;
            } else {
                if (isWorthIt(myaddress, clusterHead, rcvnexthop)) {
                    joinCluster();
                }
            }

            readAgain();


        }
        
    }

    public void joinCluster() {
        mynexthop = clusterHead;
    }

    public void readAgain() {
        try {
            rx.receive(rdg);
            int cmd = rdg.readInt();
            int newCount = rdg.readInt();
            int newColor = rdg.readInt();
            rcvnexthop = rdg.readUTF();
            rcvsink = rdg.readUTF();
            rcvpayload = rdg.readUTF();

            if (cmd == CHANGE_COLOR) {
                System.out.println("Received packet from " + rdg.getAddress());
                // System.out.println("MYADDRESS: "+rdg.readUTF());
                showColor(newColor);
            } else if (cmd == PEACH) {
                System.out.println("CONSUMINDO PACOTE!");
                System.out.println("Sou: " + myaddress);
                System.out.println("Destino final: " + rcvsink);
                System.out.println("Origem: ");
                System.out.println("newCount: " + newCount);
                System.out.println("newColor: " + newColor);
                System.out.println("Next Hop: " + rcvnexthop);
                System.out.println("Sink: " + rcvsink);
                System.out.println("Conteúdo: " + rcvpayload);
                System.out.println("##### FIM #####");

                location_unware();
            } else {
                showCount(newCount, newColor);

            }
        } catch (IOException ex) {
            System.out.println("Error receiving packet: " + ex);
            ex.printStackTrace();
        }
    }

    public String getRoute(String dest) {

        if (myaddress.equals("7f00.0101.0000.1001")) {
            return (String) um.get(dest);
        } else if (myaddress.equals("7f00.0101.0000.1002")) {
            return (String) dois.get(dest);
        } else if (myaddress.equals("7f00.0101.0000.1003")) {
            return (String) tres.get(dest);
        } else if (myaddress.equals("7f00.0101.0000.1004")) {
            return (String) quatro.get(dest);
        }
        return "ERRO";
    }

    public boolean isWorthIt(String myaddr, String clusterhd, String nexthp) {
        Hashtable addr = whichHashCost(myaddr);
        // Hashtable clsterhd = whichHashCost(clusterhd);
        // Hashtable nxthp = whichHashCost(nexthp);
        //if ((addr.get(myaddr) == null) || (clsterhd.get(clusterhd) == null) || (nxthp.get(nexthp) == null)) {
        if (addr == null) {
            return false;
        }
        if ((addr.get(clusterhd) == null) || (addr.get(nexthp) == null)) {
            return false;
        }
        int temp1 = ((Integer) addr.get(clusterhd)).intValue();
        int temp2 = ((Integer) addr.get(nexthp)).intValue();
        if (temp1 <= temp2) {
            return true;
        } else {
            return false;
        }

    }

    public boolean isWorthIt(String dest) {
        Integer temp1 = (Integer) whichHashCost(myaddress).get(myaddress);
        Integer temp2 = (Integer) whichHashCost(rcvnexthop).get(rcvnexthop);
        if (temp1.intValue() <= temp2.intValue()) {
            return true;
        } else {
            return false;
        }
    }

    public Hashtable whichHashNode(String address) {
        if (address.equals("7f00.0101.0000.1001")) {
            return um;
        } else if (address.equals("7f00.0101.0000.1002")) {
            return dois;
        } else if (address.equals("7f00.0101.0000.1003")) {
            return tres;
        } else if (address.equals("7f00.0101.0000.1004")) {
            return quatro;
        }
        return null;
    }

    public Hashtable whichHashCost(String address) {
        if (address.equals("7f00.0101.0000.1001")) {
            return umCost;
        } else if (address.equals("7f00.0101.0000.1002")) {
            return doisCost;
        } else if (address.equals("7f00.0101.0000.1003")) {
            return tresCost;
        } else if (address.equals("7f00.0101.0000.1004")) {
            return quatroCost;
        }
        return null;
    }
}
