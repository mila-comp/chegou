package com.mensagemrapida;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Vibrator;
import android.telephony.SmsManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * O que acontece ao tocar em ENVIAR.
 *
 * SMS (sem mídia) e Telegram vão sozinhos. As outras redes não permitem que um app envie
 * por você: a tela de envio mostra cada pessoa com um botão para abrir a conversa pronta.
 */
final class Envio {

    static final int AGUARDANDO = 0, ENVIANDO = 1, ENVIADO = 2, FALHOU = 3;

    static class Item {
        final Dados.Contato contato;
        final String rede;
        final boolean auto;
        int estado = AGUARDANDO;
        String erro = "";

        Item(Dados.Contato c, String r, boolean auto) {
            contato = c;
            rede = r;
            this.auto = auto;
        }
    }

    private final MainActivity a;
    private final Dados d;

    final List<Item> itens = new ArrayList<Item>();
    Dados.Mensagem mensagem;
    private Dados.Midia midia;
    private boolean voltandoDeConversa;
    private boolean vibrou;

    Envio(MainActivity a) {
        this.a = a;
        this.d = Dados.de(a);
    }

    void enviar(Dados.Mensagem m) {
        enviar(m, null);
    }

    /** so != null envia apenas para esses contatos (ex.: "enviar só para esta pessoa"). */
    void enviar(final Dados.Mensagem m, List<Dados.Contato> so) {
        if (m == null) {
            a.aviso(L.t("Crie uma mensagem primeiro.", "Create a message first."));
            return;
        }
        if (m.texto.trim().isEmpty() && d.midia(m.midia) == null) {
            a.aviso(L.t("A mensagem está vazia.", "The message is empty."));
            return;
        }
        final List<Dados.Contato> para = new ArrayList<Dados.Contato>();
        if (so != null) {
            para.addAll(so);
        } else {
            for (String id : m.destinatarios) {
                Dados.Contato c = d.contato(id);
                if (c != null) para.add(c);
            }
        }
        if (para.isEmpty()) {
            a.aviso(L.t("Escolha para quem enviar em Info.", "Choose recipients in Info."));
            return;
        }
        if (d.confirmar) {
            a.confirmar(L.t("Enviar \"", "Send \"") + m.nome + L.t("\" para ", "\" to ") + para.size()
                    + (para.size() == 1 ? L.t(" pessoa?", " person?") : L.t(" pessoas?", " people?")),
                    new Runnable() {
                        public void run() { preparar(m, para); }
                    });
        } else {
            preparar(m, para);
        }
    }

    private void preparar(Dados.Mensagem m, List<Dados.Contato> para) {
        mensagem = m;
        midia = d.midia(m.midia);
        itens.clear();
        vibrou = false;
        boolean temSms = false;
        for (Dados.Contato c : para) {
            String rede = m.rede.isEmpty() ? c.meio : m.rede;
            Item it = new Item(c, rede, Redes.automatica(rede, midia != null));
            String precisa = Redes.usaTelefone(rede) ? c.telefone : c.usuario;
            if (precisa.trim().isEmpty()) {
                it.estado = FALHOU;
                it.erro = Redes.usaTelefone(rede) ? L.t("sem telefone", "no phone number")
                        : L.t("sem usuário de ", "no username for ") + rede;
            } else if (it.auto && Redes.SMS.equals(rede)) {
                temSms = true;
            }
            itens.add(it);
        }
        if (temSms && a.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            a.pedirPermissaoSms(new MainActivity.Resposta() {
                public void sim(boolean ok) {
                    if (!ok) {
                        for (Item it : itens) {
                            if (it.auto && Redes.SMS.equals(it.rede) && it.estado == AGUARDANDO) {
                                it.estado = FALHOU;
                                it.erro = L.t("sem permissão de SMS", "no SMS permission");
                            }
                        }
                    }
                    iniciar();
                }
            });
            return;
        }
        iniciar();
    }

    private void iniciar() {
        d.ativa = mensagem.id;
        d.registrarEnvio(mensagem.nome, itens.size());
        if (!(a.telaAtual() instanceof TelaEnvio)) a.abrir(new TelaEnvio());

        List<Item> telegram = new ArrayList<Item>();
        for (Item it : itens) {
            if (!it.auto || it.estado != AGUARDANDO) continue;
            if (Redes.SMS.equals(it.rede)) enviarSms(it);
            else telegram.add(it);
        }
        if (!telegram.isEmpty()) enviarTelegram(telegram);
        mudou();
    }

    /** O texto para uma pessoa: troca {nome} pelo primeiro nome dela. */
    String textoPara(Dados.Contato c) {
        String primeiro = c.nome.trim().split("\\s+")[0];
        return mensagem.texto.replace("{nome}", primeiro);
    }

