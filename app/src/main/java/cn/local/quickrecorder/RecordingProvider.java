package cn.local.quickrecorder;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

public final class RecordingProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File file(Uri uri) throws FileNotFoundException {
        if (!"cn.local.quickrecorder.files".equals(uri.getAuthority()) || uri.getPathSegments().size()!=1) throw new FileNotFoundException();
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[0-9]{8}_[0-9]{6}_[0-9]{3}\\.m4a")) throw new FileNotFoundException();
        File f = new File(Status.dir(getContext()),name);
        if (!f.isFile()) throw new FileNotFoundException();
        return f;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) { return "audio/mp4"; }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        try {
            File f = file(uri); String[] cols = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE} : projection;
            MatrixCursor c = new MatrixCursor(cols); Object[] row = new Object[cols.length];
            for(int i=0;i<cols.length;i++) row[i] = OpenableColumns.DISPLAY_NAME.equals(cols[i]) ? f.getName() : OpenableColumns.SIZE.equals(cols[i]) ? f.length() : null;
            c.addRow(row); return c;
        } catch(FileNotFoundException e) { return null; }
    }
    @Override public Uri insert(Uri u,ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u,String s,String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u,ContentValues v,String s,String[] a) { throw new UnsupportedOperationException(); }
}
