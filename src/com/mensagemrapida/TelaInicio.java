package com.mensagemrapida;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;

/** Tela 3 ("Início"): a última mensagem e o grande botão ENVIAR. */
class TelaInicio extends Tela {

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.setPadding(Ui.dp(24), Ui.dp(24), Ui.dp(24), Ui.dp(16));
        final Dados.Mensagem m = d.mensagemAtiva();

        // Saudação com a foto (tocar na foto abre as configurações).
        LinearLayout topo = new LinearLayout(a);
        topo.setGravity(Gravity.CENTER_VERTICAL);
        String nome = d.conta != null ? d.conta.nome.trim().split("\\s+")[0] : "";
        TextView oi = Ui.texto(a, L.t("Oi, ", "Hi, ") + nome + " 👋", 22, Ui.texto);
        oi.setTypeface(Typeface.DEFAULT_BOLD);
        topo.addView(oi, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        View foto = Ui.foto(a, d.arquivoFoto(), 44);
        foto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaConfig()); }
        });
        topo.addView(foto);
        col.addView(topo, Ui.margemBaixo(12));

        col.addView(Ui.rotulo(a, L.t("Última mensagem", "Last message")));

        if (m == null) {
            TextView criarBt = Ui.botao(a, L.t("Criar mensagem", "Create message"));
            criarBt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaFormMensagem(null)); }
            });
            col.addView(Ui.estadoVazio(a, "💬", L.t("Nenhuma mensagem ainda.", "No messages yet."), criarBt));
        } else {
            if (d.mensagens.size() > 1) col.addView(chips(m), Ui.margemBaixo(10));
            col.addView(cartao(m));
        }

        View vao = new View(a);
        col.addView(vao, new LinearLayout.LayoutParams(1, 0, 1));

        // O grande botão ENVIAR: envia com tudo o que foi configurado na mensagem.
        final TextView enviar = Ui.texto(a, L.t("ENVIAR", "SEND"), 34, Ui.sobreDestaque);
        enviar.setGravity(Gravity.CENTER);
        enviar.setLetterSpacing(0.08f);
        enviar.setTypeface(Typeface.DEFAULT_BOLD);
        enviar.setBackground(Ui.clicavel(Ui.forma(Ui.destaque, 0, 60)));
        enviar.setElevation(Ui.dp(6));
        enviar.setClickable(true);
        Ui.pressionavel(enviar);
        enviar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (m == null) a.abrir(new TelaFormMensagem(null));
                else a.envio.enviar(m);
            }
        });
        LinearLayout.LayoutParams lpEnviar = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(96));
        lpEnviar.setMargins(Ui.dp(8), Ui.dp(20), Ui.dp(8), Ui.dp(8));
        col.addView(enviar, lpEnviar);
        // Um "pulinho" ao aparecer, para chamar a atenção para o botão.
        enviar.setScaleX(0.9f);
        enviar.setScaleY(0.9f);
        enviar.animate().scaleX(1f).scaleY(1f).setStartDelay(120).setDuration(320)
                .setInterpolator(new android.view.animation.OvershootInterpolator(3f));

        View vao2 = new View(a);
        col.addView(vao2, new LinearLayout.LayoutParams(1, 0, 1));

        if (!d.historico.isEmpty()) {
            Dados.Registro r = d.historico.get(0);
            TextView ultimo = Ui.link(a, "🕓 " + L.t("Último envio: ", "Last sent: ") + Dados.dataHora(r.quando)
                    + " · " + r.nome + "  ›");
            ultimo.setTextColor(Ui.textoSec);
            ultimo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaHistorico()); }
            });
            LinearLayout.LayoutParams lpU = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpU.gravity = Gravity.CENTER_HORIZONTAL;
            col.addView(ultimo, lpU);
        }

        TextView atalho = Ui.link(a, L.t("＋ Criar atalho na tela inicial", "＋ Add shortcut to home screen"));
        LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpA.gravity = Gravity.CENTER_HORIZONTAL;
        atalho.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { criarAtalho(); }
        });
        col.addView(atalho, lpA);
        return Ui.rolagem(a, col);
    }

    /** Fileira de etiquetas para trocar de mensagem com um toque. */
    private View chips(Dados.Mensagem ativa) {
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout l = new LinearLayout(a);
        for (final Dados.Mensagem x : d.mensagens) {
            TextView chip = Ui.chip(a, x.nome, x == ativa);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    d.ativa = x.id;
                    d.salvar();
                    a.desenhar();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(8);
            l.addView(chip, lp);
        }
        hs.addView(l);
        return hs;
    }

    /** Resumo da mensagem: nome, texto, para quem e por onde. */
    private View cartao(final Dados.Mensagem m) {
        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(10));
        c.setBackground(Ui.clicavel(Ui.forma(Ui.superficie, Ui.contorno, 18)));
        c.setElevation(Ui.dp(2));
        c.setClickable(true);

        TextView nome = Ui.texto(a, m.nome, 18, Ui.texto);
        nome.setTypeface(Typeface.DEFAULT_BOLD);
        c.addView(nome);
        if (!m.texto.isEmpty()) {
            TextView t = Ui.texto(a, m.texto, 15, Ui.texto);
            t.setMaxLines(4);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setPadding(0, Ui.dp(4), 0, Ui.dp(8));
            c.addView(t);
        }
        Dados.Midia md = d.midia(m.midia);
        if (md != null) c.addView(Ui.texto(a, "📎 " + md.nome, 14, Ui.textoSec));

        // Quem recebe: bolinhas com as iniciais.
        LinearLayout quem = new LinearLayout(a);
        quem.setGravity(Gravity.CENTER_VERTICAL);
        quem.setPadding(0, Ui.dp(6), 0, Ui.dp(2));
        int n = 0;
        for (String id : m.destinatarios) {
            Dados.Contato x = d.contato(id);
            if (x == null) continue;
            if (n < 5) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(30), Ui.dp(30));
                lp.rightMargin = Ui.dp(6);
                quem.addView(Ui.avatar(a, x.nome, 30), lp);
            }
            n++;
        }
        if (n > 5) quem.addView(Ui.texto(a, "+" + (n - 5), 14, Ui.textoSec));
        if (n > 0) c.addView(quem);
        c.addView(Ui.texto(a, resumoDestino(d, m), 14, Ui.textoSec));
        if (m.lembrete) {
            c.addView(Ui.texto(a, "🔔 " + L.t("Lembrete das ", "Reminder from ") + Dados.hora(m.lembreteInicio)
                    + L.t(" às ", " to ") + Dados.hora(m.lembreteFim), 14, Ui.laranja));
        }

        LinearLayout acoes = new LinearLayout(a);
        acoes.setGravity(Gravity.END);
        TextView editar = Ui.link(a, L.t("Editar texto", "Edit text"));
        TextView info = Ui.link(a, L.t("Info / quem recebe", "Info / recipients"));
        acoes.addView(editar);
        acoes.addView(info);
        c.addView(acoes);

        View.OnClickListener abrirInfo = new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaInfoMensagem(m)); }
        };
        c.setOnClickListener(abrirInfo);
        info.setOnClickListener(abrirInfo);
        editar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormMensagem(m)); }
        });
        return c;
    }

    static String resumoDestino(Dados d, Dados.Mensagem m) {
        StringBuilder nomes = new StringBuilder();
        int n = 0;
        for (String id : m.destinatarios) {
            Dados.Contato c = d.contato(id);
            if (c == null) continue;
            if (n < 3) nomes.append(n > 0 ? ", " : "").append(c.nome);
            n++;
        }
        if (n == 0) return L.t("Para: ninguém ainda (toque em Info)", "To: nobody yet (tap Info)");
        if (n > 3) nomes.append(L.t(" e mais ", " and ")).append(n - 3).append(L.t("", " more"));
        String via = m.rede.isEmpty() ? L.t("meio principal de cada um", "each one's main app") : m.rede;
        return L.t("Para: ", "To: ") + nomes + "\n" + L.t("Via: ", "Via: ") + via;
    }

    /** Pede ao launcher para fixar o widget (Android 8+); senão, explica como adicionar. */
    private void criarAtalho() {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(a);
            // requestPinAppWidget é da API 26; chamado por reflexão porque compilamos com o SDK 23.
            Method suportado = AppWidgetManager.class.getMethod("isRequestPinAppWidgetSupported");
            if ((Boolean) suportado.invoke(m)) {
                Method fixar = AppWidgetManager.class.getMethod("requestPinAppWidget",
                        ComponentName.class, Bundle.class, PendingIntent.class);
                fixar.invoke(m, new ComponentName(a, WidgetEnviar.class), null, null);
                return;
            }
        } catch (Exception e) {
            // Android antigo ou launcher sem suporte: cai nas instruções abaixo.
        }
        a.dialogo()
                .setTitle(L.t("Criar atalho", "Add shortcut"))
                .setMessage(L.t("1. Volte para a tela inicial.\n2. Toque e segure num espaço vazio.\n"
                                + "3. Escolha \"Widgets\".\n4. Procure \"Chegou\" e arraste o botão Enviar.",
                        "1. Go to your home screen.\n2. Touch and hold an empty spot.\n"
                                + "3. Choose \"Widgets\".\n4. Find \"Chegou\" and drag the Send button."))
                .setPositiveButton("OK", null)
                .show();
    }
}