    /** A próxima conversa que o usuário ainda precisa abrir, ou null. */
    Item proximoManual() {
        for (Item it : itens) if (!it.auto && it.estado == AGUARDANDO) return it;
        return null;
    }

    boolean terminou() {
        for (Item it : itens) if (it.estado == AGUARDANDO || it.estado == ENVIANDO) return false;
        return true;
    }

    int quantos(int estado) {
        int n = 0;
        for (Item it : itens) if (it.estado == estado) n++;
        return n;
    }

    /** Chamado quando o usuário volta ao app depois de uma conversa aberta. */
    void aoRetornar() {
        if (!voltandoDeConversa) return;
        voltandoDeConversa = false;
        mudou();
    }

    private void mudou() {
        if (a.telaAtual() instanceof TelaEnvio) a.desenhar();
        if (!vibrou && terminou() && quantos(ENVIADO) > 0) {
            vibrou = true;
            vibrar();
        }
    }

    // ------------------------------------------------------------------ redes manuais

    /** Abre a conversa da pessoa com tudo pronto (ou o texto copiado para colar). */
    void abrir(Item it) {
        String rede = it.rede;
        Dados.Contato c = it.contato;
        String txt = textoPara(c);
        String pkg = Redes.pacote(rede);
        Intent i;

        if (midia != null) {
            i = new Intent(Intent.ACTION_SEND);
            i.setType(midia.mime);
            i.putExtra(Intent.EXTRA_STREAM, ArquivoProvider.uri(midia));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!txt.isEmpty()) i.putExtra(Intent.EXTRA_TEXT, txt);
            if (Redes.WHATSAPP.equals(rede)) {
                // Faz o WhatsApp abrir direto na conversa da pessoa.
                i.putExtra("jid", numeroInternacional(c.telefone) + "@s.whatsapp.net");
            } else if (Redes.SMS.equals(rede)) {
                i.putExtra("address", c.telefone);
                i.putExtra("sms_body", txt);
                pkg = null;
            }
        } else if (Redes.WHATSAPP.equals(rede)) {
            i = ver("https://api.whatsapp.com/send?phone=" + numeroInternacional(c.telefone)
                    + "&text=" + codificar(txt));
        } else {
            i = ver(enderecoPerfil(rede, c.usuario));
        }

        // Fora WhatsApp e SMS, nenhuma rede aceita texto pronto: deixamos copiado para colar.
        boolean copiar = !Redes.WHATSAPP.equals(rede) && !Redes.SMS.equals(rede) && !txt.isEmpty();
        if (copiar) {
            ClipboardManager cb = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            cb.setPrimaryClip(ClipData.newPlainText("mensagem", txt));
        }

