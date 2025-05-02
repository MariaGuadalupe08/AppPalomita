package com.example.palomitera;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class DetallesActivity extends AppCompatActivity {

    private int idPalomitas;
    private String size;
    private double price;
    private String imageUrl;
    private OkHttpClient client;
    private TextView sizeTextView, priceTextView;
    private Button ordenarButton;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalles);

        client = new OkHttpClient();

        idPalomitas = getIntent().getIntExtra("id_palomitas", -1);
        size = getIntent().getStringExtra("size");
        price = getIntent().getDoubleExtra("price", 0.0);
        imageUrl = getIntent().getStringExtra("image_url");  // Obtener la URL de la imagen

        sizeTextView = findViewById(R.id.sizeTextView);
        priceTextView = findViewById(R.id.priceTextView);
        ordenarButton = findViewById(R.id.ordenarButton);
        imageView = findViewById(R.id.palomitasImageView); // Referencia a la ImageView

        if (idPalomitas != -1) {
            cargarDetallesPalomitas(idPalomitas);
        } else {
            Toast.makeText(this, "Error al obtener datos de palomitas", Toast.LENGTH_SHORT).show();
        }

        // Cargar la imagen de las palomitas
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)  // Usamos Glide para cargar la imagen desde la URL
                    .into(imageView);
        }

        ordenarButton.setOnClickListener(v -> realizarPedido());
    }

    private void cargarDetallesPalomitas(int id) {
        String url = "https://aimeetyou.pythonanywhere.com/api/v1/palomitas/" + id + "/";

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(DetallesActivity.this, "Error al cargar detalles", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String tamano = json.getString("tamano");
                        double precio = json.getDouble("precio");

                        runOnUiThread(() -> {
                            sizeTextView.setText(tamano);
                            priceTextView.setText("$" + precio);
                        });

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                response.close();
            }
        });
    }

    private void realizarPedido() {
        ordenarButton.setEnabled(false);
        String url = "https://aimeetyou.pythonanywhere.com/api/v1/pedidos/";

        JSONObject json = new JSONObject();
        try {
            json.put("codigo_verificacion", new Random().nextInt(90000000) + 10000000);
            json.put("fecha", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            json.put("monto_pagado", price);
            json.put("cambio", false);
            json.put("palomitas", idPalomitas);
            json.put("palomitera", 1);
            json.put("estado_pedido", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    ordenarButton.setEnabled(true);
                    Toast.makeText(DetallesActivity.this, "Error al realizar pedido", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("DetallesActivity", "Respuesta del servidor: " + responseBody);

                    try {
                        JSONObject respuestaJson = new JSONObject(responseBody);

                        if (respuestaJson.has("data")) {
                            JSONObject data = respuestaJson.getJSONObject("data");

                            int idPedido = data.getInt("id_pedido");

                            Log.d("DetallesActivity", "ID del pedido: " + idPedido);

                            runOnUiThread(() -> {
                                Intent intent = new Intent(DetallesActivity.this, ProcesandoActivity.class);
                                intent.putExtra("id_pedido", idPedido);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                ordenarButton.setEnabled(true);
                                Toast.makeText(DetallesActivity.this, "Error: No se encontró 'data' en la respuesta.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            ordenarButton.setEnabled(true);
                            Toast.makeText(DetallesActivity.this, "Error al procesar la respuesta del pedido", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        ordenarButton.setEnabled(true);
                        Toast.makeText(DetallesActivity.this, "Error: No se pudo registrar el pedido", Toast.LENGTH_SHORT).show();
                    });
                }
                response.close();
            }
        });
    }
}
