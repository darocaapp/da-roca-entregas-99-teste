package com.daroca.entregas;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;

    private String orderCode = "";

    private static final String FOOD99_URL =
            "https://food-b-h5.99app.com/pt-BR/v2/confirmation-entrega";

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean preenchido = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebChromeClient(
                new WebChromeClient()
        );

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url
                    ) {
                        super.onPageFinished(view, url);

                        if (url != null
                                && url.contains("99app.com")
                                && orderCode.length() == 8) {

                            iniciarTentativas(orderCode);
                        }
                    }
                }
        );

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);

        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {

        orderCode = "";
        preenchido = false;

        Uri data = intent.getData();

        /*
         * O link que receberemos será:
         *
         * daroca://99/19840113
         */

        if (data != null
                && "daroca".equalsIgnoreCase(
                        data.getScheme()
                )) {

            String host = data.getHost();

            if (host != null
                    && host.equalsIgnoreCase("99")) {

                if (!data.getPathSegments().isEmpty()) {

                    orderCode =
                            digitsOnly(
                                    data
                                    .getPathSegments()
                                    .get(0)
                            );
                }
            }
        }

        /*
         * Se abrir pelo link, mostra o código
         * que o aplicativo recebeu.
         */

        if (orderCode.length() == 8) {

            Toast.makeText(
                    this,
                    "99Food recebido: " + orderCode,
                    Toast.LENGTH_SHORT
            ).show();
        }

        webView.loadUrl(FOOD99_URL);
    }

    private String digitsOnly(String value) {

        if (value == null) {
            return "";
        }

        return value.replaceAll("\\D", "");
    }

    private void iniciarTentativas(
            final String codigo
    ) {

        preenchido = false;

        /*
         * Tentamos durante aproximadamente
         * 10 segundos porque os campos da 99
         * podem aparecer depois da página.
         */

        for (int i = 0; i <= 20; i++) {

            handler.postDelayed(
                    () -> preencherCodigo99(codigo),
                    i * 500L
            );
        }
    }

    private void preencherCodigo99(
            String codigo
    ) {

        if (preenchido) {
            return;
        }

        if (codigo == null
                || codigo.length() != 8) {

            return;
        }

        String codigoSeguro =
                codigo
                        .replace("\\", "")
                        .replace("'", "");

        /*
         * Este é exatamente o seletor
         * que encontramos no site da 99:
         *
         * .verification-code-input input
         *
         * Ele retorna os 8 quadradinhos.
         */

        String javascript =

                "(function(){" +

                "var codigo='" +
                codigoSeguro +
                "';" +

                "var campos=" +
                "document.querySelectorAll(" +
                "'.verification-code-input input'" +
                ");" +

                "if(campos.length !== 8){" +
                "return 'AGUARDANDO:' + campos.length;" +
                "}" +

                "for(var i=0;i<8;i++){" +

                "var campo=campos[i];" +
                "var numero=codigo.charAt(i);" +

                "try{" +

                "var setter=" +
                "Object.getOwnPropertyDescriptor(" +
                "window.HTMLInputElement.prototype," +
                "'value'" +
                ").set;" +

                "setter.call(campo,numero);" +

                "campo.dispatchEvent(" +
                "new Event(" +
                "'input'," +
                "{bubbles:true}" +
                ")" +
                ");" +

                "campo.dispatchEvent(" +
                "new Event(" +
                "'change'," +
                "{bubbles:true}" +
                ")" +
                ");" +

                "}catch(e){" +

                "return 'ERRO:' + e.message;" +

                "}" +

                "}" +

                "campos[7].focus();" +

                "return 'PREENCHIDO';" +

                "})();";

        webView.evaluateJavascript(
                javascript,

                resultado -> {

                    if (resultado != null
                            && resultado.contains(
                                    "PREENCHIDO"
                            )) {

                        preenchido = true;

                        Toast.makeText(
                                MainActivity.this,
                                "99Food preenchida: "
                                        + codigo,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }
}
