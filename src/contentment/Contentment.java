package contentment;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.rms.*;
import javax.wireless.messaging.*;

/**
 * @author Revence
 */

class CPUtils
{
    public static String uriEscape(String org)
    {
        StringBuffer sb = new StringBuffer();
        for(int notI = 0, ol = org.length(); notI < ol; ++notI)
        {
            char it = org.charAt(notI);
            switch(it)
            {
                case ' ': case '\n': case '\r': case '?': case ':': case '/':
                case '&': case '"': case '+': case '=': case '%':
                    sb.append("%" + Integer.toHexString((int) it));
                    continue;
                default:
                    sb.append(it);
            }
        }
        return sb.toString();
    }

    public static String uriUnescape(String org) throws NumberFormatException
    {
        StringBuffer sb = new StringBuffer();
        int pos = 0, eos = 0, ol = org.length(), pcat = 0;
        while(pos < ol)
        {
            pcat = org.indexOf("%", pos);
            if(pcat < pos)
            {
                sb.append(org.substring(pos));
                break;
            }
            sb.append(org.substring(pos, pcat));
            try
            {
                sb.append((char) Integer.parseInt(org.substring(pcat + 1, pcat + 3), 16));
                pos = pcat + 3;
            }
            catch(NumberFormatException nfe)
            {
                throw nfe;
            }
        }
        return sb.toString();
    }
}

class CPPublisher
{
    private String nom, number;
    private MessageConnection msgc;
    private MIDlet mama;
    public CPPublisher(MIDlet m, String n, String d)
    {
        nom    = n;
        number = d;
        mama   = m;
    }

    public String name()
    {
        return nom;
    }

    public String number()
    {
        return number;
    }

    MessageConnection messagesOrQuit(Displayable after)
    {
        if(msgc != null || number.equals("Core App")) return msgc;
        Alert sht = null;
        try
        {
            msgc  = (MessageConnection) Connector.open("sms://" + number);
        }
        catch(IOException _)
        {
            sht = new Alert("Messaging Error", "Messages to " + nom + " have not been allowed.", null, AlertType.ERROR);
        }
        catch(IllegalArgumentException iae)
        {
            sht = new Alert("Messaging Error", "Cannot send messages to \"" + number + "\".", null, AlertType.ERROR);
        }
        if(sht != null) Display.getDisplay(mama).setCurrent(sht, after);
        return msgc;
    }
}

class BadCollectionException extends Exception
{
    public BadCollectionException(String complaint)
    {
        super(complaint);
    }
}

interface StringCollector
{
    public String collect() throws BadCollectionException;
}

class SegmentationFaultEtc extends Exception
{
    private String prog, message;
    public SegmentationFaultEtc(String prg, String msg)
    {
        super("Error in " + prg + ": " + msg);
        prog = prg;
        message = msg;
    }

    public String message()
    {
        return message;
    }

    public String program()
    {
        return prog;
    }
}

interface FaultHandler
{
    public void handle(SegmentationFaultEtc sigseg);
}

class DataInput extends TextBox
{
    public DataInput(String title, String text, int maxSize, int constraints, Command [] cmds, CommandListener lst)
    {
        super(title, text, maxSize, constraints);
        for(int notI = 0; notI < cmds.length; ++notI)
            addCommand(cmds[notI]);
        setCommandListener(lst);
    }
}

class CPPasswordBox extends TextBox
{
    public CPPasswordBox(String ttl)
    {
        super(ttl, "", 1000, TextField.ANY | TextField.SENSITIVE | TextField.PASSWORD);
    }

    public String codedString()
    {
        int times  = (int) (new Date().getTime() % 60000);
        String ans = CPUtils.uriEscape(this.getString());
        //  TODO: Encrypt ans and put it back in ans.
        return Integer.toString(times) + ":" + ans;
    }
}

