/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package contentment;

/**
 *
 * @author Revence
 */

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.*;

class ResultPage extends Form implements CommandListener, Runnable
{
    Displayable prev;
    MIDlet mama;
    StringItem disp;
    Command don;
    Thread thd;
    InputStream ins;
    Player      ply;

    public ResultPage(String t, Displayable d, MIDlet m, int cmpt, boolean auto)
    {
        super(t);
        disp = new StringItem("", t + ": " + (auto ? "please check the results." : Integer.toString(cmpt) + " per minute."));
        mama = m;
        prev = d;
        don  = new Command("Done", Command.OK | Command.EXIT, 0);
        thd  = new Thread(this);
        ins = getClass().getResourceAsStream("/alarma.wav");
        try
        {
            ply = Manager.createPlayer(ins, "audio/X-wav");
        }
        catch(IOException ioe) {}
        catch(MediaException me) {}
        append(disp);
        setCommandListener(this);
        addCommand(don);
        thd.start();
    }

    public void run()
    {
        try
        {
            Display curd = Display.getDisplay(mama);
            curd.flashBacklight(5000);
            curd.vibrate(5000);
            ply.start();
        }
        catch(Exception e){}
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == don && d == this)
        {
            Display.getDisplay(mama).setCurrent(prev);
            ply.close();
            try
            {
                ins.close();
            }
            catch(IOException ioe) {}
        }
    }
}

class CounterPage extends Form implements CommandListener, Runnable
{
    Command inc, don, stt;
    StringItem lab;
    boolean air, automated;
    Displayable prev;
    MIDlet mama;
    int cmpt, ctdn;
    ResultPage rslt;
    String ttl;
    Thread thd;
    public CounterPage(String t, Displayable p, MIDlet m, boolean auto)
    {
        super(t);
        ttl  = t;
        cmpt = 0;
        ctdn = 60;
        prev = p;
        air  = false;
        mama = m;
        lab  = new StringItem("", "Press \"Start\" to begin counting.");
        inc  = new Command("+1", Command.OK, 0);
        stt  = new Command("Start", Command.OK, 0);
        don  = new Command("Done", Command.EXIT, 0);
        automated = auto;
        append(lab);
        addCommand(stt);
        addCommand(don);
        setCommandListener(this);
    }

    public int setCountdown(int newcd)
    {
        int old = ctdn;
        ctdn    = newcd;
        return old;
    }

    public void commandAction(Command c, Displayable d)
    {

        if(! air)
        {
            if(c == don && d == this)
            {
                Display.getDisplay(mama).setCurrent(prev);
            }
            else if(c == stt && d == this)
            {
                air = true;
                removeCommand(stt);
                if(! automated)
                    addCommand(inc);
                thd = new Thread(this);
                thd.start();
                this.increment();
            }
        }
        else
        {
            if(c == don && d == this)
            {
                //  thd.interrupt();    //  Not supported in low-end.
                ctdn = 0;
                this.nextScreen();
            }
        }

        if(c == inc && d == this && air)
        {
            this.increment();
        }
    }

    public void run()
    {
        try
        {
            for(; ctdn > 0; --ctdn)
            {
                repaint();
                Thread.sleep(1000);
                //  if thd.interrpt() no workee, simulate it.
                if(ctdn == 0) throw new InterruptedException();
            }
        }
        catch(InterruptedException e) {}
        this.nextScreen();
    }

    void nextScreen()
    {
        rslt = new ResultPage(ttl, prev, mama, cmpt, automated);
        Display.getDisplay(mama).setCurrent(rslt);
    }

    void increment()
    {
        ++cmpt;
        /*Display d = Display.getDisplay(mama);
        d.vibrate(500);*/
        repaint();
        AlertType.CONFIRMATION.playSound(Display.getDisplay(mama));
    }

    String timeDescription(int seconds)
    {
        int mins = seconds / 60;
        int secs = seconds - (mins * 60);
        //  if(mins < 1) return Integer.toString(secs) + " second" + (secs == 1 ? "s" : "");
        //  return Integer.toString(mins) + " minute" + (mins == 1 ? "" : "s") + " and " + Integer.toString(seconds) + " second" + (secs == 1 ? "s" : "");
        return (mins < 10 ? "0" : "") + Integer.toString(mins) + ":" + (secs < 10 ? "0" : "") + Integer.toString(secs);
    }

    void repaint()
    {
        lab.setText((automated ? "" : Integer.toString(cmpt) + " count" + (cmpt == 1 ? "." : "s.")) +
            (automated ? timeDescription(ctdn) + " left." : ""));
    }
}

public class InitialPage extends List implements CommandListener
{
    Command quitter;
    MIDlet mama;
    Displayable prev;
    CounterPage counter;
    public InitialPage(MIDlet m, Displayable p)
    {
        super("Measure Rate", Choice.IMPLICIT | Choice.EXCLUSIVE);
        mama = m;
        prev = p;
        append("Breathing Rate", null);
        append("Heartbeat Rate", null);
        quitter = new Command("Exit", Command.EXIT, 0);
        addCommand(quitter);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == quitter && d == this)
        {
            if(prev == null)
            {
                mama.notifyDestroyed();
                return;
            }
            Display.getDisplay(mama).setCurrent(prev);
            return;
        }
        if(c != List.SELECT_COMMAND || d != this) return;
        String titre = "Heartbeat Rate";
        switch(getSelectedIndex())
        {
            case 0:
                titre = "Breathing Rate";
                break;
            default:
                //  Is the default.
        }
        counter = new CounterPage(titre, this, mama, false);
        Display.getDisplay(mama).setCurrent(counter);
    }
}
