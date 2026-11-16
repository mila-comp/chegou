package com.mensagemrapida;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Acompanha um envio: cada pessoa com seu estado, e um botão para abrir as conversas manuais. */
class TelaEnvio extends Tela {

    private static final int VERDE = Color.rgb(37, 150, 90);
    private static final int VERMELHO = Color.rgb(205, 50, 50);

    @Override
    View criar() {
        final Envio e = a.envio;
        LinearLayout col = Ui.coluna(a);
        if (e.mensagem == null) {
            col.addView(cabecalho(L.t("Envio", "Sending")));
            return Ui.rolagem(a, col);
        }
        boolean fim = e.terminou();
        int total = e.itens.size();
        int feitos = e.quantos(Envio.ENVIADO) + e.quantos(Envio.FALHOU);
        col.addView(cabecalho(fim ? L.t("Envio concluído", "Sending finished") : L.t("Enviando…", "Sending…")));

        TextView nome = Ui.texto(a, "“" + e.mensagem.nome + "”", 16, Ui.textoSec);
        col.addView(nome);

        ProgressBar barra = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        barra.setMax(total);
        barra.setProgress(feitos);
        barra.setProgressTintList(ColorStateList.valueOf(Ui.primaria));
        LinearLayout.LayoutParams lpB = Ui.margemBaixo(4);
        lpB.topMargin = Ui.dp(10);
        col.addView(barra, lpB);
        col.addView(Ui.texto(a, feitos + L.t(" de ", " of ") + total, 13, Ui.textoSec), Ui.margemBaixo(14));

        final Envio.Item proximo = e.proximoManual();
        for (final Envio.Item it : e.itens) col.addView(linha(it, it == proximo), Ui.margemBaixo(8));

        if (proximo != null) {
            TextView abrir = Ui.botaoCheio(a, L.t("Abrir conversa com ", "Open chat with ") + proximo.contato.nome);
            abrir.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { e.abrir(proximo); }
            });
            LinearLayout.LayoutParams lp = Ui.margemBaixo(0);
            lp.topMargin = Ui.dp(16);
            col.addView(abrir, lp);
            TextView dica = Ui.texto(a, L.t("Envie na conversa e volte aqui para a próxima.",
                    "Send in the chat, then come back here for the next one."), 13, Ui.textoSec);
            dica.setGravity(Gravity.CENTER);
            dica.setPadding(0, Ui.dp(8), 0, 0);
            col.addView(dica);
        } else if (fim) {
            int falhas = e.quantos(Envio.FALHOU);
            TextView resumo = Ui.texto(a, falhas == 0
                    ? L.t("Tudo certo! ✓", "All done! ✓")
                    : falhas + L.t(" não enviada(s). Veja o motivo acima.", " not sent. See why above."),
                    17, falhas == 0 ? VERDE : VERMELHO);
            resumo.setTypeface(Typeface.DEFAULT_BOLD);
            resumo.setGravity(Gravity.CENTER);
            resumo.setPadding(0, Ui.dp(16), 0, Ui.dp(12));
            col.addView(resumo);
            TextView ok = Ui.botaoCheio(a, L.t("Concluir", "Done"));
            ok.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.irPara(new TelaInicio()); }
            });
            col.addView(ok, Ui.margemBaixo(0));
        }
        return Ui.rolagem(a, col);
    }

    private View linha(final Envio.Item it, boolean destaque) {
        LinearLayout l = new LinearLayout(a);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(Ui.dp(10), Ui.dp(8), Ui.dp(14), Ui.dp(8));
        l.setBackground(Ui.forma(Ui.superficie, destaque ? Ui.primaria : Ui.texto, 30));

        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(36));
        lpA.rightMargin = Ui.dp(12);
        l.addView(Ui.avatar(a, it.contato.nome, 36), lpA);

        LinearLayout textos = new LinearLayout(a);
        textos.setOrientation(LinearLayout.VERTICAL);
        textos.addView(Ui.texto(a, it.contato.nome, 16, Ui.texto));
        boolean falhou = it.estado == Envio.FALHOU;
        textos.addView(Ui.texto(a, falhou ? it.erro : it.rede, 13, falhou ? VERMELHO : Redes.cor(it.rede)));
        l.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (it.estado == Envio.ENVIANDO) {
            ProgressBar girando = new ProgressBar(a);
            girando.setIndeterminateTintList(ColorStateList.valueOf(Ui.primaria));
            l.addView(girando, new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
        } else if (it.estado == Envio.ENVIADO) {
            l.addView(Ui.texto(a, it.auto ? L.t("✓ enviado", "✓ sent") : L.t("✓ aberta", "✓ opened"), 14, VERDE));
        } else if (falhou) {
            l.addView(Ui.texto(a, "✕", 18, VERMELHO));
        } else if (!it.auto) {
            TextView abrir = Ui.chip(a, L.t("Abrir", "Open"), destaque);
            abrir.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.envio.abrir(it); }
            });
            l.addView(abrir);
        }
        return l;
    }
}

/** Os últimos envios feitos. */
class TelaHistorico extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Histórico de envios", "Sending history")));
        if (d.historico.isEmpty()) {
            col.addView(Ui.estadoVazio(a, "🕓", L.t("Nenhum envio ainda", "Nothing sent yet"), null));
        }
        for (Dados.Registro r : d.historico) {
            String quem = r.total + (r.total == 1 ? L.t(" pessoa", " person") : L.t(" pessoas", " people"));
            LinearLayout p = Ui.pilula(a, null, r.nome, quem, Ui.textoSec, null);
            p.setClickable(false);
            p.addView(Ui.texto(a, Dados.dataHora(r.quando), 12, Ui.laranja));
            col.addView(p, Ui.margemBaixo(8));
        }
        return Ui.rolagem(a, col);
    }
}