class CPChoiceList extends List
{
    private MIDlet mama;
    private Vector opts;
    public CPChoiceList(String ttl, String code, Command ok, Command ex, CommandListener cl, boolean mult, MIDlet m)
    {
        super(ttl, (mult ? List.MULTIPLE : (List.IMPLICIT | List.EXCLUSIVE)));
        mama = m;
        opts = new Vector(10, 5);
        for(int notI = 0, coln = code.length(), colat = 0, ncolat = 0, nxtpnt = 0; notI < coln;)
        {
            colat = code.indexOf(":", notI);
            if(colat < notI) break;
            nxtpnt = colat + 1;
            ncolat = code.indexOf(":", nxtpnt);
            if(ncolat < nxtpnt) ncolat = coln;
            opts.addElement(code.substring(notI, colat));
            append(CPUtils.uriUnescape(code.substring(nxtpnt, ncolat)), null);
            if(ncolat == coln) break;
            notI = ncolat + 1;
        }
        if(! mult) setSelectCommand(ok);
        addCommand(ok);
        addCommand(ex);
        setCommandListener(cl);
    }

    public void show()
    {
        Display.getDisplay(mama).setCurrent(this);
    }

    public String answer()
    {
        StringBuffer buf = new StringBuffer();
        for(int notI = 0, seen = 0, dl = opts.size(); notI < dl; ++notI)
        {
            if(! this.isSelected(notI)) continue;
            if(seen > 0) buf.append(":");
            buf.append((String) opts.elementAt(notI));
            ++seen;
        }
        return buf.toString();
    }
}

class CPPickNumber extends TextBox implements CommandListener
{
    private Command ccl, there;
    private MIDlet mama;
    private Runnable after;
    private Displayable prev;
    public CPPickNumber(MIDlet m, Displayable p)
    {
        super("Enter a Phone Number", "", 15, TextField.PHONENUMBER);
        ccl   = new Command("Cancel", Command.CANCEL, 0);
        there = new Command("OK", Command.OK, 0);
        mama  = m;
        prev  = p;
        addCommand(ccl);
        addCommand(there);
        setCommandListener(this);
    }

    public void show(Runnable r)
    {
        after = r;
        Display.getDisplay(mama).setCurrent(this);
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == ccl)
            Display.getDisplay(mama).setCurrent(prev);
        else
        {
            String s = this.getString();
            if(this.getString().length() < 7)
            {
                Alert alt = new Alert("Invalid phone number", "\"" + s + "\" is not a valid phone number.", null, AlertType.ERROR);
                Display.getDisplay(mama).setCurrent(alt, this);
            }
            else
            {
                after.run();
            }
        }
    }
}

class CPPhoneNumbers extends List implements CommandListener
{
    private MIDlet mama;
    private Command remove, addnew, okay;
    private CommandListener eavesdropper;
    public CPPhoneNumbers(String ttl, Command ok, Command nope, CommandListener cl, MIDlet m)
    {
        super(ttl, List.EXCLUSIVE | List.IMPLICIT);
        mama   = m;
        okay   = ok;
        remove = new Command("Remove", Command.CANCEL, 1);
        addnew = new Command("Add Number", Command.ITEM, 1);
        eavesdropper = cl;
        addCommand(nope);
        addCommand(addnew);
        setCommandListener(this);
    }

    public void show()
    {
        Display.getDisplay(mama).setCurrent(this);
    }

    public String getString()
    {
        StringBuffer buf = new StringBuffer();
        for(int notI = 0, ts = this.size(); notI < ts; ++notI)
        {
            if(notI > 0) buf.append(":");
            buf.append(this.getString(notI));
        }
        return buf.toString();
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == List.SELECT_COMMAND)
        {
            commandAction(remove, d);
            return;
        }
        if(c == remove)
        {
            this.delete(this.getSelectedIndex());
            if(this.size() < 1)
            {
                this.removeCommand(remove);
                this.removeCommand(okay);
            }
        }
        else if(c == addnew)
        {
            final CPPhoneNumbers meself = this;
            final CPPickNumber pick = new CPPickNumber(mama, this);
            pick.show(new Runnable()
            {
                public void run()
                {
                    meself.append(pick.getString(), null);
                    Display.getDisplay(mama).setCurrent(meself);
                    if(meself.size() == 1)
                    {
                        meself.addCommand(remove);
                        addCommand(okay);
                    }
                }
            });
        }
        else
            eavesdropper.commandAction(c, d);
    }
}

