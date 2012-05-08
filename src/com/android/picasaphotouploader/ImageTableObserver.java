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

import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;

import com.syplod.SyplodActivity;

/**
 * Class to observe changes to images table
 * 
 * @author Jan Peter Hooiveld
 */
public class ImageTableObserver extends ContentObserver
{
	/**
	 * Main application
	 */
	private SyplodActivity application;

	/**
	 *  Queue that handles image uploads
	 */
	private ExecutorService queue;

	private Boolean isDone = false;
	private Boolean isCheckAlbumDone = false;
	private String auth;
	private Boolean isAlbumExist = false;

	public String getAuth() {
		return auth;
	}

	public void setAuth(String auth) {
		this.auth = auth;
	}

	public Boolean getIsAlbumExist() {
		return isAlbumExist;
	}

	public void setIsAlbumExist(Boolean isAlbumExist) {
		this.isAlbumExist = isAlbumExist;
	}

	public Boolean getIsCheckAlbumDone() {
		return isCheckAlbumDone;
	}

	public void setIsCheckAlbumDone(Boolean isCheckAlbumDone) {
		this.isCheckAlbumDone = isCheckAlbumDone;
	}

	public Boolean getIsDone() {
		return isDone;
	}

	public void setIsDone(Boolean isDone) {
		this.isDone = isDone;
	}

	public final ImageTableObserver getOuterClass(){
		return this;
	}

	/**
	 * Constructor
	 * 
	 * @param handler Handler for this class
	 * @param picasaPhotoUploadActivity Main application
	 * @param queue Queue that handles image uploads
	 */
	public ImageTableObserver(Handler handler, SyplodActivity picasaPhotoUploadActivity, ExecutorService queue)
	{
		super(handler);

		this.application = picasaPhotoUploadActivity;
		this.queue       = queue;
	}

	/**
	 * This function is fired when a change occurs on the image table
	 *
	 * @param selfChange
	 */
	@Override
	public void onChange(boolean selfChange)
	{
		// get latest image id
		ImageLatest latestImage = new ImageLatest(application);
		int imageId             = latestImage.getId();

		// if id is -1 it means no record was found or it was a update/delete instead of insert
		if (imageId == -1) {
			return;
		}

		// get image item
		ImageItem item  = latestImage.getLatestItem();

		// if no image item returned abort
		if (item == null) {
			return;
		}

		// get user preferences
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(application.getBaseContext());

		// check if we can connect to internet
		if (!CheckInternet.getInstance().canConnect(application.getBaseContext(), prefs)) {
			return;
		}

		// check if album is set in preferences
		if (prefs.getString("album", "").trim().length() == 0) {
			return;
		}
		Thread workerAuth = new Thread(new Runnable(){
			@Override
			public void run()
			{
				Log.d("ImageTableObserver - retrieveAUTH", "Thread run() : started");
				String auth = getAuthentication(prefs);
				getOuterClass().setAuth(auth);
				Log.e("info ImageTableObserver ", "auth value : " + auth);
				getOuterClass().setIsDone(true);
				Log.d("ImageTableObserver - retrieveAUTH", "Thread run() : finished");
			}

		});
		workerAuth.start();
		

		while (this.getIsDone() == false){
			try {
				Thread.sleep(2000);
				if (this.getIsDone() == true) {
					
					// check if auth is null
					if (this.getAuth() == null) {
						return;
					}
					
					Thread workerCheckAlbum = new Thread(new Runnable(){
						@Override
						public void run()
						{
							Log.d("ImageTableObserver - workerCheckAlbum", "Thread run() : started");
							Boolean isExist = albumExists(getOuterClass().getAuth(), prefs);
							getOuterClass().setIsAlbumExist(isExist);
							getOuterClass().setIsCheckAlbumDone(true);
							Log.d("ImageTableObserver - workerCheckAlbum", "Thread run() : finished");
						}

					});
					workerCheckAlbum.start();

					while (this.getIsCheckAlbumDone() == false){
						Thread.sleep(2000);
						if (this.getIsCheckAlbumDone() == true) {
							// check if albums from preferences does not exist
							if (!this.getIsAlbumExist()) {
								Log.w("ImageTableObserver - workerCheckAlbum", "album not exist...");
								this.setIsCheckAlbumDone(false);
								return;
							}

							// add auth to image item and then add it to queue
							item.imageAuth = this.getAuth();

							// collect id of notification
							application.addId(item.imageId);
							
							// add item to queue
							queue.execute(new ImageUploader(application.getBaseContext(), queue, item, 0));
							this.setIsCheckAlbumDone(false);
							break;
						}
						Log.d("ImageTableObserver - workerCheckAlbum", "looping...");
					}
					this.setIsDone(false);
					break;
				}
				Log.d("ImageTableObserver - retrieveAUTH", "looping...");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// check if authentication succeeded
	}

	/**
	 * Get authentication string from Google
	 *
	 * @param prefs User preferences
	 * @return Google authentication string
	 */
	private String getAuthentication(SharedPreferences prefs)
	{
		return new GoogleAuthentication(prefs, application.getBaseContext()).getAuthenticationString();
	}

	/**
	 * Check if Picasa album stored in user preferences exists
	 *
	 * @param auth Google authentication string
	 * @param prefs User preferences
	 * @return If album exists or not
	 */
	private boolean albumExists(String auth, SharedPreferences prefs)
	{
		return new AlbumExists(auth, prefs.getString("email", ""), prefs.getString("album", "")).hasAlbum();
	}
}
