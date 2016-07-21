package com.cellsites.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.support.v4.app.FragmentActivity;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

//	static Context context;
	TelephonyManager tm;// = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
	LocationManager lm;
	LocationListener locationListener;
	int sigstr = 0;//phone signal strength
	List<String> locprders;
	String subid,dev_info;
	TextView devinfo;
	int update_freq = 2,//minutes
		update_cnt = 0,//update count not really useful
		update_dist = 0;//meters
	List<Integer> acells =  new ArrayList<Integer>();
	List<Integer> cellsrsi =  new ArrayList<Integer>();
	List<Marker> markers = null;
	List<Circle> circles = null;
	Object[] rcells ;
	Object[] rsi;
	int CircleColor[];
	public GoogleMap map;
	Marker pmarker = null;//marker for phone provided location
	Circle pcircle = null;//circle for phone provided location
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try{
			setContentView(R.layout.activity_main);
			CircleColor = new int[] {Color.BLUE,Color.BLACK,Color.MAGENTA,Color.RED,Color.YELLOW,
					Color.DKGRAY,Color.CYAN,Color.GRAY,Color.LTGRAY,Color.GREEN,Color.WHITE,Color.TRANSPARENT
			};
		    map = ((com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
		            .getMap();
			tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
			subid= "Subscriber ID: "+ tm.getSubscriberId()+"\n Software version: "+
					tm.getDeviceSoftwareVersion()+"\n Line 1 number: "+
					tm.getLine1Number()+"\n Network operator: "+
					tm.getNetworkOperator()+"\n Network operator name: "+
					tm.getNetworkOperatorName()+"\n Network Country ISO: "+
					tm.getNetworkCountryIso()+"\n Sim operator: "+
					tm.getSimOperator()+"\n Sim operator name: "+
					tm.getSimOperatorName()+"\n Sim country ISO: "+
					tm.getSimCountryIso()+"\n Sim serial number: "+
					tm.getSimSerialNumber();
			List<String> locprders=lm.getAllProviders();
			int llen=locprders.size();
			for(int i =0;i<llen;i++)
				subid+="\nLocation Provider "+Integer.toString(i)+": "+locprders.get(i);
			dev_info=subid;
			init_state();
			//insert markers and circles
			map.clear();
			if(pmarker!=null){
				pmarker = map.addMarker(new MarkerOptions().position(pmarker.getPosition())
					.title(pmarker.getTitle()).snippet(pmarker.getSnippet()));
				pmarker.showInfoWindow();
			}
			try{
			if(pcircle!=null)
				pcircle = map.addCircle(new CircleOptions().center(pcircle.getCenter())
					.radius(pcircle.getRadius())
					.strokeColor(Color.GREEN)
					.strokeWidth(pcircle.getStrokeWidth())
					);
			}catch(IllegalArgumentException e){
				
			}
			if(markers!=null)
				for(int i=0;i<markers.size();i++){
					Marker marker = map.addMarker(new MarkerOptions().position(markers.get(i).getPosition())
							.title(markers.get(i).getTitle()).snippet(markers.get(i).getSnippet()));
					marker.showInfoWindow();
					markers.set(i, marker);
				}
			else markers = new LinkedList<Marker>();
			if(circles!=null)
				for(int i=0;i<markers.size();i++){
					try{
					Circle circle = map.addCircle(new CircleOptions().center(circles.get(i).getCenter())
							.radius(circles.get(i).getRadius())
							.strokeColor(circles.get(i).getStrokeColor())
							.strokeWidth(2));
					circles.set(i, circle);
				}catch(IllegalArgumentException e){
					
				}
			}
			else circles = new LinkedList<Circle>();
			
		}
		catch(NullPointerException e){}
		catch(NoClassDefFoundError e){
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
//			finish();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void init_state(){
		// Define a listener that responds to location updates
		locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		      // Called when a new location is found by the network location provider.
		    	float accuracy = location.getAccuracy();
		    	if(pmarker!=null)
		    		pmarker.remove();
		    	pmarker  = map.addMarker(new MarkerOptions()
		    		.position(new LatLng(location.getLatitude(),location.getLongitude()))
		    			.snippet("Estimated location").title("Me"));
		    	if(pcircle!=null)
		    		pcircle.remove();
		    	pcircle = map.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(),location.getLongitude()))
						.radius(accuracy)
						.strokeColor(Color.GREEN)
						.strokeWidth(5)
						);

		    	if(LocationManager.GPS_PROVIDER.compareTo(location.getProvider())==0){
		    		//execute gps location change
		    	}
		    	else if(LocationManager.NETWORK_PROVIDER.compareTo(location.getProvider())==0){
					int cid=0,lac=0,sid=0,nid=0,bid=0;String cellinfo="\nCell info not available";
					acells.clear();cellsrsi.clear();
		    		//execute network location change
		    		//check network type
		    		switch(tm.getPhoneType()){
		    		//handle gsm
		    		case TelephonyManager.PHONE_TYPE_GSM:
						GsmCellLocation gsmloc = (GsmCellLocation)tm.getCellLocation();
						cid = gsmloc.getCid();
						lac = gsmloc.getLac();
						acells.add(cid);
						acells.add(lac);
						cellsrsi.add(sigstr);
						cellinfo ="\n\tSignal Strength: "+Integer.toString(sigstr)
								+"\n\tCID: "+Integer.toString(cid)
								+"\n\tLAC: "+Integer.toString(lac);
	    			break;
		    		//handle cdma
		    		case TelephonyManager.PHONE_TYPE_CDMA:
						CdmaCellLocation cdmaloc = (CdmaCellLocation)tm.getCellLocation();
						bid=cdmaloc.getBaseStationId();
						sid=cdmaloc.getSystemId();
						nid=cdmaloc.getNetworkId();
						cellinfo ="\n\tSignal Strength: "+Integer.toString(sigstr)
								+"\n\tBID: "+Integer.toString(bid)
								+"\n\tNID: "+Integer.toString(nid)
								+"\n\tSID: "+Integer.toString(sid);
		    			break;
		    		//handle unknown network types
		    		default:
		    				break;
		    		}
					List<NeighboringCellInfo> nbcells;
					nbcells = tm.getNeighboringCellInfo();
					int cells=nbcells.size();
					cellinfo+="\nNeigbouring cells "+Integer.toString(cells);
					for(int i=0;i<cells;i++){
						NeighboringCellInfo nbcell = nbcells.get(i);
						int rssi = nbcell.getRssi();
						//ignore cell if rssi is not valid
						if(rssi<0 || rssi>31)
							continue;
						cellinfo+="\nCell"+Integer.toString(i+1);
						cid = nbcell.getCid();
						lac = nbcell.getLac();
						cellinfo+="\n\tSignal Strength: "+Integer.toString(rssi)
								+"\n\tCell ID: "+Integer.toString(cid)
								+"\n\tCell LAC: "+Integer.toString(lac);
						acells.add(cid);
						acells.add(lac);
						cellsrsi.add(rssi);
					}
					dev_info=subid+cellinfo;
					//fire update changes
					onReqLoc();
		    	}
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {
		    	//if unavailable set status to disable
		    }

		    public void onProviderEnabled(String provider) {
		    	//set provider to enable
		    }

		    public void onProviderDisabled(String provider) {
		    	//set provider to disable
		    }
		  };
		  
		tm.listen(new PhoneStateListener(){
			
			@Override
			public void onSignalStrengthsChanged (SignalStrength sigstrength){
				try{
				if(sigstrength.isGsm())
				{
					sigstr=sigstrength.getGsmSignalStrength();
					
				}
				else
				{
					sigstr=sigstrength.getCdmaDbm();
				}
				}catch(NullPointerException ex){
					
				}
			}
		},PhoneStateListener.LISTEN_SERVICE_STATE|PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
	}
	public void onReqLoc(){
//		setTitle("Retrieving location ...");
		rcells=acells.toArray();
		rsi=cellsrsi.toArray();
		BgTask btask = new BgTask();
		btask.execute();
		
	}

	public class BgTask extends AsyncTask<Void, Void, List<Integer>> {

		@Override
		protected List<Integer> doInBackground(Void... params) {
			int numcells=rcells.length;
			if(numcells<1)return null;
			List<Integer> gpsloc=new LinkedList<Integer>();
			for(int i=0;i<numcells;i++){
				Integer [] cellpos=RqsLocation((Integer)rcells[(i++)],(Integer)rcells[(i)]);
				if(cellpos!=null){
					gpsloc.add(cellpos[0]);
					gpsloc.add(cellpos[1]);
				}
			}
			return gpsloc;
		}
		
/*		@Override
		public void onPreExecute(){
		}
*/		@Override
		public void onPostExecute(List<Integer> results){
			if(results==null){
				MainActivity.this.setTitle(getString(R.string.app_name) +" - Updates failed");
				return;
			}
			int numcells=results.size();
			int kcnt;
			if((numcells/2)>rsi.length)
				kcnt=rsi.length;
			else
				kcnt=numcells/2;
			String retpos=getString(R.string.app_name) +" - "+Integer.toString(kcnt)+"cells Located ";
			double lats[]=new double[numcells/2];
			double lngs[]=new double[numcells/2];
			for(int i=0,k=0;i<numcells;i++,k++){
				lats[k]=((double)(results.get(i++))/1000000.000);
				lngs[k]=((double)(results.get(i))/1000000.000);
			}
			//calculate summation of signal stregth
			//calculate circle
			float radi[]= getLocationRadius(lats,lngs);
			map.clear();circles.clear();markers.clear();
			//restore location given by api
			if(pmarker!=null){
				pmarker = map.addMarker(new MarkerOptions().position(pmarker.getPosition())
					.title(pmarker.getTitle()).snippet(pmarker.getSnippet()));
				pmarker.showInfoWindow();
			}
			if(pcircle!=null)
				try{
				pcircle = map.addCircle(new CircleOptions().center(pcircle.getCenter())
					.radius(pcircle.getRadius())
					.strokeColor(Color.GREEN)
					.strokeWidth(pcircle.getStrokeWidth())
					);
				}catch(IllegalArgumentException e){
					
				}
			//draw circles
			for(int i=0;i<lats.length;i++){
				LatLng latlng = new LatLng(lats[i],lngs[i]);
				try{
				circles.add(map.addCircle(new CircleOptions().center(latlng)
						.radius(radi[i])
						.strokeColor(CircleColor[i])
						.strokeWidth(2))
						);
				}catch(IllegalArgumentException e){
					
				}
				markers.add( map.addMarker(new MarkerOptions().position(latlng)
					.title("CID: "+Integer.toString((Integer)rcells[(i*2)])).snippet("RSSI: "+Integer.toString((Integer)rsi[i]))));
			if(i==0)map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 13));
		}
			MainActivity.this.setTitle(retpos);
}
	
	}
	
	public float[] getLocationRadius(double []lats,double[]lngs){
		//calculate max distance between max and min rsi cells
		if(lats.length<1 || lngs.length<1)
			return null;
		float []radi=new float[lats.length];
		int cell1=0,cell2=0;
		float [][]distances = new float[lats.length][lats.length];
		float tempdist[]=new float[3],maxdist=0;
		//calculate distances between towers
		for(int i=0;i<lats.length;i++)
			for(int k=0;k<lats.length;k++){
				if(i==k){
					distances[i][k]=0;
					continue;//eliminating same tower 
				}
				Location.distanceBetween(lats[i], lngs[i], lats[k], lngs[k], tempdist);
				distances[i][k]=tempdist[0];
				if(tempdist[0]>maxdist){
					maxdist=tempdist[0];
					cell1=i;cell2=k;
				}
			}
		//ensure cell 1 has a higher rsi
		if((Integer)(rsi[cell2])>(Integer)(rsi[cell1])){
			int midcell = cell1;
			cell1 = cell2;
			cell2 = midcell;
		}
		//calculate initial radi for all positions
		int rsimax = (Integer)rsi[cell1]+(Integer)rsi[cell2];
		for(int i=0;i<lats.length;i++)
			radi[i]=(maxdist*((float)(rsimax - (Integer)rsi[i]))/rsimax)+5;
		//ensure all radi touch each other
		for(int i=0;i<lats.length;i++)
			for(int k=0;k<lats.length;k++){
				if(i==k)continue;
				float celldist = radi[i]+radi[k];
				if(celldist<distances[i][k]){
					//trouble increase radi for them to be the equal
					float mratio = distances[i][k]/celldist;
					for(int j=0;j<lats.length;j++)
						radi[i]*=mratio;
				}
			}
		//now all circles are touching
		return radi;
	}
	 private Integer[] RqsLocation(Integer cid, Integer lac){
		  
//		   Boolean result = false;
		 if(cid<1||lac<1)
			 return null;
		   String urlmmap = "http://www.google.com/glm/mmap";

		      try {
		       URL url = new URL(urlmmap);
		          URLConnection conn = url.openConnection();
		          HttpURLConnection httpConn = (HttpURLConnection) conn;     
		          httpConn.setRequestMethod("POST");
		          httpConn.setDoOutput(true);
		          httpConn.setDoInput(true);
		  httpConn.connect();
		 
		  OutputStream outputStream = httpConn.getOutputStream();
		        WriteData(outputStream, cid, lac);
		      
		        InputStream inputStream = httpConn.getInputStream();
		        DataInputStream dataInputStream = new DataInputStream(inputStream);
		      
		        dataInputStream.readShort();
		        dataInputStream.readByte();
		        int code = dataInputStream.readInt();
		        Integer result[]= new Integer[2];
		        if (code == 0) {
		         result[0] = dataInputStream.readInt();
		         result[1] = dataInputStream.readInt();
		          dataInputStream.close();
		            return result;
		          
		        }
		 } catch (IOException e) {
		  // TODO Auto-generated catch block
		  e.printStackTrace();
		  return null;
		 }
			return null;

	  }

		  private void WriteData(OutputStream out, int cid, int lac)
		  throws IOException
		  {   
		      DataOutputStream dataOutputStream = new DataOutputStream(out);
		      dataOutputStream.writeShort(21);
		      dataOutputStream.writeLong(0);
		      dataOutputStream.writeUTF("en");
		      dataOutputStream.writeUTF("Android");
		      dataOutputStream.writeUTF("1.0");
		      dataOutputStream.writeUTF("Web");
		      dataOutputStream.writeByte(27);
		      dataOutputStream.writeInt(0);
		      dataOutputStream.writeInt(0);
		      dataOutputStream.writeInt(3);
		      dataOutputStream.writeUTF("");

		      dataOutputStream.writeInt(cid);
		      dataOutputStream.writeInt(lac);  

		      dataOutputStream.writeInt(0);
		      dataOutputStream.writeInt(0);
		      dataOutputStream.writeInt(0);
		      dataOutputStream.writeInt(0);
		      dataOutputStream.flush();  
		      dataOutputStream.close();
		  }

			static void displayHelp(Context context,Activity act) {
				String current_version_name;
				android.content.pm.PackageInfo packageInfo;
				try {
					packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
					current_version_name = packageInfo.versionName;
				} catch (NameNotFoundException e) {
					current_version_name = "1.0";

				}
				AlertDialogManager.showAlertDialog(context,act,
						context.getString(R.string.app_name) + " Version "
								+ current_version_name,
						"visit cellsites.kofigyima.com for more details\n\n"
								+ "All rights reserved\n"
								+ "Copyright ©2014 Kofi Gyima", null, null, "Close",
						R.drawable.cellsites);
			}


			@Override
			public boolean onOptionsItemSelected(MenuItem item) {
				// Handle item selection
				switch (item.getItemId()) {
				case R.id.action_cells:
					Intent i = new Intent(this,CellInfoActivity.class);
					i.putExtra("cells_info",dev_info);
					startActivity(i);
					return true;
				case R.id.action_start_trace:
					try{
						lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, update_freq*60*1000, update_dist, locationListener);
					}catch(IllegalArgumentException	ex){
						ex.printStackTrace();
					}catch(RuntimeException	ex){
						ex.printStackTrace();
					}
					return true;
				case R.id.action_stop_trace:
					try{
					lm.removeUpdates(locationListener);
					}catch(IllegalArgumentException	ex){
						ex.printStackTrace();
					}
					return true;
				case R.id.action_help:
					displayHelp(this,this);
					return true;
				case R.id.action_exit:
					System.runFinalization(); System.exit(0);
					return true;
				default:
					return super.onOptionsItemSelected(item);
				}
			}


}