class CPProgram implements CommandListener
{
    private String program, name;
    private Displayable after;
    private MIDlet mama;
    private CPPublisher pub;
    private Form disp;
    private int notI = 0, nxt = 0, lst = 0;
    private Command exit, advc;
    private StringBuffer rez;
    private Runnable updater;
    private CPProgram enfin;
    private StringCollector collector;
    private FaultHandler onfault;
    private Calendar cld;
    private boolean nosend;
    public CPProgram(String p, MIDlet m, CPPublisher q, String n)
    {
        program = p;
        pub     = q;
        mama    = m;
        name    = n;
        cld     = Calendar.getInstance();
        nosend  = false;
        cld.setTime(new Date());

    }

    public void run(Displayable d, Runnable u, FaultHandler onf)
    {
        onfault = onf;
        rez = new StringBuffer("");
        after = d;
        updater = u;
        disp = new Form(name);
        disp.setCommandListener(this);
        exit = new Command("Exit", Command.EXIT, 0);
        advc = new Command("OK", Command.OK, 0);
        disp.addCommand(exit);
        disp.addCommand(advc);
        Display.getDisplay(mama).setCurrent(disp);
        commandAction(advc, disp);
    }

    public void commandAction(Command c, Displayable d)
    {
        try
        {
            try
            {
                commandActionHandler(c, d);
            }
            catch(Exception e)
            {
                throw new SegmentationFaultEtc(name, (e.getMessage() == null ? "Programming error." : e.getMessage()));
            }
        }
        catch(SegmentationFaultEtc sigseg)
        {
            onfault.handle(sigseg);
        }
    }

