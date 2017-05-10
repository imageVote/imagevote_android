/*package at.imagevote;

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

//https://firebase.google.com/docs/app-indexing/android/activity?hl=es-419
public class GoogleIndex {

    private Context ctx;
    private GoogleApiClient mClient;
    private Uri mUrl;
    private String mTitle;
    private String mDescription;

    public GoogleIndex(Context context){
        ctx = context;

        mClient = new GoogleApiClient.Builder(ctx).addApi(AppIndex.API).build();
        mUrl = Uri.parse("http://examplepetstore.com/dogs/standard-poodle");
        mTitle = "Standard Poodle";
        mDescription = "The Standard Poodle stands at least 18 inches at the withers";
    }

    public Action getAction() {
        Thing object = new Thing.Builder()
                .setName(mTitle)
                .setDescription(mDescription)
                .setUrl(mUrl)
                .build();

        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    public void start(){
        mClient.connect();
        AppIndex.AppIndexApi.start(mClient, getAction());
    }
    public void stop(){
        mClient.disconnect();
    }
}
*/