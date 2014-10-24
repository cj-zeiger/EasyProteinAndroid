package com.zygr.easyprotein.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.LinkedList;

//Azure
import com.microsoft.windowsazure.mobileservices.*;



public class MainActivity extends Activity implements ModifyFoodEntry {

    private HistoryFragment mHistoryFragment;
    private InputFragment mInputFragment;
    private MobileServiceClient mClient;


    private LinkedList<FoodEntry> mHistoryData;

    private final String filename = "easyprotein.dat";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHistoryFragment = (HistoryFragment) getFragmentManager().findFragmentById(R.id.list);
        mInputFragment = (InputFragment) getFragmentManager().findFragmentById(R.id.input);
        mHistoryData = new LinkedList<FoodEntry>();
        try {
            FileInputStream fis = openFileInput(filename);
            ObjectInputStream objectIn = new ObjectInputStream(fis);
            Object object = objectIn.readObject();
            mHistoryData = (LinkedList) object;
            objectIn.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        mHistoryFragment.setData(mHistoryData);


        //Azure Connect
        try{
            mClient = new MobileServiceClient(
                "https://easyprotein.azure-mobile.net/",
                "PTWlFurSwaVDiTzDvZxxCcbXeVzaYh75",
                this);
        }
        catch (MalformedURLException e){
            e.printStackTrace();
        }


    }
    @Override
    protected void onResume(){
        super.onResume();
        //To account for data being loaded from a file.
        dataModified();
    }
    @Override
    protected void onStop(){
        super.onStop();
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            ObjectOutputStream objStream = new ObjectOutputStream(fos);
            objStream.writeObject(mHistoryData);
            objStream.close();
        } catch (Exception e){
            Toast.makeText(this,"Issue Saving Data", Toast.LENGTH_SHORT).show();
            //exception
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void resetFoodEntry() {
        ResetDataDialog dialog = new ResetDataDialog();
        dialog.show(getFragmentManager(),null);
    }
    @Override
    public void confirmReset() {
        mHistoryData.clear();
        mHistoryFragment.updateData();
        mInputFragment.updateDisplay(0,0);
    }
    @Override
    public void addFoodEntry(int newCalorie, int newProtein) {
        mHistoryData.push(new FoodEntry(newCalorie, newProtein, new Date()));
        dataModified();
        mInputFragment.mInputCal.setText("");
        mInputFragment.mInputPro.setText("");
    }
    @Override
    public void dataModified(){
        int calorie=0;
        int protein=0;
        for(FoodEntry entry:mHistoryData){
            calorie+=entry.getCalorie();
            protein+=entry.getProtein();
        }

        //update calls to fragments
        mInputFragment.updateDisplay(calorie,protein);
        mHistoryFragment.updateData();
    }

    public static class ResetDataDialog extends DialogFragment{
        private ModifyFoodEntry mListener;
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mListener = (ModifyFoodEntry)getActivity();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure you want to erase your entries?")
                    .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.confirmReset();
                        }
                    })
                    .setNegativeButton("No",null);
            return builder.create();
        }
    }
    public static class InputFragment extends Fragment {

        private Button mAddButton;
        private Button mResetButton;

        public EditText mInputCal;
        public EditText mInputPro;

        private int mCal;
        private int mPro;

        private TextView mTextCal;
        private TextView mTextPro;

        private final String SAVE_FILE = "saved_history";

        public ModifyFoodEntry mListener;

        public InputFragment() {
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_input, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {

            super.onActivityCreated(savedInstanceState);
            mAddButton = (Button) getActivity().findViewById(R.id.button_add);
            mResetButton = (Button) getActivity().findViewById(R.id.button_reset);

            mAddButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addButton();
                }
            });
            mResetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    resetButton();
                }
            });

            mInputCal = (EditText) getActivity().findViewById(R.id.editText_calorie);
            mInputPro = (EditText) getActivity().findViewById(R.id.editText_protein);

            mTextCal = (TextView) getActivity().findViewById(R.id.text_calvar);
            mTextPro = (TextView) getActivity().findViewById(R.id.text_provar);

            mListener = (ModifyFoodEntry) getActivity();

        }

        private void addButton(){
            if(validInput()){
                mListener.addFoodEntry(mCal,mPro);
            } else {
                Toast.makeText(getActivity(),"Invalid amounts",Toast.LENGTH_SHORT).show();
            }
        }
        private void resetButton(){
            mListener.resetFoodEntry();

        }
        public void updateDisplay(int calorie, int protein){
            mTextCal.setText(""+calorie);
            mTextPro.setText(protein+"g");
        }

        /**
         * Sets mCal and mPro to correct value if they are valid inputs
         * @return true if two valid inputs are found
         */
        private boolean validInput(){
            try{
                String sCal = mInputCal.getText().toString();
                String sPro = mInputPro.getText().toString();
                int cal;
                int pro;
                if(sCal.isEmpty()){
                    cal = 0;
                } else {
                    double tmp = Double.parseDouble(mInputCal.getText().toString());
                    cal = (int)Math.round(tmp);
                }
                if(sPro.isEmpty()){
                    pro = 0;
                } else {
                    double tmp = Double.parseDouble(mInputPro.getText().toString());
                    pro = (int)Math.round(tmp);
                }
                if(cal==pro&&cal==0){
                    return false;
                }
                mCal = cal;
                mPro = pro;
                return true;
            } catch(NumberFormatException nfe){
                return false;
            }
        }
    }
    public static class HistoryFragment extends ListFragment {
        View mFooterView;
        HistoryAdapter mHistoryAdapter;
        LinkedList<FoodEntry> mHistoryData;
        ModifyFoodEntry mListener;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getListView().addFooterView(mFooterView,null,false);
            if(mHistoryData == null){
                throw new NullPointerException();
            }
            mListener = (ModifyFoodEntry)getActivity();
            mHistoryAdapter = new HistoryAdapter(getActivity(),mHistoryData);
            setListAdapter(mHistoryAdapter);
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
                                 saved){
            View listView = inflater.inflate(R.layout.fragment_customlist, null);
            mFooterView = inflater.inflate(R.layout.listfooter_custom, null);
            return listView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        public void updateData(){
            mHistoryAdapter.notifyDataSetChanged();
        }
        public void setData(LinkedList<FoodEntry> data){
            mHistoryData = data;

        }

        public class HistoryAdapter extends BaseAdapter {
            Activity mActivity;
            private LinkedList<FoodEntry> mItems;


            public HistoryAdapter(Activity c, LinkedList<FoodEntry> items){
                mActivity = c;
                mItems = items;
            }
            @Override
            public int getCount() {
                return mItems.size();
            }

            @Override
            public void notifyDataSetChanged(){
                super.notifyDataSetChanged();

            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v;
                if (convertView != null){
                    v = convertView;
                } else {
                    v = mActivity.getLayoutInflater().inflate(R.layout.listitem_historyitem,parent,false);
                }

                TextView calView = (TextView)v.findViewById(R.id.item_calorie);
                TextView proView = (TextView)v.findViewById(R.id.item_protein);
                Button delButton = (Button) v.findViewById(R.id.button_delete);

                final FoodEntry entry = mItems.get(position);
                delButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (entry != null){
                            mItems.remove(entry);
                        }
                        mListener.dataModified();
                    }
                });
                calView.setText("" + mItems.get(position).getCalorie());
                proView.setText(mItems.get(position).getProtein()+"g");
                return v;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public Object getItem(int position) {
                return mItems.get(position);
            }
        }

    }
}
