package com.mensagemrapida;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Uma tela dos rascunhos. A MainActivity preenche "a" e "d" antes de chamar criar(). */
abstract class Tela {
    MainActivity a;
    Dados d;

    abstract View criar();

    /** Mostra a barra de baixo com o ☰? (as telas de entrada não mostram) */
    boolean temBarra() {
        return true;
    }

    /** Título da tela, com a seta de voltar quando há para onde voltar. */
    View cabecalho(String titulo) {
        LinearLayout l = new LinearLayout(a);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, Ui.dp(4), 0, Ui.dp(16));
        if (a.podeVoltar()) {
            TextView seta = Ui.texto(a, "‹", 34 / Ui.escalaFonte, Ui.primaria);
            seta.setIncludeFontPadding(false);
            seta.setPadding(0, 0, Ui.dp(14), Ui.dp(4));
            seta.setContentDescription(L.t("Voltar", "Back"));
            seta.setBackground(Ui.clicavel(Ui.forma(android.graphics.Color.TRANSPARENT, 0, 8)));
            seta.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.voltar(); }
            });
            l.addView(seta);
        }
        TextView t = Ui.texto(a, titulo, 22, Ui.texto);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        l.addView(t);
        return l;
    }
}
