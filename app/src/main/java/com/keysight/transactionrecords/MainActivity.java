package com.keysight.transactionrecords;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

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

public class MainActivity extends AppCompatActivity {
    static GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {
            "https://www.googleapis.com/auth/drive",
            "https://www.googleapis.com/auth/spreadsheets",
            "https://www.googleapis.com/auth/script.external_request"
    };

    private static final String account_Dbs_eMCA_LIU_YULEI_SGD = "DBS eMCA - LIU YULEI (SGD)";
    private static final String account_Amex_True_Cashback_LIU_YULEI = "AMEX True Cashback - LIU YULEI";
    private static final String account_Amex_True_Cashback_LI_CHANG = "AMEX True Cashback - LI CHANG";
    private static final String account_Posb_Everyday_LIU_YULEI = "POSB Everyday - LIU YULEI (Main)";
    private static final String account_Posb_Everyday_LI_CHANG_S = "POSB Everyday - LI CHANG (Supplementary)";
    private static final String account_Ocbc_360_Account = "OCBC 360 Account";
    private static final String account_Boc_Savings_Suqian_LI_CHANG = "BOC Savings Suqian - LI CHANG";
    private static final String scriptId_DBS_POSB = "MBJnBsoaMrR3J4HbtnjuXqxU9l98eQNnp";
    private static final String scriptId_AMEX = "MgexJWpf6y7_67esZ6IXqnEw9ezPKz0cG";
    private static final String scriptId_OCBC = "MV2T0hPrD2ktnUOLUHHbKGkw9ezPKz0cG";
    private static final String scriptId_BOC = "M6X7qbK-Sn1QkE66425Rf2RU9l98eQNnp";
    //private static final String scriptId_CangBaoTu = "MPPfRL3Vn2anQuRIUA-fu70w9ezPKz0cG"; //藏宝图

    public static final String transactionAccount = "Transaction Account";
    public static final String transactionDate = "Transaction Date";
    public static final String transactionAmount = "Transaction Amount";
    public static final String transactionRemark = "Transaction Remark";
    public static final String accountBalance = "Account Balance";

    private static Spinner spinnerAccount = null;
    private static EditText editDate = null;
    private static EditText editAmount = null;
    private static EditText editRemark = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Apps Script Execution API ...");

        spinnerAccount = (Spinner) findViewById(R.id.spinnerAccount);
        editDate = (EditText) findViewById(R.id.editDate);
        editAmount = (EditText) findViewById(R.id.editAmount);
        editRemark = (EditText) findViewById(R.id.editRemark);

