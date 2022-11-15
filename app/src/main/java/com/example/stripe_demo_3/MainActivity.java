package com.example.stripe_demo_3;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String STRIPE_PUBLISH_KEY = "pk_test_51LyMLEDtE9pecSduXidyxz8VmYo1tVrxGdXWWErlmCcdwoQQrlnwH4GWt2m9Ik65tIbwn4LT5JwXYS4ZDiOVt0Ix0011BDdnqG";
    private final String STRIPE_SECRET_KEY = "sk_test_51LyMLEDtE9pecSduWmaMpOdYbAThjy4Y3zcnwNWXTV1A06pqvzcsuWM44khZtXHUvQAseIgwPH9v6jNk5FAb8oU500iAKNPiwp";
    PaymentSheet paymentSheet;

    private String customerID;
    private String ephemeralKey;
    private String clientSecret;

    private String amount;
    private String currency;

    private Button btn_StartFlow;
    private Spinner spn_Currency;
    private EditText et_Amount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn_StartFlow = findViewById(R.id.btn_PaymenttFlow);
        spn_Currency = findViewById(R.id.spn_Currency);
        et_Amount = findViewById(R.id.et_Amount);

        // Stripe Initial Configuration
        PaymentConfiguration.init(this, STRIPE_PUBLISH_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentResult);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currency_codes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn_Currency.setAdapter(adapter);

        btn_StartFlow.setOnClickListener(new View.OnClickListener(){
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view){

                DecimalFormat df = new DecimalFormat("0.00");
                try {
                    amount = et_Amount.getText().toString();
                    if(amount.equals("") || Double.parseDouble(amount) <= 0.5){
                        Toast.makeText(getApplicationContext(), "Amount must be greater than or equal to 0.5", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        et_Amount.setText(df.format(Double.parseDouble(amount)));

                        amount = et_Amount.getText().toString().replace(".", "");

                        currency = spn_Currency.getSelectedItem().toString().toLowerCase();

                        Toast.makeText(getApplicationContext(), amount + " " + currency, Toast.LENGTH_LONG).show();

                        // region Request
                        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                                "https://api.stripe.com/v1/customers",
                                response -> {
                                    try {
                                        JSONObject json_data = new JSONObject(response);
                                        customerID = json_data.getString("id");
                                        Toast.makeText(getApplicationContext(), "Customer ID - " + customerID, Toast.LENGTH_SHORT).show();
                                        getEphemeralKey(customerID);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                },
                                error -> {

                                }) {
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Authorization", "Bearer " + STRIPE_SECRET_KEY);
                                return headers;
                            }
                        };
                        // endregion
                        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                        requestQueue.add(stringRequest);
                    }
                }
                catch (Exception e){

                }
            }
        });

    }

    private void onPaymentResult(PaymentSheetResult paymentSheetResult) {
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
        }
    }

    private void getEphemeralKey(String customerID) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/ephemeral_keys",
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json_data = new JSONObject(response);
                            ephemeralKey = json_data.getString("id");
                            Toast.makeText(getApplicationContext(), "Ephemeral Key - " + ephemeralKey, Toast.LENGTH_SHORT).show();

                            getClientSecret(customerID, ephemeralKey);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener(){
            public void onErrorResponse(VolleyError error){
                Log.d("Volley Error", error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + STRIPE_SECRET_KEY);
                headers.put("Stripe-Version", "2022-08-01");
                return headers;
            }

            @NonNull
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("customer", customerID);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    private void getClientSecret(String customerID, String ephemeralKey) {
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                "https://api.stripe.com/v1/payment_intents",
                new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject json_data = new JSONObject(response);
                            clientSecret = json_data.getString("client_secret");
                            Toast.makeText(getApplicationContext(), "Client Secret - " + clientSecret, Toast.LENGTH_SHORT).show();

                            PaymentFlow();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener(){
            public void onErrorResponse(VolleyError error){
                Log.d("Volley Error", error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + STRIPE_SECRET_KEY);
                return headers;
            }

            @NonNull
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("customer", customerID);
                params.put("amount", amount);
                params.put("currency", currency);
                params.put("automatic_payment_methods[enabled]", "true");
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }

    private void PaymentFlow() {
        paymentSheet.presentWithPaymentIntent(clientSecret, new PaymentSheet.Configuration(
                    "Stripe Demo",
                    new PaymentSheet.CustomerConfiguration(
                            customerID,
                            ephemeralKey
                    )
                ));
    }
}