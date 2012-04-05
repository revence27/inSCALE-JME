/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package contentment;

/**
 *
 * @author Revence
 */

import javax.microedition.rms.*;

/*
 *  Provides a simple high-level interface to RMS.
 */
public class StoreManager
{
    public static String read(String place) throws RecordStoreException
    {
        return StoreManager.read(place, 1);
    }

    public static String read(String place, int pos) throws RecordStoreException
    {
        RecordStore rs = RecordStore.openRecordStore(place, true);
        String ans = new String(rs.getRecord(pos));
        rs.closeRecordStore();
        return ans;
    }

    public static void write(String place, String what) throws RecordStoreException
    {
        RecordStore rs = RecordStore.openRecordStore(place, true);
        byte [] data   = what.getBytes();
        try
        {
            rs.setRecord(1, data, 0, data.length);
        }
        catch(InvalidRecordIDException irie)
        {
            //  irie.printStackTrace();
            rs.addRecord(data, 0, data.length);
        }
        rs.closeRecordStore();
    }

    public static boolean exists(String place)
    {
        boolean ans = false;
        String them[] = RecordStore.listRecordStores();
        if(them == null) return false;
        for(int notI = 0; notI < them.length; ++notI)
            if(them[notI].equals(place)) return true;
        return ans;
    }

    public static void append(String place, String what) throws RecordStoreException
    {
        append(place, 1, what);
    }

    public static void append(String place, int pos, String what) throws RecordStoreException
    {
        RecordStore rs = RecordStore.openRecordStore(place, true);
        String ans     = new String(rs.getRecord(pos));
        byte [] data   = (ans + what).getBytes();
        rs.setRecord(pos, data, 0, data.length);
        rs.closeRecordStore();
    }
}