        List<String> tentativas = new ArrayList<String>();
        tentativas.add(pkg);
        if (Redes.WHATSAPP.equals(rede)) tentativas.add("com.whatsapp.w4b");
        if (pkg != null && midia == null) tentativas.add(null); // abre no navegador
        for (String p : tentativas) {
            i.setPackage(p);
            try {
                a.startActivity(i);
                it.estado = ENVIADO;
                voltandoDeConversa = true;
                if (copiar) {
                    android.widget.Toast.makeText(a, L.t("Mensagem copiada: é só colar e enviar",
                            "Message copied: just paste and send"), android.widget.Toast.LENGTH_LONG).show();
                }
                return;
            } catch (ActivityNotFoundException e) {
                // tenta o próximo
            }
        }
        it.estado = FALHOU;
        it.erro = rede + L.t(" não está instalado", " is not installed");
        mudou();
    }

    private static Intent ver(String url) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    private static String enderecoPerfil(String rede, String usuario) {
        String u = usuario.trim().replaceFirst("^@", "");
        if (Redes.INSTAGRAM.equals(rede)) return "https://ig.me/m/" + u;
        if (Redes.FACEBOOK.equals(rede)) return "https://m.me/" + u;
        if (Redes.SNAPCHAT.equals(rede)) return "https://www.snapchat.com/add/" + u;
        if (Redes.X.equals(rede)) return "https://x.com/" + u;
        return "https://t.me/" + u;
    }

    /** Só dígitos; sem código do país (até 11 dígitos), assume Brasil. */
    static String numeroInternacional(String telefone) {
        String n = telefone.replaceAll("[^0-9]", "");
        if (telefone.trim().startsWith("+")) return n;
        n = n.replaceFirst("^0+", "");
        if (n.length() <= 11) n = "55" + n;
        return n;
    }

    private static String codificar(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ SMS

    @SuppressWarnings("deprecation")
    private void enviarSms(Item it) {
        try {
            SmsManager sm = SmsManager.getDefault();
            sm.sendMultipartTextMessage(it.contato.telefone, null, sm.divideMessage(textoPara(it.contato)), null, null);
            it.estado = ENVIADO;
        } catch (Exception e) {
            it.estado = FALHOU;
            it.erro = L.t("SMS falhou", "SMS failed");
        }
    }

    // ------------------------------------------------------------------ Telegram

    private void enviarTelegram(final List<Item> lista) {
        final String token = d.telegramToken.trim();
        if (token.isEmpty()) {
            for (Item it : lista) {
                it.estado = FALHOU;
                it.erro = L.t("configure o bot em Contas vinculadas", "set up the bot in Linked accounts");
            }
            return;
        }
        final Dados.Midia md = midia;
        final File arquivo = md != null ? new File(d.pastaMidias(), md.arquivo) : null;
        for (Item it : lista) it.estado = ENVIANDO;
        new Thread(new Runnable() {
            public void run() {
                for (final Item it : lista) {
                    int estado;
                    String erro = "";
                    try {
                        String texto = textoPara(it.contato);
                        JSONObject r;
                        if (arquivo != null) {
                            r = new JSONObject(enviarArquivoTelegram(token, it.contato.usuario.trim(), texto, md, arquivo));
                        } else {
                            r = new JSONObject(http("https://api.telegram.org/bot" + token + "/sendMessage",
                                    "chat_id=" + codificar(it.contato.usuario.trim()) + "&text=" + codificar(texto)));
                        }
                        estado = r.optBoolean("ok") ? ENVIADO : FALHOU;
                        if (estado == FALHOU) erro = r.optString("description");
                    } catch (Exception e) {
                        estado = FALHOU;
                        erro = L.t("sem conexão", "no connection");
                    }
                    final int estadoFinal = estado;
                    final String erroFinal = erro;
                    a.runOnUiThread(new Runnable() {
                        public void run() {
                            it.estado = estadoFinal;
                            it.erro = erroFinal;
                            mudou();
                        }
                    });
                }
            }
        }).start();
    }

    private static String enviarArquivoTelegram(String token, String chat, String legenda,
                                                Dados.Midia md, File f) throws Exception {
        String metodo, campo;
        if (md.mime.startsWith("image/") && !md.mime.equals("image/gif")) {
            metodo = "sendPhoto";
            campo = "photo";
        } else if (md.mime.startsWith("video/")) {
            metodo = "sendVideo";
            campo = "video";
        } else if (md.mime.startsWith("audio/")) {
            metodo = "sendAudio";
            campo = "audio";
        } else {
            metodo = "sendDocument";
            campo = "document";
        }
        String limite = "----chegou" + System.currentTimeMillis();
        HttpURLConnection con = (HttpURLConnection) new URL(
                "https://api.telegram.org/bot" + token + "/" + metodo).openConnection();
        con.setConnectTimeout(20000);
        con.setReadTimeout(60000);
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + limite);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        parte(out, limite, "chat_id", chat);
        if (!legenda.isEmpty()) parte(out, limite, "caption", legenda);
        out.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + campo
                + "\"; filename=\"" + md.arquivo + "\"\r\nContent-Type: " + md.mime + "\r\n\r\n").getBytes("UTF-8"));
        FileInputStream in = new FileInputStream(f);
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.write(("\r\n--" + limite + "--\r\n").getBytes("UTF-8"));
        out.close();
        return ler(con);
    }

    private static void parte(DataOutputStream out, String limite, String nome, String valor) throws Exception {
        out.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + nome + "\"\r\n\r\n")
                .getBytes("UTF-8"));
        out.write(valor.getBytes("UTF-8"));
        out.write("\r\n".getBytes("UTF-8"));
    }

    /** GET quando corpo == null, senão POST form-urlencoded. Retorna o corpo da resposta. */
    static String http(String endereco, String corpo) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(endereco).openConnection();
        con.setConnectTimeout(15000);
        con.setReadTimeout(15000);
        if (corpo != null) {
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            OutputStream out = con.getOutputStream();
            out.write(corpo.getBytes("UTF-8"));
            out.close();
        }
        return ler(con);
    }

    private static String ler(HttpURLConnection con) throws Exception {
        InputStream in = con.getResponseCode() < 400 ? con.getInputStream() : con.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String linha;
        while ((linha = br.readLine()) != null) sb.append(linha);
        br.close();
        con.disconnect();
        return sb.toString();
    }

    // ------------------------------------------------------------------ retorno

    @SuppressWarnings("deprecation")
    private void vibrar() {
        if (!d.vibrar) return;
        Vibrator v = (Vibrator) a.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) v.vibrate(80);
    }
}
