package com.keysight.transactionrecords;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.script.model.ExecutionRequest;
import com.google.api.services.script.model.Operation;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    static GoogleAccountCredential accountCredential;
    ProgressDialog progressDialog;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES =
    {
            "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/script.external_request",
            "https://www.googleapis.com/auth/documents" //2018-12-04: added.
    };

    private static List<String> listof_AccountName = null;
    private static final String account_Rws_Invites = "RWS Invites (LIU YULEI)";
    private static final String account_Frasers_Rewards = "FRASERS Rewards";
    private static final String account_Kopitiam = "Kopitiam";

    private static final String scriptId_MyBank = "MoNdSxfXDH8wP_ODK4qZ9IBU9l98eQNnp";

    public static final String transactionAccount = "Transaction Account";
    public static final String transactionDate = "Transaction Date";
    public static final String transactionAmount = "Transaction Amount";
    public static final String transactionRemark = "Transaction Remark";
    public static final String accountBalance = "Account Balance";
    public static final String debitCredit = "Debit or Credit";

    private static AutoCompleteTextView editAccount = null;
    private static EditText editDate = null;
    private static EditText editAmount = null;
    private static AutoCompleteTextView editRemark = null;
    private static RadioButton chooseDebit = null;
    private static RadioButton chooseCredit = null;

    private static List<String> remarkList = null;

    private static int initJobCount = 0;
    private static int initJobTotal = 0;

    private AccountViewModel mAccountViewModel;
    private RemarkViewModel mRemarkViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        progressDialog = new ProgressDialog(this);

        editAccount = findViewById(R.id.editAccount);
        editDate = findViewById(R.id.editDate);
        editAmount = findViewById(R.id.editAmount);
        editRemark = findViewById(R.id.editRemark);
        chooseDebit = findViewById(R.id.radioButtonDebit);
        chooseCredit = findViewById(R.id.radioButtonCredit);

        editAccount.setOnDismissListener(new AutoCompleteTextView.OnDismissListener()
        {
            @Override
            public void onDismiss()
            {
                switch (editAccount.getText().toString())
                {
                    case account_Rws_Invites:
                    case account_Frasers_Rewards:
                        chooseDebit.setText("Spend");
                        chooseCredit.setText("Redeem");
                        break;
                    case account_Kopitiam:
                        chooseDebit.setText("Sub Total");
                        chooseCredit.setText("Top Up");
                        break;
                    default:
                        chooseDebit.setText("Debit");
                        chooseCredit.setText("Credit");
                        break;
                }
            }
        });

        editDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime()));
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // get current selected year, month and day.
                String currentDate = editDate.getText().toString();
                int mYear = Integer.parseInt(currentDate.substring(0,4)); // current year
                int mMonth = Integer.parseInt(currentDate.substring(5,7)) - 1; // current month
                int mDay = Integer.parseInt(currentDate.substring(8)); // current day
                // date picker dialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener()
                        {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
                            {
                                // set year, month and day value in the edit text
                                editDate.setText(year +
                                        (monthOfYear<9?"-0":"-") + (monthOfYear + 1) +
                                        (dayOfMonth<10?"-0":"-") + dayOfMonth
                                );
                            }
                        },
                        mYear, mMonth, mDay
                );
                datePickerDialog.show();
            }
        });

        // Initialize credentials and service object.
        accountCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(),Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        initializeAccount();

        mAccountViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        mRemarkViewModel = ViewModelProviders.of(this).get(RemarkViewModel.class);

        mAccountViewModel.getAllElements().observe(this, new Observer<List<Account>>()
        {
            @Override
            public void onChanged(@Nullable final List<Account> accounts)
            {
                if (listof_AccountName == null)
                {
                    listof_AccountName = new ArrayList<>();
                }
                else
                {
                    listof_AccountName.clear();
                }
                for (Account account : accounts)
                {
                    listof_AccountName.add(account.getAccountName());
                }
                editAccount.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, listof_AccountName));
            }
        });

        mRemarkViewModel.getAllElements().observe(this, new Observer<List<Remark>>()
        {
            @Override
            public void onChanged(@Nullable final List<Remark> remarks)
            {
                if (remarkList == null)
                {
                    remarkList = new ArrayList<>();
                }
                else
                {
                    remarkList.clear();
                }
                for (Remark remark : remarks)
                {
                    remarkList.add(remark.getRemark());
                }
                editRemark.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, remarkList));
            }
        });
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.integrity_check:
                integrityCheck();
                break;
            case R.id.init_data:
                initializeDataFromApi();
                break;
            case R.id.rst_form:
                editAccount.setText("");
                editDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime()));
                chooseDebit.setSelected(Boolean.TRUE);
                editAmount.setText("");
                editRemark.setText("");
                break;
           default:
                //do nothing here!
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void alert(String msg)
    {
        AlertDialog.Builder abuilder = new AlertDialog.Builder(this);
        abuilder.setMessage(msg);
        abuilder.setCancelable(Boolean.FALSE);
        abuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert11 = abuilder.create();
        alert11.show();
    }

    private void integrityCheck()
    {
        if (! isDeviceOnline())
        {
            alert("No network connection available.");
        }
        else
        {
            new MakeRequestTask(accountCredential, scriptId_MyBank, "integrityCheck", null).execute();
        }
    }

    private void initializeAccount()
    {
        if (! isGooglePlayServicesAvailable())
        {
            acquireGooglePlayServices();
        }
        else if (accountCredential.getSelectedAccountName() == null)
        {
            chooseAccount();
        }
    }

    private void initializeDataFromApi()
    {
        if (! isDeviceOnline())
        {
            alert("No network connection available.");
        }
        else
        {
            mAccountViewModel.deleteAllElements();
            mRemarkViewModel.deleteAllElements();
            initJobTotal = 2;
            initJobCount = 0;
            new MakeRequestTask(accountCredential, scriptId_MyBank, "getAccountList", null).execute();
            new MakeRequestTask(accountCredential, scriptId_MyBank, "getRemarkList", null).execute();
        }
    }

    /** Called when the user taps the Submit Transaction button */
    public void submitTransaction(View view)
    {
        findViewById(R.id.buttonSubmit).setEnabled(Boolean.FALSE);
        getResultsFromApi();
        findViewById(R.id.buttonSubmit).setEnabled(Boolean.TRUE);
    }

    private void displayResult(List<String> output)
    {
        Intent intent = new Intent(this, DisplayResult.class);
        intent.putExtra(transactionAccount, editAccount.getText().toString());
        intent.putExtra(transactionDate, editDate.getText().toString());
        if (chooseDebit.isChecked())
        {
            if (editAccount.getText().toString().equals(account_Rws_Invites) ||
                    editAccount.getText().toString().equals(account_Frasers_Rewards))
            {
                intent.putExtra(debitCredit, " (Spend)");
            }
            else
            {
                intent.putExtra(debitCredit, " (Debit)");
            }
        }
        else
        {
            if (editAccount.getText().toString().equals(account_Rws_Invites) ||
                    editAccount.getText().toString().equals(account_Frasers_Rewards))
            {
                intent.putExtra(debitCredit, " (Redeem)");
            }
            else
            {
                intent.putExtra(debitCredit, " (Credit)");
            }
        }
        intent.putExtra(transactionAmount, editAmount.getText().toString());
        intent.putExtra(transactionRemark, editRemark.getText().toString());
        if (output != null)
        {
            intent.putExtra(accountBalance, output.toArray()[0].toString());
        }
        startActivity(intent);
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi()
    {
        // ID of the script to call. Acquire this from the Apps Script editor,
        // under Publish > Deploy as API executable.
        //String scriptId = null;
        String functionName = "newTransactionRecord";
        List<Object> functionParameters = new ArrayList<>();
        String account = editAccount.getText().toString();
        if (listof_AccountName.indexOf(account) < 0)
        {
            alert("The Account Name (" + account + ") is invalid. Please enter a valid Account Name.");
            return;
        }
        String remark = editRemark.getText().toString();

        functionParameters.add(account);
        functionParameters.add(editDate.getText().toString());
        functionParameters.add(editAmount.getText().toString());

        if (chooseDebit.isChecked())
        {
            functionParameters.add(true);
        }
        else
        {
            functionParameters.add(false);
        }
        functionParameters.add(remark);

        if (!isDeviceOnline())
        {
            alert("No network connection available.");
            return;
        }

        new MakeRequestTask(accountCredential, scriptId_MyBank, functionName, functionParameters).execute();
        if (remarkList.indexOf(remark) < 0)
        {
            mRemarkViewModel.insert(new Remark(remark));
            List<Object> remarkToAdd = new ArrayList<>();
            remarkToAdd.add(remark);
            new MakeRequestTask(accountCredential, scriptId_MyBank, "addRemark", remarkToAdd).execute();
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode))
        {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount()
    {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS))
        {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null)
            {
                accountCredential.setSelectedAccountName(accountName);
            }
            else
            {
                // Start a dialog from which the user can choose an account
                startActivityForResult(accountCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        }
        else
        {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS
            );
        }
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline()
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * An asynchronous task that handles the Google Apps Script Execution API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>>
    {
        private com.google.api.services.script.Script mService = null;
        private Exception mLastError = null;
        private String scriptId = null;
        private String functionName = null;
        private List<Object> functionParameters = null;

        MakeRequestTask(GoogleAccountCredential credential, String script, String function, List<Object> parameters)
        {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            scriptId = script;
            functionName = function;
            functionParameters = parameters;
            mService = new com.google.api.services.script.Script.Builder(
                    transport, jsonFactory, setHttpTimeout(credential)
            ).setApplicationName("TransactionRecords").build();
        }

        /**
         * Background task to call Google Apps Script Execution API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params)
        {
            try
            {
                return getDataFromApi();
            }
            catch (Exception e)
            {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Call the API to run an Apps Script function that returns a list
         * of folders within the user's root directory on Drive.
         *
         * @return list of String folder names and their IDs
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException, GoogleAuthException
        {
            // Create an execution request object.
            ExecutionRequest request = new ExecutionRequest()
                    .setDevMode(Boolean.TRUE) //TODO disable it before release.
                    .setParameters(functionParameters)
                    .setFunction(functionName);

            // Make the request.
            Operation op = mService.scripts().run(scriptId, request).execute();

            // Print results of request.
            if (op.getError() != null)
            {
                throw new IOException(getScriptError(op));
            }

            if (op.getResponse() != null && op.getResponse().get("result") != null)
            {
                return (List<String>) op.getResponse().get("result");
            }
            else
            {
                return null;
            }
        }

        /**
         * Interpret an error response returned by the API and return a String
         * summary.
         *
         * @param op the Operation returning an error response
         * @return summary of error response, or null if Operation returned no
         *     error
         */
        private String getScriptError(Operation op)
        {
            if (op.getError() == null)
            {
                return null;
            }

            // Extract the first (and only) set of error details and cast as a Map.
            // The values of this map are the script's 'errorMessage' and
            // 'errorType', and an array of stack trace elements (which also need to
            // be cast as Maps).
            Map<String, Object> detail = op.getError().getDetails().get(0);
            List<Map<String, Object>> stacktrace = (List<Map<String, Object>>)detail.get("scriptStackTraceElements");

            java.lang.StringBuilder sb = new StringBuilder("\nScript error message: ");
            sb.append(detail.get("errorMessage"));

            if (stacktrace != null)
            {
                // There may not be a stacktrace if the script didn't start
                // executing.
                sb.append("\nScript error stacktrace:");
                for (Map<String, Object> elem : stacktrace)
                {
                    sb.append("\n  ");
                    sb.append(elem.get("function"));
                    sb.append(":");
                    sb.append(elem.get("lineNumber"));
                }
            }
            sb.append("\n");
            return sb.toString();
        }

        @Override
        protected void onPreExecute()
        {
            if (functionName.equals("getAccountList") || functionName.equals("getRemarkList"))
            {
                progressDialog.setMessage("Initializing data from the backend system ...");
            }
            else if (functionName.equals("integrityCheck"))
            {
                progressDialog.setMessage("Executing Integrity Check ...");
            }
            else
            {
                progressDialog.setMessage("Adding transaction record in the backend system ...");
            }
            progressDialog.show();
            progressDialog.setCanceledOnTouchOutside(Boolean.FALSE);
        }

        @Override
        protected void onPostExecute(List<String> output)
        {
            if (functionName.equals("getRemarkList"))
            {
                for (String remark : output)
                {
                    if (remarkList.indexOf(remark) < 0)
                    {
                        mRemarkViewModel.insert(new Remark(remark));
                    }
                }
                initJobCount++;
                if (initJobCount >= initJobTotal)
                {
                    progressDialog.dismiss();
                }
            }
            else if (functionName.equals("getAccountList"))
            {
                for (String account : output)
                {
                    if (listof_AccountName.indexOf(account) < 0)
                    {
                        mAccountViewModel.insert(new Account(account));
                    }
                }
                initJobCount++;
                if (initJobCount >= initJobTotal)
                {
                    progressDialog.dismiss();
                }
            }
            else if (functionName.equals("addRemark"))
            {
                //do nothing here.
            }
            else if (functionName.equals("integrityCheck"))
            {
                progressDialog.dismiss();
                if (output.get(1).equals("Summary"))
                {
                    alert("Integrity Check finished successfully.");
                }
                else
                {
                    alert("Integrity Check finished, but data is incorrect.");
                }
            }
            else
            {
                progressDialog.dismiss();
                if (output.size() == 0)
                {
                    alert("The transaction API for Account Name (" + editAccount.getText().toString() + ") is not available. Please enter a valid Account Name.");
                }
                else
                {
                    displayResult(output);
                }
            }
        }

        @Override
        protected void onCancelled()
        {
            progressDialog.dismiss();
            if (mLastError != null)
            {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException)
                {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                }
                else if (mLastError instanceof UserRecoverableAuthIOException)
                {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                }
                else
                {
                    alert("The following error occurred:\n" + mLastError.getMessage());
                }
            }
            else
            {
                alert("Request cancelled.");
            }
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode)
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(MainActivity.this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * Extend the given HttpRequestInitializer (usually a credentials object)
     * with additional initialize() instructions.
     *
     * @param requestInitializer the initializer to copy and adjust; typically
     *         a credential object.
     * @return an initializer with an extended read timeout.
     */
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer)
    {
        return new HttpRequestInitializer()
        {
            @Override
            public void initialize(HttpRequest httpRequest) throws java.io.IOException
            {
                requestInitializer.initialize(httpRequest);
                // This allows the API to call (and avoid timing out on)
                // functions that take up to 6 minutes to complete (the maximum
                // allowed script run time), plus a little overhead.
                httpRequest.setReadTimeout(380000);
            }
        };
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode)
        {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK)
                {
                    //mOutputText.setText(
                    //        "This app requires Google Play Services. Please install " +
                    //                "Google Play Services on your device and relaunch this app.");
                }
                else
                {
                    //TODO what to do?
                    //initializeDataFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null)
                    {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        accountCredential.setSelectedAccountName(accountName);

                        //TODO what to do?
                        //initializeDataFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK)
                {
                    //TODO what to do?
                    //initializeDataFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    //@Override
    public void onPermissionsGranted(int requestCode, List<String> list)
    {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    //@Override
    public void onPermissionsDenied(int requestCode, List<String> list)
    {
        // Do nothing.
    }
}
