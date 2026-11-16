package com.mensagemrapida;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Entrega as mídias salvas para outros apps (WhatsApp, Instagram…) com permissão temporária
 * de leitura. Faz o papel do FileProvider da AndroidX, que não usamos.
 */
public class ArquivoProvider extends ContentProvider {

    static final String AUTORIDADE = "com.mensagemrapida.arquivos";

    static Uri uri(Dados.Midia m) {
        return Uri.parse("content://" + AUTORIDADE + "/" + m.arquivo);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File arquivo(Uri uri) throws FileNotFoundException {
        String nome = uri.getLastPathSegment();
        if (nome == null || nome.contains("/") || nome.contains("..")) throw new FileNotFoundException();
        File f = new File(Dados.de(getContext()).pastaMidias(), nome);
        if (!f.exists()) throw new FileNotFoundException();
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(arquivo(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        Dados.Midia m = Dados.de(getContext()).midiaPorArquivo(String.valueOf(uri.getLastPathSegment()));
        return m != null ? m.mime : "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Dados.Midia m = Dados.de(getContext()).midiaPorArquivo(String.valueOf(uri.getLastPathSegment()));
        if (m == null) return null;
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{m.arquivo, m.tamanho});
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }
}
