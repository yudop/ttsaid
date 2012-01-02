package com.ttsaid;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class SelectLanguage extends ListActivity {
	private int selected = 0;
	private ArrayList<ListItem> list = new ArrayList<ListItem>();

	/* list activity creation */
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		final Intent intent = getIntent();
		MyArrayAdapter currentAdapter;

		/* get current folder */
		String [] listItems = intent.getStringArrayExtra("list_items");

		Toast.makeText(this,"listItems" + ((listItems != null) ? listItems.length : 0), 1).show();		
		
		if(true)return;
		
		list.clear();
		for(int x=0;x < listItems.length;x++) {
			ListItem item = new ListItem(listItems[x],"","");
			list.add(item);
		}
		currentAdapter = new MyArrayAdapter(this,R.layout.rowlayout,list);

		/* put items on list */
		setListAdapter(currentAdapter);

		/* set current selected item */
		SelectLanguage.this.setSelection(selected);

		/* get list view to override long click event */
		ListView list = getListView();
		
		/* list item long click (select folder) */
		list.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> av, View v,int position, long id) {
				ListItem item = (ListItem) getListAdapter().getItem(position);

				setResult(RESULT_OK,intent);
				intent.putExtra("selected",item.listName);
				finish();
				return false;
			}
		});
	}
	
	/* list item click (go into folder tree) */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ListItem	item = (ListItem) getListAdapter().getItem(selected);
		if(item.bt != null) {
			item.bt.setChecked(false);
		}
		item = (ListItem) getListAdapter().getItem(position);
		if(item.bt != null) {
			item.bt.setChecked(true);
		}
	}
	
	private class ListItem {
        private String	listName;
        private String	listLanguage;
        private String	listCountry;
        public	RadioButton bt;
        
        /* constructor */
        public ListItem(String name,String language,String country) {
            listName = name;
            listLanguage = language;
            listCountry = country;
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
	
	public class MyArrayAdapter extends ArrayAdapter<ListItem> {
        private int resource;
        private LayoutInflater vi;

        /* constructor */
        public MyArrayAdapter(Context context, int _resource, List<ListItem> listitems) {
            super(context, _resource, listitems);
            vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            resource = _resource;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout newView;
            
            /* get item for the given list position */
            ListItem item = getItem(position);

            /* if the list item is not yet created, create it */
            if (convertView == null) {
                newView = new LinearLayout(getContext());
                vi.inflate(resource, newView);
            } else {
                newView = (LinearLayout)convertView;
            }
            /* set list item text */
            item.bt = (RadioButton) newView.findViewById(R.id.langButton);
            item.bt.setText(item.toString());
            item.bt.setChecked((position == selected));
            
            return newView;
        }		
	}
}