    public void commandActionHandler(Command c, Displayable d) throws SegmentationFaultEtc
    {
        if(d != disp)
        {
            Display.getDisplay(mama).setCurrent(disp);
        }
        if(c == exit)
        {
            Display.getDisplay(mama).setCurrent(after);
        }
        else if(c == advc)
        {
            if(collector != null)
            {
                try
                {
                    rez.append(collector.collect());
                    collector = null;
                }
                catch(BadCollectionException bce)
                {
                    Alert sht = new Alert("Bad Data", bce.getMessage(), null, AlertType.ERROR);
                    Display.getDisplay(mama).setCurrent(sht,
                            Display.getDisplay(mama).getCurrent());
                    return;
                }
            }
            if(notI >= program.length())
            {
                if(! nosend)
                {
                    final MessageConnection msgc = pub.messagesOrQuit(after);
                    if(msgc != null)
                    {
                        final TextMessage tm = (TextMessage) msgc.newMessage(MessageConnection.TEXT_MESSAGE);
                        tm.setPayloadText(rez.toString());
                        final Alert sdg = new Alert("Sending message in the background ...", rez.toString(), null, AlertType.ERROR);
                        Thread bg = new Thread(new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    msgc.send(tm);
                                    if(enfin != null)
                                    {
                                        enfin.run(after, updater, onfault);
                                        return;
                                    }
                                    Display.getDisplay(mama).setCurrent(sdg,
                                            Display.getDisplay(mama).getCurrent());
                                }
                                catch(IOException ioe)
                                {
                                    Alert sht = new Alert("Failed to Send Message", ioe.getMessage(), null, AlertType.ERROR);
                                    Display.getDisplay(mama).setCurrent(sht,
                                            Display.getDisplay(mama).getCurrent());
                                }
                            }
                        });
                        bg.start();
                    }
                }
                Display.getDisplay(mama).setCurrent(after);
                return;
            }
            else
            {
                nxt = program.indexOf("{", notI);
                if(nxt == -1)
                {
                    rez.append(program.substring(notI));
                    notI = program.length();
                    commandAction(c, d);
                    return;
                }
                rez.append(program.substring(notI, nxt));
                lst = program.indexOf("}", nxt + 1);
                String opc = program.substring(nxt + 1, lst);
                if(opc.equals("exit"))
                {
                    notI = program.length();
                    nosend = true;
                    commandAction(c, d);
                    return;
                }
                else if(opc.equals("update"))
                {
                    String dft = "Running system update in the background.";
                    StringItem si = new StringItem("", dft);
                    disp.deleteAll();
                    disp.append(si);
                    Thread bg = new Thread(updater);
                    bg.start();
                }
                else
                {
                    int sp     = opc.indexOf(32);
                    String cmd = opc.substring(0, sp),
                           rst = (sp == -1 ? "" : opc.substring(sp + 1));
                    if(cmd.equals("call"))
                    {
                        String sfr = "Starting call ...";
                        StringItem stt = new StringItem(rst, sfr);
                        disp.deleteAll();
                        disp.append(stt);
                        try
                        {
                            mama.platformRequest("tel:" + rst);
                            stt.setText(sfr + " finished.");
                        }
                        catch(ConnectionNotFoundException cnf)
                        {
                            stt.setText(sfr + " failed: " + cnf.getMessage());
                        }
                    }
                    else if(cmd.equals("year"))
                    {
                        disp.deleteAll();
                        final TextField yr = new TextField((rst.equals("") ? "Year" : rst), Integer.toString(cld.get(Calendar.YEAR)), 4, TextField.NUMERIC);
                        collector = new StringCollector()
                        {
                           public String collect()
                           {
                               return yr.getString();
                           }
                        };
                        disp.append(yr);
                    }
                    else if(cmd.equals("month"))
                    {
                        disp.deleteAll();
                        String [] mths = {"January", "February", "March", "April",
                                          "May", "June", "July", "August",
                                          "September", "October", "November", "December"};
                        final ChoiceGroup mn = new ChoiceGroup((rst.equals("") ? "Month" : rst),
                                Choice.EXCLUSIVE,
                                mths,
                                null);
                        mn.setSelectedIndex(cld.get(Calendar.MONTH), true);
                        collector = new StringCollector()
                        {
                           public String collect() throws BadCollectionException
                           {
                               int sel = mn.getSelectedIndex();
                               if(sel < 0)
                                   throw new BadCollectionException("Choose a month.");
                               return Integer.toString(mn.getSelectedIndex() + 1);
                           }
                        };
                        disp.append(mn);
                    }
                    else if(cmd.equals("day"))
                    {
                        disp.deleteAll();
                        final TextField dy = new TextField((rst.equals("") ? "Day of the Month" : rst),
                                Integer.toString(cld.get(Calendar.DATE)), 2, TextField.NUMERIC);
                        collector = new StringCollector()
                        {
                           public String collect() throws BadCollectionException
                           {
                               int lint = Integer.parseInt(dy.getString());
                               if(lint > 31 || lint < 1)
                                   throw new BadCollectionException("The day of the month is supposed to be between the 1st and the 31st.");
                               return dy.getString();
                           }
                        };
                        disp.append(dy);
                    }
                    else if(cmd.equals("password"))
                    {
                        final CPPasswordBox dy = new CPPasswordBox((rst.equals("") ? "Your Password" : rst));
                        dy.addCommand(exit);
                        dy.addCommand(advc);
                        dy.setCommandListener(this);
                        Display.getDisplay(mama).setCurrent(dy);
                        collector = new StringCollector()
                        {
                           public String collect()
                           {
                               return dy.codedString();
                           }
                        };
                    }
                    else if(cmd.equals("number"))
                    {
                        disp.deleteAll();
                        int dft = 0;
                        String ttl = rst;
                        try
                        {
                            dft = Integer.parseInt(rst);
                            ttl = "Number";
                        }
                        catch(NumberFormatException nfe) {}
                        final TextField dy = new TextField(ttl, Integer.toString(dft), 13, TextField.NUMERIC);
                        collector = new StringCollector()
                        {
                           public String collect()
                           {
                               return dy.getString();
                           }
                        };
                        disp.append(dy);
                    }
                    else if(cmd.equals("phone"))
                    {
                        disp.deleteAll();
                        String ttl = rst;
                        final TextField dy = new TextField(ttl, "", 40, TextField.PHONENUMBER);
                        collector = new StringCollector()
                        {
                           public String collect()
                           {
                               return dy.getString();
                           }
                        };
                        disp.append(dy);
                    }
                    else if(cmd.equals("phones"))
                    {
                        final CPPhoneNumbers phns = new CPPhoneNumbers(rst, advc, exit, this, mama);
                        collector = new StringCollector()
                        {
                            public String collect() throws BadCollectionException
                            {
                                if(phns.size() < 1)
                                {
                                    throw new BadCollectionException("You have not entered any phone numbers.");
                                }
                                return phns.getString();
                            }
                        };
                        phns.show();
                    }
                    else if(cmd.equals("bool"))
                    {
                        disp.deleteAll();
                        String [] yn = {"Yes", "No"};
                        final ChoiceGroup bl = new ChoiceGroup(rst, Choice.EXCLUSIVE, yn, null);
                        collector = new StringCollector()
                        {
                           public String collect()
                           {
                               return (bl.getSelectedIndex() == 0 ? "Y" : "N");
                           }
                        };
                        disp.append(bl);
                    }
                    else if(cmd.equals("choice") || cmd.equals("choices"))
                    {
                        int spc = rst.indexOf(32);
                        if(spc >= 0)
                        {
                            String ttl = CPUtils.uriUnescape(rst.substring(0, spc));
                            final CPChoiceList chl = new CPChoiceList(ttl, rst.substring(spc + 1), advc, exit, this, cmd.equals("choices"), mama);
                            chl.show();
                            collector = new StringCollector()
                            {
                                public String collect()
                                {
                                    return chl.answer();
                                }
                            };
                        }
                    }
                    else if(cmd.equals("url"))
                    {
                        disp.deleteAll();
                        String dft = "Loading URL ...";
                        StringItem si = new StringItem(rst, dft);
                        disp.append(si);
                        try
                        {
                            mama.platformRequest(rst);
                            si.setText(dft + " finished.");
                        }
                        catch(ConnectionNotFoundException cnfe)
                        {
                            si.setText(dft + " failed (" + cnfe.getMessage() + ").");
                        }
                    }
                    else if(cmd.equals("text"))
                    {
                        int sze = rst.indexOf(" ");
                        int size = 1000;
                        try
                        {
                            size = Integer.parseInt(rst.substring(0, sze));
                        }
                        catch(NumberFormatException nfe) {}
                        final TextField tf = new TextField(rst.substring(sze + 1), "", size, TextField.ANY);
                        collector = new StringCollector()
                        {
                            public String collect()
                            {
                                return tf.getString();
                            }
                        };
                        disp.deleteAll();
                        disp.append(tf);
                    }
                    else if(cmd.equals("data"))
                    {
                        int sze = rst.indexOf(" ");
                        int size = 1000;
                        try
                        {
                            size = Integer.parseInt(rst.substring(0, sze));
                        }
                        catch(NumberFormatException nfe)
                        {
                            sze = -1;
                        }
                        Command [] cmds = {exit, advc};
                        final DataInput di = new DataInput(rst.substring(sze + 1), "", size, TextField.ANY, cmds, this);
                        collector = new StringCollector()
                        {
                            public String collect()
                            {
                                return CPUtils.uriEscape(di.getString());
                            }
                        };
                        Display.getDisplay(mama).setCurrent(di);
                    }
                    else if(cmd.equals("show"))
                    {
                        disp.deleteAll();
                        disp.append(new StringItem(name, rst));
                    }
                    else if(cmd.equals("exec"))
                    {
                        try
                        {
                            HttpConnection cn = (HttpConnection) Connector.open(rst);
                            if(cn.getResponseCode() != 200)
                                throw new IOException(rst + " did not respond as expected.");
                            InputStream ins = cn.openInputStream();
                            byte [] dem     = new byte[ins.available()];
                            ins.read(dem);
                            ins.close();
                            cn.close();
                            program         = new String(dem);
                            enfin           = this;
                            lst             = program.length();
                        }
                        catch(IOException ioe)
                        {
                            disp.deleteAll();
                            disp.append(name + " could not finish successfully. " + ioe.getMessage());
                        }
                    }
                    else
                    {
                        disp.deleteAll();
                        disp.append(opc);
                    }
                }
                notI = lst + 1;
            }
        }
    }
}

