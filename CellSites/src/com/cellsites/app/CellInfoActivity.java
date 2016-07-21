package com.cellsites.app;


import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.TextView;

public class CellInfoActivity extends Activity {

	TextView devinfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cell_info);
		devinfo = (TextView)findViewById(R.id.devinfo);
		Intent i = this.getIntent();
		try{
			devinfo.setText(i.getStringExtra("cells_info"));
		}catch(NullPointerException e){}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.cell_info, menu);
		return true;
	}

}
