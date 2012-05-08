/**
 * This file is part of Picasa Photo Uploader.
 *
 * Picasa Photo Uploader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Picasa Photo Uploader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Picasa Photo Uploader. If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.picasaphotouploader;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Looper;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

/**
 * Override of class ListPreference to show list of Picasa album
 * in preferences to select album to upload images to
 *
 * @author Jan Peter Hooiveld
 */
public class AlbumPreference extends ListPreference
{
	/**
	 * Constructor
	 *
	 * @param context Application context
	 * @param attrs Attributes
	 */
	private String auth;
	private Boolean isDone = false;
	private Boolean isAlbumRetrieveDone = false;
	private Boolean isNoAlbum = true;
	private AlbumList list;

	public AlbumList getList() {
		return list;
	}
	public void setList(AlbumList list) {
		this.list = list;
	}
	
	public String getAuth() {
		return auth;
	}
	public void setAuth(String auth) {
		this.auth = auth;
	}

	public Boolean getIsDone() {
		return isDone;
	}
	public void setIsDone(Boolean isDone) {
		this.isDone = isDone;
	}
	
	public Boolean getIsAlbumRetrieveDone() {
		return isAlbumRetrieveDone;
	}
	public void setIsAlbumRetrieveDone(Boolean isAlbumRetrieveDone) {
		this.isAlbumRetrieveDone = isAlbumRetrieveDone;
	}
	public Boolean getIsNoAlbum() {
		return isNoAlbum;
	}
	public void setIsNoAlbum(Boolean isNoAlbum) {
		this.isNoAlbum = isNoAlbum;
	}
	
	public final AlbumPreference getOuterClass(){
		return this;
	}
	
	public AlbumPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	/**
	 * Constructor
	 *
	 * @param context Application context
	 */
	public AlbumPreference(Context context)
	{
		super(context);
	}

	/**
	 * User clicked on album preference in user preferences
	 */
	@Override
	protected void onClick()
	{
		// get user preferences and then the user email and password
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		final String email            = prefs.getString("email", "").trim();
		final String password         = prefs.getString("password", "").trim();

		// if no email and password are set we can't authenticate with google to
		// retrieve list of albums
		if (email.length() == 0 || password.length() == 0) {
			Utils.textDialog(getContext(), "Notification", "Set username and password first.");
			return;
		}

		// check if we have internet connection to retrieve albums
		if (!CheckInternet.getInstance().canConnect(getContext(), prefs)) {
			Utils.textDialog(getContext(), "Notification", "Can't connect to internet to get Picasa albums.\n\nEither internet is down or your connection in this application is set to allow Wi-Fi only.");
			return;
		}

		// authenticate with google and get new authentication string

		
		class Asyctast extends AsyncTask<String, Integer, Integer>
		{
			@Override
			protected void onPreExecute() {
				//main.progressBar.setVisibility(View.VISIBLE);
			}

			@Override
			protected Integer doInBackground(String... params) {
				// TODO Auto-generated method stub


				Log.d("Asynctask", ""+params);  
				Looper.prepare();   
				GoogleAuthentication google = new GoogleAuthentication(email, password, getContext());
				String auth                 = google.getAuthenticationString();
				getOuterClass().setAuth(auth);
				Log.e("info Asyctast ", "auth value : " + auth);
				getOuterClass().setIsDone(true);
				Looper.loop();
				
				return null;
			}

			@Override
			protected void onPostExecute(Integer result) {
				//getOuterClass().setIsDone(true);
			}  
		}
		
		// retrieve auth in a background process
		//Asyctast asycnAuth = new Asyctast();
		//asycnAuth.execute("test");
		
		class AsyncRetrieveAlbum extends AsyncTask<String, Integer, Integer>
		{
			@Override
			protected void onPreExecute() {
				//main.progressBar.setVisibility(View.VISIBLE);
			}

			@Override
			protected Integer doInBackground(String... params) {
			
				Log.d("AsyncRetrieveAlbum", ""+params);
				Looper.prepare();   
				Log.e("info AsyncRetrieveAlbum ", "start fetching.....");
				// get picasa album list
				AlbumList list = new AlbumList(getOuterClass().getAuth(), email);
				getOuterClass().setList(list);
				
				// check if any albums were found
				if (list.fetchAlbumList() == false) {
					getOuterClass().setIsNoAlbum(true);
				} else {
					getOuterClass().setIsNoAlbum(false);
				}
				
				Log.e("info AsyncRetrieveAlbum ", "auth value : " + getOuterClass().getAuth());
				getOuterClass().setIsAlbumRetrieveDone(true);
				Looper.loop();
				
				return null;
			}

			@Override
			protected void onPostExecute(Integer result) {
				//getOuterClass().setIsDone(true);
			}  
		}
		
		final Thread workerAuth = new Thread(new Runnable(){
            @Override
            public void run()
            {
            	Log.d("retrieveAUTH", "Thread run() : started");
            	GoogleAuthentication google = new GoogleAuthentication(email, password, getContext());
				String auth                 = google.getAuthenticationString();
				getOuterClass().setAuth(auth);
				Log.e("info Asyctast ", "auth value : " + auth);
				getOuterClass().setIsDone(true);
				Log.d("retrieveAUTH", "Thread run() : finished");
            }

        });
        workerAuth.start();

		while (this.getIsDone() == false){
			try {
				Thread.sleep(2000);
				if (this.getIsDone() == true) {
					// if authentication string is null it means we failed authentication
					if (auth == null) {
						Utils.textDialog(getContext(), "Notification", "Google authentication failed.\n\nCheck your e-mail and password.");
						return;
					}

					// retrieve album in a background process
					//new AsyncRetrieveAlbum().execute("test");
					final Thread worker = new Thread(new Runnable(){
			            @Override
			            public void run()
			            {
			                Log.d("retrievealbum", "Thread run() : started");
			                AlbumList list = new AlbumList(getOuterClass().getAuth(), email);
							getOuterClass().setList(list);
							
							// check if any albums were found
							if (list.fetchAlbumList() == false) {
								getOuterClass().setIsNoAlbum(true);
							} else {
								getOuterClass().setIsNoAlbum(false);
							}
							
							Log.e("info AsyncRetrieveAlbum ", "auth value : " + getOuterClass().getAuth());
							getOuterClass().setIsAlbumRetrieveDone(true);
							Log.d("retrievealbum", "Thread run() :  finished");
			            }

			        });
			        worker.start();
					
					while (this.getIsAlbumRetrieveDone() == false){
						
						Thread.sleep(2000);
						if (this.getIsAlbumRetrieveDone() == true) {
							
							if (this.getIsNoAlbum()) {
								Utils.textDialog(getContext(), "Notification", "No Picasa albums found. Create one first.");
								return;
							}
								
							// user can choose
							setEntries(list.getAlbumNames());
							setEntryValues(list.getAlbumIds());

							// call parent function to show preference dialog
							showDialog(null);
							this.setIsAlbumRetrieveDone(false);
							break;
						}
						Log.e("info AsyncRetrieveAlbum ", "looping...");
					} 
					//asycnAuth.cancel(true);
					this.setIsDone(false);
					break;
				}
				Log.e("info AsyncAUTH ", "looping...");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 

		
	}
}