class CPApplication
{
    private CPPublisher pub;
    private String descr, name, program;
    public CPApplication(CPPublisher p, String n, String d, String c)
    {
        pub     = p;
        name    = n;
        descr   = d;
        program = c;
    }

    public CPPublisher publisher()
    {
        return pub;
    }

    public String name()
    {
        return name;
    }

    public String description()
    {
        return descr;
    }

    public CPProgram program(MIDlet m)
    {
        return new CPProgram(program, m, pub, name);
    }
}

class CPUpdaterApp extends CPApplication
{
    public CPUpdaterApp(MIDlet m, boolean empty)
    {
        super(new CPPublisher(m, "inSCALE Project", "Core App"),
              (empty ? "Install Questionnaire" : "Update Questionnaire"),
              (empty ? "Welcome to inSCALE. Run this to load the questionnaire." : "Get the latest questionnaires."),
              "{update}{exit}");
    }
}

class CPServices extends Vector
{
    public CPServices(MIDlet m)
    {
        super(10, 10);
        loadAppsIn(m);
    }

    void loadAppsIn(MIDlet m)
    {
        try
        {
            RecordStore rs = RecordStore.openRecordStore("contentment", false);
            String prov = new String(rs.getRecord(1));
            String apps = new String(rs.getRecord(2));
            Vector pubs = new Vector(10, 5);
            for(int notI = 0, trm = 0, fin = 0; ;)
            {
                ++notI;
                if(notI >= prov.length()) break;
                trm = prov.indexOf(0, notI);
                fin = prov.indexOf(0, trm + 2);
                fin = (fin < 0 ? prov.length() : fin);
                String num = prov.substring(notI, trm),
                       nom = prov.substring(trm + 1, fin);
                pubs.addElement(new CPPublisher(m, nom, num));
                notI = fin;
            }
            for(int notI = 0, pend = 0, aned = 0, pos = 0, dend = 0, fin = 0; ;)
            {
                ++notI;
                if(notI >= apps.length()) break;
                pend = apps.indexOf(3, notI);
                pos  = Integer.parseInt(apps.substring(notI, pend));
                aned = apps.indexOf(3, pend + 1);
                String nom = apps.substring(pend + 1, aned);
                dend = apps.indexOf(3, aned + 1);
                String desc = apps.substring(aned + 1, dend);
                fin = apps.indexOf(3, dend + 1);
                fin = (fin < 0 ? apps.length() : fin);
                String prog;
                if(fin < 0)
                    prog = apps.substring(dend + 1);
                else
                    prog = apps.substring(dend + 1, fin);
                this.addApp(new CPApplication((CPPublisher) pubs.elementAt(pos), nom, desc, prog));
                notI = fin;
            }
            rs.closeRecordStore();
        }
        catch(RecordStoreNotFoundException _) {}
        catch(RecordStoreException rse)
        {
            Alert sht = new Alert("Phone Memory Error", rse.getMessage(), null, AlertType.ERROR);
            Display.getDisplay(m).setCurrent(sht, Display.getDisplay(m).getCurrent());
        }
        this.addApp(new CPUpdaterApp(m, this.size() == 0));
    }

