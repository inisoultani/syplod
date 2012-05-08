package com.android.picasaphotouploader;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 * Class with support functions
 * 
 * @author Hooiveld
 */
public final class Utils
{
  public static void textDialog(Context context, String title, String text)
  {
    new AlertDialog
    .Builder(context)
    .setTitle(title)
    .setMessage(text)
    .setNeutralButton("OK",
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
          dialog.cancel();
        }
      }
    )
    .show();
  }
}
