package com.ttsaid;

import java.util.ArrayList;
import java.util.Comparator;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectLanguage extends ListActivity {
	private ArrayList<ListItem> list = new ArrayList<ListItem>();
	Intent intent;

	/* list activity creation */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		intent = getIntent();

		/* get current folder */
		String [] listItems = intent.getStringArrayExtra("list_items");
		list.clear();
		for(int x=0;x < listItems.length;x++) {
			ListItem item = new ListItem(listItems[x]);
			list.add(item);
		}

		/* create adapter */

		ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this,R.layout.rowlayout,list);
		adapter.sort(new Comparator<ListItem>() {
			public int compare(ListItem a,ListItem b) {
				return(a.listName.compareTo(b.listName));
			}
		});		

		/* put items on list */
		setListAdapter(adapter);

		/* set title */
		setTitle(getString(R.string.selectLanguage));
		
		/* get list view */
		ListView listview = getListView();
		
		listview.setOnItemLongClickListener(new OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,int posit, long id) {
				ListItem item = (ListItem) getListAdapter().getItem(posit);

				setResult(RESULT_OK,intent);
				intent.putExtra("selected",item.getLangCode());
				finish();
				return false;
			}
		});
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
	}	

	private class ListItem {
        private String	listName="";
        private String	listLanguage="";
        private String	listCountry="";
        
        /* constructor */
        public ListItem(String str) {
			String [] l = str.split(",");

			if(l.length > 2) {
				listCountry=l[2];
			}
			if(l.length > 1) {
				listLanguage=l[1];
			}
			if(l.length > 0) {
				listName=l[0];
			}			
        }

        public String getLangCode()
        {
        	String str = listLanguage;
        	if(listCountry.length() > 0) {
        		str+="_"+listCountry;
        	}
        	return(str);
        }
        
        @Override
        public String toString() {
            String str = listName + " (" + listLanguage;
            if(listCountry.length() > 0) {
            	str += "_" + listCountry;
            }
            return(str+=")");
        }
        
        
	}
	
}
