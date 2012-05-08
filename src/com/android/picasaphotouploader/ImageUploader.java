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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

/**
 * Class to upload image to Picasa
 *
 * @author Jan Peter Hooiveld
 */
public class ImageUploader implements Runnable
{
  /**
   * Application context
   */
  private Context context;

  /**
   *  Queue that handles image uploads
   */
  private ExecutorService queue;

  /**
   * Image queue item
   */
  private ImageItem item;

  /**
   * Number of retries for failed uploads
   */
  private int retries;

  /**
   * Constructor
   *
   * @param context Application context
   * @param queue Queue that handles image uploads
   * @param item Image queue item
   * @param retries Number of retries for failed uploads
   */
  public ImageUploader(Context context, ExecutorService queue, ImageItem item, int retries)
  {
    this.context  = context;
    this.queue    = queue;
    this.item     = item;
    this.retries  = retries;
  }

  /**
   * Upload image to Picasa
   */
  public void run()
  {
    // create items for http client
    UploadNotification notification = new UploadNotification(context, item.imageId, item.imageSize, item.imageName);
    String url                      = "http://picasaweb.google.com/data/feed/api/user/"+item.prefs.getString("email", "")+"/albumid/"+item.prefs.getString("album", "");
    HttpClient client               = new DefaultHttpClient();
    HttpPost post                   = new HttpPost(url);

    try {
      // new file and and entity
      File file            = new File(item.imagePath);
      Multipart multipart  = new Multipart("Media multipart posting", "END_OF_PART");

      // create entity parts
      multipart.addPart("<entry xmlns='http://www.w3.org/2005/Atom'><title>"+item.imageName+"</title><category scheme=\"http://schemas.google.com/g/2005#kind\" term=\"http://schemas.google.com/photos/2007#photo\"/></entry>", "application/atom+xml");
      multipart.addPart(file, item.imageType);

      // create new Multipart entity
      MultipartNotificationEntity entity = new MultipartNotificationEntity(multipart, notification);

      // get http params
      HttpParams params = client.getParams();

      // set protocal and timeout for httpclient
      params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
      params.setParameter(CoreConnectionPNames.SO_TIMEOUT, new Integer(15000));
      params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, new Integer(15000));

      // set body with upload entity
      post.setEntity(entity);

      // set headers
      post.addHeader("Authorization", "GoogleLogin auth="+item.imageAuth);
      post.addHeader("GData-Version", "2");
      post.addHeader("MIME-version", "1.0");

      // execute upload to picasa and get response and status
      HttpResponse response = client.execute(post);
      StatusLine line       = response.getStatusLine();

      // return code indicates upload failed so throw exception
      if (line.getStatusCode() > 201) {
        throw new Exception("Failed upload");
      }

      // shut down connection
      client.getConnectionManager().shutdown();

      // notify user that file has been uploaded
      notification.finished();
    } catch (Exception e) {
      // file upload failed so abort post and close connection
      post.abort();
      client.getConnectionManager().shutdown();

      // get user preferences and number of retries for failed upload
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      int maxRetries          = Integer.valueOf(prefs.getString("retries", "").substring(1));

      // check if we can connect to internet and if we still have any tries left
      // to try upload again
      if (CheckInternet.getInstance().canConnect(context, prefs) && retries < maxRetries) {
        // remove notification for failed upload and queue item again
        notification.remove();
        queue.execute(new ImageUploader(context, queue, item, retries++));
      } else {
        // upload failed, so let's notify user
        notification.failed();
      }
    }
  }
}
