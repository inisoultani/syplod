package com.PicasaPhotoUpload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.android.picasaphotouploader.ApplicationNotification;
import com.android.picasaphotouploader.ImageTableObserver;
import com.android.picasaphotouploader.Utils;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
//import android.content.CursorLoader;
import android.support.v4.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class PicasaPhotoUploadActivity extends Activity {
	 /**
	   * Observer that listens to changes on image table
	   */
	  private ImageTableObserver camera;
	
	  /**
	   * Highest image id in database
	   */
	  private int maxId;
	
	  /**
	   * Image item queue
	   */
	  private ExecutorService queue = Executors.newSingleThreadExecutor();
	
	  /**
	   * Menu item to send application to background
	   */
	  private static final int MENU_BACK = 1;
	
	  /**
	   * Menu item for user preferences
	   */
	  private static final int MENU_PREFS = 2;
	
	  private static final int MENU_NOTIFY = 3;
	  /**
	   * Menu item for user preferences
	   */
	  private static final int MENU_LICENSE = 4;
	  
	  /**
	   * Menu item to exit application
	   */
	  private static final int MENU_EXIT = 5;
	  
	  /**
	   * Hold all notification ID's
	   */
	  private List<Integer> liID;
	
	  /**
	   * Main appplication constructor
	   * 
	   * @param parent
	   */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	
  	  	
    	
    	// call parent
        super.onCreate(savedInstanceState);
    	
        // set main layout screen
        setContentView(R.layout.main);

        // get user preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // check if application notification is on
        if (prefs.getString("notification", "").contains("enabled")) {
          ApplicationNotification.getInstance().enable(getBaseContext());
        }

        // store highest image id from database in application
        setMaxIdFromDatabase();

        // register camera observer
        camera = new ImageTableObserver(new Handler(), this, queue);
        getContentResolver().registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, camera);
    }
    
    /**
     * Override parent function so back button won't stop application
     * but instead we send it to background
     * 
     * @param keyCode Button that was pressed
     * @param event Even information
     * @return Parent function
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
      if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        moveTaskToBack(true);
        return true;
      }

      // call parent function for other keys and events
      return super.onKeyDown(keyCode, event);
    }

    /**
     * Create application menu
     * 
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
      // call parent
      super.onCreateOptionsMenu(menu);

      // add menu items
      menu.add(0, MENU_BACK, Menu.NONE, "Send to background").setIcon(android.R.drawable.ic_menu_set_as);
      menu.add(1, MENU_PREFS, Menu.NONE, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
      menu.add(2, MENU_NOTIFY, Menu.NONE, "Clear Notification").setIcon(android.R.drawable.ic_menu_delete);
      menu.add(3, MENU_LICENSE, Menu.NONE, "License").setIcon(android.R.drawable.ic_menu_view);
      menu.add(4, MENU_EXIT, Menu.NONE, "Exit").setIcon(android.R.drawable.ic_menu_close_clear_cancel);

      // return
      return true;
    }

    /**
     * Code to execute when menu item is selected
     * 
     * @param item Menu item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
      switch (item.getItemId()) {
        // move task to background
        case MENU_BACK:
          moveTaskToBack(true);
          break;
        // start user preferences screen
        case MENU_PREFS:
          startActivity(new Intent(this, EditPreferences.class));
          break;
        // clear all notification
        case MENU_NOTIFY:
        	clearNotifications();
            break;
        // show license
        case MENU_LICENSE:
          Utils.textDialog(this, "License Information", getString(R.string.license));
          break;
        // kill the queue, all running notifications and exit application
        // usual way is to use finish() but uploads will keep running otherwise
        case MENU_EXIT:
          queue.shutdownNow();
          ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
          System.exit(0);
          break;
      }

      // return parent
      return (super.onOptionsItemSelected(item));
    }

    /**
     * Store highest image id from image table
     */
    private void setMaxIdFromDatabase()
    {
      String columns[] = new String[]{ Media._ID, Media.DISPLAY_NAME, Media.MINI_THUMB_MAGIC };
      // Cursor cursor    = managedQuery(Media.EXTERNAL_CONTENT_URI, columns, null, null, Media._ID+" DESC");
      
      // works in Honeycomb
      String selection = null;
      String[] selectionArgs = null;
      String sortOrder = Media._ID+" DESC";
      
      CursorLoader cursorLoader = new CursorLoader(
    	        this, 
    	        Media.EXTERNAL_CONTENT_URI, 
    	        columns, 
    	        selection, 
    	        selectionArgs, 
    	        sortOrder);

      Cursor cursor = cursorLoader.loadInBackground();
      startManagingCursor(cursor);
      maxId = cursor.moveToFirst() ? cursor.getInt(cursor.getColumnIndex(Media._ID)) : -1;
    }
    
    /**
     * Clear all upload notifications
     */
    private void clearNotifications(){
    	if (liID == null) return;
    	NotificationManager nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
    	for (Integer id : liID){
    		nm.cancel(id);
    	}
    }

    /**
     * Set highest image id
     *
     * @param maxId New value for maxId
     */
    public void setMaxId(int maxId)
    {
      this.maxId = maxId;
    }

    /**
     * Get highest image id
     * 
     * @return Highest id
     */
    public int getMaxId()
    {
      return maxId;
    }

	public List<Integer> getLiID() {
		return liID;
	}

	public void setLiID(List<Integer> liID) {
		this.liID = liID;
	}
	
	public void addId(Integer id){
		if (liID == null){
			liID = new ArrayList<Integer>();
		}
		this.liID.add(id);
	}
}