    String dbVersion()
    {
        try
        {
            RecordStore rs = RecordStore.openRecordStore("version", false);
            String ans = new String(rs.getRecord(1));
            rs.closeRecordStore();
            return ans;
        }
        catch(RecordStoreException _) {}
        return "fresh";
    }

    public void runUpdate(MIDlet m)
    {
        try
        {
            String vsn = m.getAppProperty("MIDlet-Version");
            String u = "http://detamble.com/blogs/inscale/update.cgi?version=" + (vsn == null ? "1.0" : vsn) + "&status=" + dbVersion() + "&client=inscale";
            HttpConnection ucon = (HttpConnection) Connector.open(u);
            InputStream inlet = ucon.openInputStream();
            if(ucon.getResponseCode() != 200)
            {
                Alert sht = new Alert("HTTP Error " + Integer.toString(ucon.getResponseCode()), "Is your Internet connection working well?", null, AlertType.ERROR);
                Display.getDisplay(m).setCurrent(sht,
                        Display.getDisplay(m).getCurrent());
                return;
            }
            byte [] them = new byte[(int) ucon.getLength()];
            inlet.read(them);
            inlet.close();
            ucon.close();
            String rsp = new String(them);
            Alert sht  = null;
            if(rsp.substring(0, 2).equals("OK"))
            {
                Alert alw = new Alert("Up-to-Date", "Everything you have is up-to-date.", null, AlertType.INFO);
                Display.getDisplay(m).setCurrent(alw, Display.getDisplay(m).getCurrent());
            }
            else if(rsp.substring(0, 6).equals("UPDATE"))
            {
                Alert alw = new Alert("Updating", "There is a new version of the inSCALE cliet! Accept the update. Thank you!", null, AlertType.INFO);
                Display.getDisplay(m).setCurrent(alw, Display.getDisplay(m).getCurrent());
                m.platformRequest(rsp.substring(1 + rsp.indexOf(0)));
            }
            else
            {
                int rss = rsp.indexOf(0);
                int rse = rsp.indexOf(0, rss + 1);
                byte [] ver = rsp.substring(rss + 1, rse).getBytes();
                String dat  = rsp.substring(rse);
                int prvi    = dat.indexOf(1);
                byte [] prv = dat.substring(0, prvi).getBytes();
                byte [] aps = dat.substring(prvi).getBytes();
                try
                {
                    try
                    {
                        RecordStore.deleteRecordStore("contentment");
                    }
                    catch(RecordStoreNotFoundException _) {}
                    try
                    {
                        RecordStore.deleteRecordStore("version");
                    }
                    catch(RecordStoreNotFoundException _) {}
                    RecordStore rs = RecordStore.openRecordStore("contentment", true);
                    RecordStore rv = RecordStore.openRecordStore("version", true);
                    rs.addRecord(prv, 0, prv.length);
                    rs.addRecord(aps, 0, aps.length);
                    rv.addRecord(ver, 0, ver.length);
                    rs.closeRecordStore();
                    rv.closeRecordStore();
                    sht = new Alert("Updated!", "Updated successfully to version \"" + new String(ver) + "\".", null, AlertType.INFO);
                }
                catch(RecordStoreException re)
                {
                    sht = new Alert("Update Failed", "Could not save application update: " + re.getMessage(), null, AlertType.ERROR);
                }
                if(sht != null)
                {
                    Display.getDisplay(m).setCurrent(sht, Display.getDisplay(m).getCurrent());
                }
            }
        }
        catch(IOException ioe)
        {
            String fmsg = ioe.getMessage();
            if(fmsg == null)fmsg = "Network failure";
            fmsg += " (" + ioe.toString() + "). HTTP request could not be completed, due to a network problem.";
            Alert sht = new Alert("System Update Failed", fmsg + "\nYou can try running the update later on.", null, AlertType.ERROR);
            Display.getDisplay(m).setCurrent(sht,
                    Display.getDisplay(m).getCurrent());
        }
    }

