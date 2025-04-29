package com.example.palomitera;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout radioGroupContainer;
    private RadioGroup radioGroup;
    private Button continuarButton;
    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        radioGroupContainer = findViewById(R.id.radioGroupContainer);
        radioGroup = new RadioGroup(this);
        radioGroup.setOrientation(RadioGroup.VERTICAL);
        radioGroupContainer.addView(radioGroup);

        continuarButton = findViewById(R.id.continuarButton);
        client = new OkHttpClient();

        cargarOpcionesDePalomitas();

        continuarButton.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedButton = findViewById(selectedId);

                if (selectedButton != null) {
                    String[] data = selectedButton.getTag().toString().split(",");
                    int idPalomitas = Integer.parseInt(data[0]);
                    double price = Double.parseDouble(data[1]);
                    String size = selectedButton.getText().toString();

                    Log.d("MainActivity", "Datos enviados: id_palomitas: " + idPalomitas + ", Tamaño: " + size + ", Precio: " + price);

                    Intent intent = new Intent(MainActivity.this, DetallesActivity.class);
                    intent.putExtra("id_palomitas", idPalomitas);
                    intent.putExtra("size", size);
                    intent.putExtra("price", price);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Error al seleccionar una opción", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Por favor selecciona un tamaño", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarOpcionesDePalomitas() {
        String url = "https://aimeetyou.pythonanywhere.com/api/v1/palomitas/";

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            radioGroup.removeAllViews();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                try {
                                    JSONObject item = jsonArray.getJSONObject(i);
                                    if (item.has("id_palomitas") && item.has("tamano") && item.has("precio")) {
                                        RadioButton radioButton = new RadioButton(MainActivity.this);
                                        radioButton.setText(item.getString("tamano"));
                                        radioButton.setTag(item.getInt("id_palomitas") + "," + item.getDouble("precio"));
                                        radioButton.setId(View.generateViewId());
                                        radioGroup.addView(radioButton);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
