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
        StoreManager.write(place, 0, what);
    }

    public static void write(String place, int pos, String what) throws RecordStoreException
    {
        RecordStore rs = RecordStore.openRecordStore(place, true);
        byte [] data   = what.getBytes();
        rs.addRecord(data, pos, data.length);
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
}