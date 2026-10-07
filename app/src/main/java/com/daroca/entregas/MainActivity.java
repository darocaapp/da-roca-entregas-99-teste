package com.daroca.entregas;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView webView;

    private static final String FOOD99_URL =
            "https://food-b-h5.99app.com/pt-BR/v2/confirmation-entrega";

    // CÓDIGO FIXO APENAS PARA NOSSO PRIMEIRO TESTE
    private static final String CODIGO_TESTE =
            "19840113";

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

                        if (url != null &&
                                url.contains("99app.com")) {

                            iniciarTentativas(
                                    CODIGO_TESTE
                            );
                        }
                    }
                }
        );

        webView.loadUrl(FOOD99_URL);
    }

    private void iniciarTentativas(
            final String codigo
    ) {

        preenchido = false;

        /*
         * A página da 99 pode terminar de carregar
         * antes dos 8 campos aparecerem.
         *
         * Por isso tentamos novamente a cada
         * 500 milissegundos.
         */

        for (int i = 0; i <= 20; i++) {

            final int tentativa = i;

            handler.postDelayed(
                    () -> preencherCodigo99(
                            codigo,
                            tentativa
                    ),
                    i * 500L
            );
        }
    }

    private void preencherCodigo99(
            String codigo,
            int tentativa
    ) {

        if (preenchido) {
            return;
        }

        if (codigo == null ||
                codigo.length() != 8) {

            return;
        }

        String codigoSeguro =
                codigo
                        .replace("\\", "")
                        .replace("'", "");

        /*
         * Este é o seletor que acabamos
         * de testar no Chrome.
         *
         * Ele encontrou exatamente
         * os 8 campos da 99Food.
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

                /*
                 * Ainda não apareceram
                 * os oito campos.
                 */

                "if(campos.length !== 8){" +

                "return 'AGUARDANDO:' +" +
                "campos.length;" +

                "}" +

                /*
                 * Coloca um dígito
                 * em cada quadradinho.
                 */

                "for(var i=0;i<8;i++){" +

                "var campo=campos[i];" +

                "var numero=" +
                "codigo.charAt(i);" +

                "try{" +

                /*
                 * Usa o setter nativo do INPUT.
                 * Foi exatamente o método que
                 * funcionou no nosso teste
                 * pelo Console do Chrome.
                 */

                "var setter=" +

                "Object.getOwnPropertyDescriptor(" +
                "window.HTMLInputElement.prototype," +
                "'value'" +
                ").set;" +

                "setter.call(" +
                "campo," +
                "numero" +
                ");" +

                /*
                 * Avisa a página/Vue que
                 * o valor realmente mudou.
                 */

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

                /*
                 * Deixa o último campo
                 * selecionado.
                 */

                "campos[7].focus();" +

                "return 'PREENCHIDO';" +

                "})();";

        webView.evaluateJavascript(
                javascript,

                resultado -> {

                    if (resultado != null &&
                            resultado.contains(
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

        handler.removeCallbacksAndMessages(
                null
        );
    }
}
