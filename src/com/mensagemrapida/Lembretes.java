package com.mensagemrapida;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Lembretes: dentro da janela de horário de uma mensagem (ex.: 16h às 19h) aparece uma notificação
 * com o botão Enviar. Se todos os destinatários são de envio automático (SMS/Telegram), o botão
 * envia ali mesmo, sem abrir o app; senão, abre a tela de envio.
 */
public class Lembretes extends BroadcastReceiver {

    static final String ACAO_ALARME = "com.mensagemrapida.ALARME";
    static final String ACAO_ENVIAR_FUNDO = "com.mensagemrapida.ENVIAR_FUNDO";
    static final String ACAO_DISPENSAR = "com.mensagemrapida.DISPENSAR";
    static final String EXTRA_MENSAGEM = "mensagem";
    static final String PERMISSAO_NOTIFICAR = "android.permission.POST_NOTIFICATIONS"; // API 33

    private static final String CANAL = "lembretes";
    // PendingIntent.FLAG_IMMUTABLE — escrito como número porque compilamos com o SDK 23.
    private static final int IMUTAVEL = 0x04000000;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String acao = String.valueOf(intent.getAction());
        Dados d = Dados.de(ctx);
        Dados.Mensagem m = d.mensagem(String.valueOf(intent.getStringExtra(EXTRA_MENSAGEM)));
        if (ACAO_DISPENSAR.equals(acao)) {
            if (m != null) {
                m.lembreteFeito = hoje();
                d.salvar();
            }
        } else if (ACAO_ENVIAR_FUNDO.equals(acao)) {
            if (m != null) enviarEmFundo(ctx, m, goAsync());
        } else {
            // Alarme de início/fim de janela, ou o celular acabou de ligar.
            reagendar(ctx);
        }
    }

    // ------------------------------------------------------------------ agenda

    /** Mostra/esconde as notificações conforme a hora atual e agenda o próximo início ou fim de janela. */
    static void reagendar(Context ctx) {
        Dados d = Dados.de(ctx);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (nm == null || am == null) return;

        Calendar agora = Calendar.getInstance();
        long proximo = Long.MAX_VALUE;
        for (Dados.Mensagem m : d.mensagens) {
            if (!d.logado || !m.lembrete) {
                nm.cancel(idNotificacao(m));
                continue;
            }
            if (!dentroDaJanela(m, agora)) nm.cancel(idNotificacao(m));
            else if (m.lembreteFeito != hoje()) mostrar(ctx, d, m);
            proximo = Math.min(proximo, Math.min(proximaVez(m.lembreteInicio, agora), proximaVez(m.lembreteFim, agora)));
        }

        PendingIntent alarme = PendingIntent.getBroadcast(ctx, 0,
                new Intent(ctx, Lembretes.class).setAction(ACAO_ALARME), PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);
        am.cancel(alarme);
        if (proximo == Long.MAX_VALUE) return;
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo, alarme);
        } catch (SecurityException e) {
            // Sem permissão de alarme exato: o aviso pode atrasar alguns minutos.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo, alarme);
        }
    }

    static boolean dentroDaJanela(Dados.Mensagem m, Calendar agora) {
        int dia = agora.get(Calendar.DAY_OF_WEEK) - 1; // domingo = 0
        if ((m.lembreteDias & (1 << dia)) == 0) return false;
        int min = agora.get(Calendar.HOUR_OF_DAY) * 60 + agora.get(Calendar.MINUTE);
        if (m.lembreteInicio < m.lembreteFim) return min >= m.lembreteInicio && min < m.lembreteFim;
        return min >= m.lembreteInicio || min < m.lembreteFim; // janela que atravessa a meia-noite
    }

    /** O próximo instante (em ms) em que o relógio marcar esse horário. */
    private static long proximaVez(int minutos, Calendar agora) {
        Calendar c = (Calendar) agora.clone();
        c.set(Calendar.HOUR_OF_DAY, minutos / 60);
        c.set(Calendar.MINUTE, minutos % 60);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= agora.getTimeInMillis()) c.add(Calendar.DAY_OF_MONTH, 1);
        return c.getTimeInMillis();
    }

    static int hoje() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    static int idNotificacao(Dados.Mensagem m) {
        return 1000 + (m.id.hashCode() & 0xFFFFF);
    }

    // ------------------------------------------------------------------ notificação

    private static void mostrar(Context ctx, Dados d, Dados.Mensagem m) {
        List<String> nomes = new ArrayList<String>();
        for (String id : m.destinatarios) {
            Dados.Contato c = d.contato(id);
            if (c != null) nomes.add(c.nome.trim().split("\\s+")[0]);
        }
        if (nomes.isEmpty()) return;
        String quem = nomes.size() <= 3 ? juntar(nomes)
                : nomes.get(0) + ", " + nomes.get(1) + L.t(" e mais ", " and ") + (nomes.size() - 2) + L.t("", " more");

        Uri unico = Uri.parse("chegou://mensagem/" + m.id);
        PendingIntent enviar;
        if (podeEnviarEmFundo(ctx, d, m)) {
            enviar = PendingIntent.getBroadcast(ctx, 0, new Intent(ctx, Lembretes.class)
                    .setAction(ACAO_ENVIAR_FUNDO).setData(unico).putExtra(EXTRA_MENSAGEM, m.id),
                    PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);
        } else {
            enviar = PendingIntent.getActivity(ctx, 0, new Intent(ctx, MainActivity.class)
                    .setAction(WidgetEnviar.ACAO_ENVIAR).setData(unico).putExtra(EXTRA_MENSAGEM, m.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);
        }
        PendingIntent abrir = PendingIntent.getActivity(ctx, 1, new Intent(ctx, MainActivity.class)
                .setAction(Intent.ACTION_VIEW).setData(unico).putExtra(EXTRA_MENSAGEM, m.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);
        PendingIntent dispensar = PendingIntent.getBroadcast(ctx, 2, new Intent(ctx, Lembretes.class)
                .setAction(ACAO_DISPENSAR).setData(unico).putExtra(EXTRA_MENSAGEM, m.id),
                PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);

        Notification.Builder b = construtor(ctx)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(Ui.corPrincipal(d))
                .setContentTitle(m.nome)
                .setContentText(L.t("Avisar ", "Tell ") + quem + "?")
                .setContentIntent(abrir)
                .setDeleteIntent(dispensar)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .addAction(R.drawable.ic_notif, L.t("Enviar agora", "Send now"), enviar);
        if (!m.texto.isEmpty()) b.setStyle(new Notification.BigTextStyle().bigText(
                L.t("Avisar ", "Tell ") + quem + "?\n“" + m.texto + "”"));
        avisar(ctx, idNotificacao(m), b.build());
    }

    /** Notificação curta com o resultado de um envio feito pelo botão. */
    private static void mostrarResultado(Context ctx, Dados d, Dados.Mensagem m, String texto) {
        PendingIntent abrir = PendingIntent.getActivity(ctx, 3, new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | IMUTAVEL);
        Notification.Builder b = construtor(ctx)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(Ui.corPrincipal(d))
                .setContentTitle(m.nome)
                .setContentText(texto)
                .setContentIntent(abrir)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);
        avisar(ctx, idNotificacao(m), b.build());
    }

    private static void avisar(Context ctx, int id, Notification n) {
        try {
            ((NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
        } catch (SecurityException e) {
            // sem permissão de notificar: nada a fazer
        }
    }

    /**
     * No Android 8+ toda notificação precisa de um "canal". As classes de canal não existem no SDK 23
     * com que compilamos, então são chamadas por reflexão.
     */
    @SuppressWarnings("deprecation")
    private static Notification.Builder construtor(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                Class<?> canal = Class.forName("android.app.NotificationChannel");
                // Importância 4 (alta): a notificação aparece como pop-up no topo da tela.
                Object c = canal.getConstructor(String.class, CharSequence.class, int.class)
                        .newInstance(CANAL, L.t("Lembretes de envio", "Send reminders"), 4);
                NotificationManager.class.getMethod("createNotificationChannel", canal).invoke(nm, c);
                return Notification.Builder.class.getConstructor(Context.class, String.class).newInstance(ctx, CANAL);
            } catch (Exception e) {
                // cai no construtor antigo
            }
        }
        return new Notification.Builder(ctx).setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);
    }

    private static String juntar(List<String> nomes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nomes.size(); i++) {
            if (i > 0) sb.append(i == nomes.size() - 1 ? L.t(" e ", " and ") : ", ");
            sb.append(nomes.get(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ envio sem abrir o app

    /** Só dá para enviar em segundo plano se ninguém depender de abrir uma conversa. */
    static boolean podeEnviarEmFundo(Context ctx, Dados d, Dados.Mensagem m) {
        boolean temMidia = d.midia(m.midia) != null;
        int n = 0;
        for (String id : m.destinatarios) {
            Dados.Contato c = d.contato(id);
            if (c == null) continue;
            String rede = m.rede.isEmpty() ? c.meio : m.rede;
            if (!Redes.automatica(rede, temMidia)) return false;
            if (Redes.SMS.equals(rede)) {
                if (c.telefone.trim().isEmpty()) return false;
                if (ctx.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            } else if (c.usuario.trim().isEmpty() || d.telegramToken.trim().isEmpty()) {
                return false;
            }
            n++;
        }
        return n > 0;
    }

    @SuppressWarnings("deprecation")
    private static void enviarEmFundo(final Context ctx, final Dados.Mensagem m, final PendingResult pendente) {
        final Dados d = Dados.de(ctx);
        final Dados.Midia md = d.midia(m.midia);
        final String token = d.telegramToken.trim();
        final List<Dados.Contato> para = new ArrayList<Dados.Contato>();
        for (String id : m.destinatarios) {
            Dados.Contato c = d.contato(id);
            if (c != null) para.add(c);
        }
        mostrarResultado(ctx, d, m, L.t("Enviando…", "Sending…"));
        new Thread(new Runnable() {
            public void run() {
                int ok = 0;
                for (Dados.Contato c : para) {
                    String rede = m.rede.isEmpty() ? c.meio : m.rede;
                    String texto = Envio.textoPara(m, c);
                    try {
                        if (Redes.SMS.equals(rede)) {
                            SmsManager sm = SmsManager.getDefault();
                            sm.sendMultipartTextMessage(c.telefone, null, sm.divideMessage(texto), null, null);
                            ok++;
                        } else {
                            String r = md != null
                                    ? Envio.enviarArquivoTelegram(token, c.usuario.trim(), texto, md,
                                            new File(d.pastaMidias(), md.arquivo))
                                    : Envio.http("https://api.telegram.org/bot" + token + "/sendMessage",
                                            "chat_id=" + Envio.codificar(c.usuario.trim()) + "&text=" + Envio.codificar(texto));
                            if (new JSONObject(r).optBoolean("ok")) ok++;
                        }
                    } catch (Exception e) {
                        // conta como falha
                    }
                }
                final int enviados = ok;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        m.lembreteFeito = hoje();
                        d.registrarEnvio(m.nome, para.size());
                        int falhas = para.size() - enviados;
                        mostrarResultado(ctx, d, m, falhas == 0
                                ? L.t("Enviado ✓ para ", "Sent ✓ to ") + enviados
                                        + (enviados == 1 ? L.t(" pessoa", " person") : L.t(" pessoas", " people"))
                                : falhas + L.t(" não enviada(s). Toque para abrir o app.", " not sent. Tap to open the app."));
                        pendente.finish();
                    }
                });
            }
        }).start();
    }
}
