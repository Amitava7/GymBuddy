package com.gymbuddy.app;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/** Backup & Restore: export everything to a JSON file, or bring one back in. */
public final class BackupActivity extends Screen {

    private static final int PICK = 1;

    private ProgressBar spinner;
    private View exportCard, importCard;
    private final Handler ui = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        header("Backup & Restore", true);
        LinearLayout col = Ui.column(this);
        Ui.pad(col, 20, 20, 20, 20);
        exportCard = card(R.drawable.ic_share_outline, "Export Data",
                "Save gyms, exercises, templates and workout history to a JSON file");
        Ui.touch(exportCard, 0.7f, new View.OnClickListener() {
            public void onClick(View v) {
                exportData();
            }
        });
        importCard = card(R.drawable.ic_download_outline, "Import Data", "Restore from a GymBuddy backup file");
        Ui.touch(importCard, 0.7f, 1f, new View.OnClickListener() {
            public void onClick(View v) {
                pickFile();
            }
        });
        col.addView(exportCard, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        col.addView(importCard, Ui.margins(this, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));
        spinner = new ProgressBar(this);
        spinner.setIndeterminateTintList(ColorStateList.valueOf(Ui.PRIMARY));
        spinner.setVisibility(View.GONE);
        col.addView(spinner, Ui.margins(this, Ui.dp(this, 36), Ui.dp(this, 36), 0, 24, 0, 0));
        ((LinearLayout.LayoutParams) spinner.getLayoutParams()).gravity = android.view.Gravity.CENTER_HORIZONTAL;
        content.addView(col);
        openFromIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        openFromIntent(intent);
    }

    @Override
    void load() {
        // Back from the share sheet: the export is done.
        if (exporting) {
            exporting = false;
            setBusy(false);
        }
    }

    private View card(int icon, String title, String subtitle) {
        LinearLayout card = Ui.row(this);
        card.setBackground(Ui.card(this, 14));
        Ui.pad(card, 16, 16, 16, 16);
        View box = Ui.iconBox(this, icon, 44, 12, 24);
        ((LinearLayout.LayoutParams) box.getLayoutParams()).setMarginEnd(Ui.dp(this, 14));
        card.addView(box);
        LinearLayout text = Ui.column(this);
        text.addView(Ui.text(this, title, 16, Ui.TEXT, 600));
        TextView s = Ui.text(this, subtitle, 13, Ui.TEXT_SECONDARY, 400);
        text.addView(s, Ui.margins(this, Ui.WRAP, Ui.WRAP, 0, 2, 0, 0));
        card.addView(text, Ui.lp(0, Ui.WRAP, 1f));
        return card;
    }

    private void setBusy(boolean busy) {
        spinner.setVisibility(busy ? View.VISIBLE : View.GONE);
        exportCard.setEnabled(!busy);
        importCard.setEnabled(!busy);
    }

    // ---------------------------------------------------------------- export

    private boolean exporting;

    private void exportData() {
        setBusy(true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    final File f = writeExport();
                    ui.post(new Runnable() {
                        public void run() {
                            share(f);
                        }
                    });
                } catch (final Exception e) {
                    failed("Export failed", e);
                }
            }
        }, "export").start();
    }

    /** Writes the backup into the cache, where BackupProvider can serve it. */
    File writeExport() throws IOException {
        Map<String, Object> backup = Backup.build(db.getReadableDatabase(), new Date());
        File dir = BackupProvider.dir(this);
        File[] old = dir.listFiles();
        if (old != null) for (File o : old) o.delete();
        File f = new File(dir, Backup.fileName((String) backup.get("exported_at")));
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(Json.stringify(backup).getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        return f;
    }

    private void share(File f) {
        Uri uri = BackupProvider.uriFor(f);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/json");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, f.getName());
        send.setClipData(ClipData.newRawUri(f.getName(), uri));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            exporting = true;
            startActivity(Intent.createChooser(send, "Export GymBuddy data"));
        } catch (Exception e) {
            exporting = false;
            setBusy(false);
            alert("Export failed", "Sharing is not available on this device.", new String[]{"OK"});
        }
    }

    // ---------------------------------------------------------------- import

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "*/*"});
        setBusy(true);
        try {
            startActivityForResult(i, PICK);
        } catch (Exception e) {
            setBusy(false);
            alert("Import failed", e.getMessage(), new String[]{"OK"});
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK) return;
        if (result != RESULT_OK || data == null || data.getData() == null) {
            setBusy(false);
            return;
        }
        read(data.getData());
    }

    /** A backup opened from a file manager or an attachment. */
    private void openFromIntent(Intent i) {
        if (i != null && Intent.ACTION_VIEW.equals(i.getAction()) && i.getData() != null) {
            Uri uri = i.getData();
            i.setData(null);
            setBusy(true);
            read(uri);
        }
    }

    private void read(final Uri uri) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Backup.Data backup = Backup.parse(readText(uri));
                    ui.post(new Runnable() {
                        public void run() {
                            setBusy(false);
                            confirmImport(backup);
                        }
                    });
                } catch (final Exception e) {
                    failed("Import failed", e);
                }
            }
        }, "import-read").start();
    }

    private String readText(Uri uri) throws IOException {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("Could not open the selected file.");
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private void confirmImport(final Backup.Data backup) {
        alert("Import Data",
                "Backup from " + Fmt.localDateTime(Fmt.iso(backup.exportedAt)) + "\n\n"
                        + Backup.summarize(backup).describe()
                        + "\n\nMerge keeps your current data and adds anything new. Replace deletes everything first.",
                new String[]{"Cancel", "Merge", "Replace"},
                null,
                new Runnable() {
                    public void run() {
                        runImport(backup, false);
                    }
                },
                new Runnable() {
                    public void run() {
                        alert("Replace all data?", "All current gyms, exercises and workouts will be deleted.",
                                new String[]{"Cancel", "Replace"}, null, new Runnable() {
                                    public void run() {
                                        runImport(backup, true);
                                    }
                                });
                    }
                });
    }

    private void runImport(final Backup.Data backup, final boolean replace) {
        setBusy(true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Backup.Summary s = Backup.importBackup(db.getWritableDatabase(), backup, replace);
                    ui.post(new Runnable() {
                        public void run() {
                            setBusy(false);
                            alert("Import complete", "Added " + s.describe() + ".", new String[]{"OK"});
                        }
                    });
                } catch (final Exception e) {
                    failed("Import failed", e);
                }
            }
        }, "import").start();
    }

    private void failed(final String title, final Exception e) {
        ui.post(new Runnable() {
            public void run() {
                setBusy(false);
                exporting = false;
                alert(title, e.getMessage() != null ? e.getMessage() : e.toString(), new String[]{"OK"});
            }
        });
    }
}
