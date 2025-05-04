package com.example.palomitera;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

public class ProcesandoActivity extends AppCompatActivity {

    private int idPedido;
    private Handler handler = new Handler();
    private OkHttpClient client = new OkHttpClient();
    private LinearLayout layoutContainer;
    private ProgressBar progressBar;
    private TextView statusTextView;

    private boolean mostrarBotonServirMostrado = false;
    private boolean dialogoMostrado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_procesando);

        layoutContainer = findViewById(R.id.procesandoContainer);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.statusTextView);

        idPedido = getIntent().getIntExtra("id_pedido", -1);

        if (idPedido != -1) {
            Log.d("ProcesandoActivity", "ID del pedido: " + idPedido);
            verificarEstadoPedido();
        } else {
            Toast.makeText(this, "Error al obtener el ID del pedido", Toast.LENGTH_SHORT).show();
        }
    }

    private void verificarEstadoPedido() {
        String url = "https://aimeetyou.pythonanywhere.com/api/v1/pedidos/" + idPedido + "/";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ProcesandoActivity.this, "Error en la conexión", Toast.LENGTH_SHORT).show());
                volverAVerificar(); // <- seguir intentando
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        JSONObject pedido = new JSONObject(responseBody);

                        int estadoPedidoId = pedido.getInt("estado_pedido");

                        Log.d("ProcesandoActivity", "Respuesta de la API: " + responseBody);
                        Log.d("ProcesandoActivity", "Estado del pedido: " + estadoPedidoId);

                        runOnUiThread(() -> {
                            if (estadoPedidoId == 2 && !mostrarBotonServirMostrado) {
                                mostrarBotonServirMostrado = true;
                                mostrarBotonServir();
                            } else if (estadoPedidoId == 3 && !dialogoMostrado) {
                                dialogoMostrado = true;
                                progressBar.setVisibility(View.GONE);
                                try {
                                    String codigo = pedido.getString("codigo_verificacion");
                                    mostrarDialogoPago(codigo);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else if (estadoPedidoId != 3) {
                                statusTextView.setText("Esperando a que el pedido esté listo...");
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        response.close();
                        volverAVerificar(); // <- continuar monitoreando
                    }
                } else {
                    Log.e("ProcesandoActivity", "Respuesta no exitosa: " + response.code());
                    response.close();
                    volverAVerificar(); // <- continuar monitoreando
                }
            }
        });
    }

    private void volverAVerificar() {
        handler.postDelayed(this::verificarEstadoPedido, 3000); // <- Verifica cada 3 segundos
    }

    private void mostrarBotonServir() {
        progressBar.setVisibility(View.GONE);
        statusTextView.setText("¡Pedido listo para servir!");

        Button servirButton = new Button(this);
        servirButton.setText("Servir Pedido");

        servirButton.setOnClickListener(v -> confirmarPedidoServido());

        layoutContainer.addView(servirButton);
    }

    private void confirmarPedidoServido() {
        String url = "https://aimeetyou.pythonanywhere.com/api/v1/pedidos/" + idPedido + "/";

        JSONObject json = new JSONObject();
        try {
            json.put("estado_pedido", 3);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ProcesandoActivity.this, "Error al actualizar estado", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProcesandoActivity.this, "¡Pedido servido con éxito!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ProcesandoActivity.this, "Error al servir el pedido", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void mostrarDialogoPago(String codigoVerificacion) {
        new AlertDialog.Builder(this)
                .setTitle("¡Pedido listo!")
                .setMessage("Listo, ahora puedes pagar.")
                .setPositiveButton("OK", (dialog, which) -> mostrarCodigoVerificacion(codigoVerificacion))
                .setCancelable(false)
                .show();
    }

    private void mostrarCodigoVerificacion(String codigo) {
        new AlertDialog.Builder(this)
                .setTitle("Código de verificación")
                .setMessage("Tu código es: " + codigo)
                .setPositiveButton("Cerrar", null)
                .show();
    }
}