    public void runBackgroundUpdate(MIDlet m)
    {
        final MIDlet mama = m;
        Thread thd = new Thread(new Runnable()
        {
            public void run()
            {
                runUpdate(mama);
            }
        });
        thd.start();
    }

    public void addApp(CPApplication app)
    {
        this.addElement(app);
    }

    public void clearApps()
    {
        this.removeAllElements();
    }

    public void reset(MIDlet m)
    {
        clearApps();
        loadAppsIn(m);
    }

    public CPApplication[] applications()
    {
        CPApplication[] them = new CPApplication[this.size()];
        this.copyInto(them);
        return them;
    }
}

class CPDescriptionPage extends Form implements CommandListener
{
    private CPApplication app;
    private Command back, run;
    private Displayable prev;
    private MIDlet mama;
    private Runnable updater;
    private FaultHandler onfault;
    public CPDescriptionPage(CPApplication a, Displayable p, MIDlet m, Runnable u, FaultHandler onf)
    {
        super(a.name() + " by " + a.publisher().name() +
                " (" + a.publisher().number() + ")");
        app  = a;
        prev = p;
        mama = m;
        back = new Command("Back", Command.BACK, 0);
        run  = new Command("Run", Command.OK, 0);
        onfault = onf;
        updater = u;
        append(new StringItem("", a.description() + "\n\n" + a.name() + " by " + a.publisher().name() +
                " (" + a.publisher().number() + ")" ));
        this.setCommandListener(this);
        this.addCommand(back);
        this.addCommand(run);
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == back)
            Display.getDisplay(mama).setCurrent(prev);
        else
            app.program(mama).run(prev, updater, onfault);
    }
}

