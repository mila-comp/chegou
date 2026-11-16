package com.mensagemrapida;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Widget da tela inicial: um botão que abre o app já enviando a mensagem do Início.
 */
public class WidgetEnviar extends AppWidgetProvider {

    static final String ACAO_ENVIAR = "com.mensagemrapida.ENVIAR";

    // PendingIntent.FLAG_IMMUTABLE (API 23) — escrito como número porque compilamos com o SDK 23.
    private static final int FLAG_IMMUTABLE = 0x04000000;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        atualizar(context);
    }

    /** Redesenha todos os widgets com o resumo atual (quantos contatos e o começo da mensagem). */
    static void atualizar(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WidgetEnviar.class));
        if (ids.length == 0) return;

        Dados d = Dados.de(context);
        Dados.Mensagem m = d.logado ? d.mensagemAtiva() : null;
        String detalhe;
        if (m == null) {
            detalhe = L.t("Abra o app para criar uma mensagem", "Open the app to create a message");
        } else {
            int n = m.destinatarios.size();
            detalhe = m.nome + " · " + n + (n == 1 ? L.t(" contato", " contact") : L.t(" contatos", " contacts"));
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(ACAO_ENVIAR);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.widget_titulo, L.t("ENVIAR", "SEND"));
        views.setTextViewText(R.id.widget_detalhe, detalhe);
        views.setOnClickPendingIntent(R.id.widget_raiz, pi);
        manager.updateAppWidget(ids, views);
    }
}