        spinnerAccount.setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[] {
                                account_Dbs_eMCA_LIU_YULEI_SGD,
                                account_Amex_True_Cashback_LIU_YULEI,
                                account_Amex_True_Cashback_LI_CHANG,
                                account_Posb_Everyday_LIU_YULEI,
                                account_Posb_Everyday_LI_CHANG_S,
                                account_Ocbc_360_Account,
                                account_Boc_Savings_Suqian_LI_CHANG
                        }
                )
        );

        editDate.setText(
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                ).format(Calendar.getInstance().getTime())
        );
        editDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get current selected year, month and day.
                String currentDate = editDate.getText().toString();
                int mYear = Integer.parseInt(currentDate.substring(0,4)); // current year
                int mMonth = Integer.parseInt(currentDate.substring(5,7)) - 1; // current month
                int mDay = Integer.parseInt(currentDate.substring(8)); // current day
                // date picker dialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                // set year, month and day value in the edit text
                                editDate.setText(
                                        year +
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
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff());
    }

    /** Called when the user taps the Submit Transaction button */
    public void submitTransaction(View view) {
        findViewById(R.id.buttonSubmit).setEnabled(Boolean.FALSE);
        getResultsFromApi();
        findViewById(R.id.buttonSubmit).setEnabled(Boolean.TRUE);
    }

    private void displayResult(List<String> output) {
        Intent intent = new Intent(this, DisplayResult.class);
        intent.putExtra(transactionAccount, spinnerAccount.getSelectedItem().toString());
        intent.putExtra(transactionDate, editDate.getText().toString());
        intent.putExtra(transactionAmount, editAmount.getText().toString());
        intent.putExtra(transactionRemark, editRemark.getText().toString());
        if (output != null) {
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
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
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
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
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
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * An asynchronous task that handles the Google Apps Script Execution API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.script.Script mService = null;
        private Exception mLastError = null;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.script.Script.Builder(
                    transport, jsonFactory, setHttpTimeout(credential)
            ).setApplicationName("TransactionRecords").build();
        }

        /**
         * Background task to call Google Apps Script Execution API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
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
        private List<String> getDataFromApi() throws IOException, GoogleAuthException {
            // ID of the script to call. Acquire this from the Apps Script editor,
            // under Publish > Deploy as API executable.
            String scriptId = null;
            String functionName = null;
            List<Object> functionParameters = new ArrayList<>();

            switch (spinnerAccount.getSelectedItem().toString()) {
                case account_Dbs_eMCA_LIU_YULEI_SGD:
                    scriptId = scriptId_DBS_POSB;
                    functionName = "newTransaction_eMCA_SGD";
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Amex_True_Cashback_LIU_YULEI:
                    scriptId = scriptId_AMEX;
                    functionName = "newTransaction_True_Cashback";
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Amex_True_Cashback_LI_CHANG:
                    scriptId = scriptId_AMEX;
                    functionName = "newTransaction_True_Cashback_LI_CHANG";
                    functionParameters.add(editDate.getText().toString());
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Posb_Everyday_LIU_YULEI:
                    scriptId = scriptId_DBS_POSB;
                    functionName = "newTransaction_Posb_Everyday";
                    functionParameters.add(editDate.getText().toString());
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add("Liu Yulei");
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Posb_Everyday_LI_CHANG_S:
                    scriptId = scriptId_DBS_POSB;
                    functionName = "newTransaction_Posb_Everyday";
                    functionParameters.add(editDate.getText().toString());
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add("Li Chang");
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Ocbc_360_Account:
                    scriptId = scriptId_OCBC;
                    functionName = "newTransaction_360_Account";
                    functionParameters.add(editDate.getText().toString());
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add(editRemark.getText().toString());
                    break;
                case account_Boc_Savings_Suqian_LI_CHANG:
                    scriptId = scriptId_BOC;
                    functionName = "newTransaction_Savings_Suqian";
                    functionParameters.add(editDate.getText().toString());
                    functionParameters.add(editAmount.getText().toString());
                    functionParameters.add(editRemark.getText().toString());
                    break;
            }

            // Create an execution request object.
            ExecutionRequest request = new ExecutionRequest()
                    .setDevMode(Boolean.TRUE)
                    .setParameters(functionParameters)
                    .setFunction(functionName);

            // Make the request.
            Operation op = mService.scripts().run(scriptId, request).execute();

            // Print results of request.
            if (op.getError() != null) {
                throw new IOException(getScriptError(op));
            }

            if (op.getResponse() != null && op.getResponse().get("result") != null) {
                return (List<String>) op.getResponse().get("result");
            } else {
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
        private String getScriptError(Operation op) {
            if (op.getError() == null) {
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

            if (stacktrace != null) {
                // There may not be a stacktrace if the script didn't start
                // executing.
                sb.append("\nScript error stacktrace:");
                for (Map<String, Object> elem : stacktrace) {
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
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            displayResult(output);
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //mOutputText.setText("The following error occurred:\n"
                    //        + mLastError.getMessage());
                }
            } else {
                //mOutputText.setText("Request cancelled.");
            }
        }
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
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
    private static HttpRequestInitializer setHttpTimeout(final HttpRequestInitializer requestInitializer) {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws java.io.IOException {
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    //mOutputText.setText(
                    //        "This app requires Google Play Services. Please install " +
                    //                "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    //@Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
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
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }
}
