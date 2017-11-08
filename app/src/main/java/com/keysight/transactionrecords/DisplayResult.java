package com.keysight.transactionrecords;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class DisplayResult extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_result);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        ((TextView) findViewById(R.id.textView)).setText(
                "New transaction has been added successfully." +
                "\nAccount: " + intent.getStringExtra(MainActivity.transactionAccount) +
                "\nDate: " + intent.getStringExtra(MainActivity.transactionDate) +
                "\nAmount: " + intent.getStringExtra(MainActivity.transactionAmount) + intent.getStringExtra(MainActivity.debitCredit) +
                "\nRemark: " +  intent.getStringExtra(MainActivity.transactionRemark) +
                "\nBalance: " + intent.getStringExtra(MainActivity.accountBalance)
        );
    }
}