class CPFrontPage extends List implements CommandListener
{
    private CPServices srv;
    private Command quit, desc, updt;
    private MIDlet mama;
    private Runnable updater;
    private FaultHandler onfault;
    public CPFrontPage(String tt, CPServices s, MIDlet m)
    {
        super(tt, Choice.EXCLUSIVE | Choice.IMPLICIT | Choice.TEXT_WRAP_OFF);
        srv     = s;
        quit    = new Command("Quit", Command.EXIT, 0);
        desc    = new Command("Describe", Command.HELP, 5);
        mama    = m;
        final List meself = this;
        updater = new Runnable()
        {
            public void run()
            {
                srv.runUpdate(mama);
                srv.reset(mama);
                meself.deleteAll();
                listApps();
            }
        };
        onfault = new FaultHandler()
        {
            public void handle(SegmentationFaultEtc sigseg)
            {
                Alert sht = new Alert("Error", sigseg.getMessage(), null, AlertType.ERROR);
                Display.getDisplay(mama).setCurrent(sht, meself/*Display.getDisplay(mama).getCurrent()*/);
            }
        };
        listApps();
        setCommandListener(this);
        addCommand(desc);
        addCommand(quit);
    }

    void listApps()
    {
        CPApplication [] apps = srv.applications();
        for(int notI = 0; notI < apps.length; ++notI)
            append(apps[notI].name(), null);
    }

    public void commandAction(Command c, Displayable d)
    {
        if(c == quit)
            mama.notifyDestroyed();
        else if(c == desc)
        {
            CPDescriptionPage des = new CPDescriptionPage((CPApplication) srv.elementAt(this.getSelectedIndex()), this, mama, updater, onfault);
            Display.getDisplay(mama).setCurrent(des);
        }
        else
        {
            ((CPApplication) srv.elementAt(this.getSelectedIndex())).program(mama).run(this, updater, onfault);
        }
    }
}

public class Contentment extends MIDlet
{
    public Contentment()
    {

    }

    public void startApp()
    {
        CPServices svs = new CPServices(this);
        CPFrontPage fp = new CPFrontPage("inSCALE", svs, this);
        Display.getDisplay(this).setCurrent(fp);
    }

    public void pauseApp()
    {

    }

    public void destroyApp(boolean unconditional)
    {

    }
}