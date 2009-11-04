package org.googlecode.vkontakte_android;

import java.util.LinkedList;
import java.util.List;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import org.googlecode.vkontakte_android.MessagesListTabActivity.MessagesCursorType;
import org.googlecode.vkontakte_android.database.UserDao;
import org.googlecode.vkontakte_android.service.CheckingService;

import static org.googlecode.vkontakte_android.provider.UserapiDatabaseHelper.*;
import static org.googlecode.vkontakte_android.provider.UserapiProvider.USERS_URI;

public class FriendsListTabActivity extends AutoLoadActivity implements AdapterView.OnItemClickListener {
    private FriendsListAdapter adapter;
    private static String TAG = "FriendsListTabActivity";
    
    enum MessagesCursorType {
        ALL, NEW, ONLINE
    }

    public static final String SHOW_ONLY_NEW = "SHOW_ONLY_NEW";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.friend_list);
        boolean onlyNew = false;
        Bundle extras = getIntent().getExtras();
        if (extras != null) onlyNew = extras.getBoolean(SHOW_ONLY_NEW);
        Cursor cursor = onlyNew ? makeCursor(MessagesCursorType.NEW) : makeCursor(MessagesCursorType.ALL);
        adapter = new FriendsListAdapter(this, R.layout.friend_row, cursor);
        
        setupLoader(new AutoLoadActivity.Loader(){

			@Override
			public Boolean load() {
				getIdsToUpdate();
				try {
                   ServiceHelper.getService().loadUsersPhotos(getIdsToUpdate());
                } catch (RemoteException e) {
                    e.printStackTrace();
                    AppHelper.showFatalError(FriendsListTabActivity.this, "While trying to load friends photos");
                }
                return false;
			}
        	 
        }, adapter);
        
       registerForContextMenu(getListView());
       getListView().setOnItemClickListener(this);
   }

    private List<String> getIdsToUpdate() {
    	List<String> us = new LinkedList<String>();
    	
    	int f = getListView().getFirstVisiblePosition();
    	int l = getListView().getLastVisiblePosition();
    	int num_to_load = l - f;
    	
    	//load next num_to_load photos
    	for (int i=l; i<=l+num_to_load; ++i) {
    		Cursor c = (Cursor)getListView().getItemAtPosition(i);
    		if (c == null || c.isAfterLast()) {
    			break;
    		}
    		UserDao ud = new UserDao(c);
     		Log.d(TAG, "getIdsToUpdate: "+ud.userName);
    		us.add(String.valueOf(ud.userId));
    	}
    	return us; 
    }
    
    
    private Cursor makeCursor(MessagesCursorType type) {
        switch (type) {
            case NEW:
                return managedQuery(USERS_URI, null, KEY_USER_NEW + "=1", null,
                        KEY_USER_USERID + " ASC," + KEY_USER_NEW + " DESC, " + KEY_USER_ONLINE + " DESC"
                );
            case ONLINE:
                return managedQuery(USERS_URI, null, KEY_USER_ONLINE + "=1", null,
                		KEY_USER_USERID + " ASC," + KEY_USER_NEW + " DESC, " + KEY_USER_ONLINE + " DESC"
                );
            case ALL:
                return managedQuery(USERS_URI, null,
                        KEY_USER_IS_FRIEND + "=?", new String[]{"1"},
                        KEY_USER_USERID + " ASC," + KEY_USER_NEW + " DESC, " + KEY_USER_ONLINE + " DESC"
                );
            default:
                return managedQuery(USERS_URI, null, null, null, null);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.friend_context_menu, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        UserDao user = UserDao.get(this, info.id);
        if (user.isNewFriend()) {
            menu.removeItem(R.id.remove_from_friends);
        } else {
            menu.removeItem(R.id.add_to_friends);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        long rowId = info.id;
        UserDao user = UserDao.get(this, rowId);
        long userId = user.userId;
        switch (item.getItemId()) {
            case R.id.view_profile:
                UserHelper.viewProfile(this, userId);
                return true;
            case R.id.remove_from_friends:
                //todo
                return true;
            case R.id.send_message:
                UserHelper.sendMessage(this, userId);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long rowId) {
        UserDao user = UserDao.get(this, rowId);
        UserHelper.viewProfile(this, user.userId);
    }
}