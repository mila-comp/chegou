package com.mensagemrapida;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Tudo que o app guarda no celular: conta, contatos, mensagens, mídias e configurações. */
final class Dados {

    static final int LIMITE_TEXTO = 1000;
    static final long LIMITE_MIDIA = 10L * 1024 * 1024; // 10 MB

    static class Conta {
        String nome, email, hash, sal;
    }

    static class Contato {
        String id, nome, telefone = "", usuario = "", meio;
        long criado;
    }

    static class Mensagem {
        String id, nome, texto = "", rede = "", midia = "";
        long criado;
        List<String> destinatarios = new ArrayList<String>();

        // Lembrete: notificação com botão Enviar dentro de uma janela de horário.
        boolean lembrete;
        int lembreteInicio = 16 * 60, lembreteFim = 19 * 60; // minutos desde a meia-noite
        int lembreteDias = 127; // um bit por dia, domingo = bit 0
        int lembreteFeito; // dia (aaaammdd) em que já foi enviado ou dispensado
    }

    static class Midia {
        String id, nome, arquivo, mime;
        long tamanho, criado;
    }

    /** Um envio feito (para o histórico). */
    static class Registro {
        long quando;
        String nome;
        int total;
    }

    private static Dados instancia;

    static Dados de(Context c) {
        if (instancia == null) instancia = new Dados(c.getApplicationContext());
        return instancia;
    }

    final Context ctx;
    private final SharedPreferences p;

    Conta conta;
    boolean logado;
    final List<Contato> contatos = new ArrayList<Contato>();
    final List<Mensagem> mensagens = new ArrayList<Mensagem>();
    final List<Midia> midias = new ArrayList<Midia>();
    String ativa = "";
    final List<Registro> historico = new ArrayList<Registro>();
    boolean confirmar = true; // perguntar antes de enviar
    boolean whatsJunto = true; // WhatsApp: abrir uma vez só e marcar todos lá
    int cor; // cor principal escolhida (índice em Ui.CORES)

    boolean escuro, contraste, vibrar = true;
    int fonte; // 0 normal, 1 grande, 2 muito grande
    String idioma = "pt";
    String telegramToken = "";

    private Dados(Context ctx) {
        this.ctx = ctx;
        p = ctx.getSharedPreferences("chegou", Context.MODE_PRIVATE);
        carregar();
        migrarVersaoAntiga();
        L.ingles = "en".equals(idioma);
    }

    // ------------------------------------------------------------------ consultas

    Contato contato(String id) {
        for (Contato c : contatos) if (c.id.equals(id)) return c;
        return null;
    }

    Mensagem mensagem(String id) {
        for (Mensagem m : mensagens) if (m.id.equals(id)) return m;
        return null;
    }

    Midia midia(String id) {
        for (Midia m : midias) if (m.id.equals(id)) return m;
        return null;
    }

    Midia midiaPorArquivo(String arquivo) {
        for (Midia m : midias) if (m.arquivo.equals(arquivo)) return m;
        return null;
    }

    /** A mensagem mostrada no Início: a escolhida por último, ou a mais recente. */
    Mensagem mensagemAtiva() {
        Mensagem m = mensagem(ativa);
        if (m == null && !mensagens.isEmpty()) m = mensagens.get(mensagens.size() - 1);
        return m;
    }

    File pastaMidias() {
        File f = new File(ctx.getFilesDir(), "midias");
        f.mkdirs();
        return f;
    }

    File arquivoFoto() {
        return new File(ctx.getFilesDir(), "foto.jpg");
    }

    /** Copia um arquivo escolhido pelo usuário para dentro do app. Retorna o tamanho, ou -1. */
    long copiar(android.net.Uri origem, File destino, long limite) {
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = ctx.getContentResolver().openInputStream(origem);
            out = new java.io.FileOutputStream(destino);
            byte[] buf = new byte[16384];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > limite) throw new java.io.IOException("grande demais");
                out.write(buf, 0, n);
            }
            return total;
        } catch (Exception e) {
            destino.delete();
            return -1;
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) { }
            try { if (out != null) out.close(); } catch (Exception e) { }
        }
    }

    static String novoId() {
        return UUID.randomUUID().toString();
    }

    static String data(long t) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(t));
    }

    static String hora(int minutos) {
        return String.format(Locale.US, "%02d:%02d", minutos / 60, minutos % 60);
    }

    static String dataHora(long t) {
        return new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(t));
    }

    void registrarEnvio(String nome, int total) {
        Registro r = new Registro();
        r.quando = System.currentTimeMillis();
        r.nome = nome;
        r.total = total;
        historico.add(0, r);
        while (historico.size() > 50) historico.remove(historico.size() - 1);
        salvar();
    }

    static String dataCurta(long t) {
        return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(t));
    }

    static String tamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ------------------------------------------------------------------ alterações

    void excluirContato(Contato c) {
        contatos.remove(c);
        for (Mensagem m : mensagens) m.destinatarios.remove(c.id);
        salvar();
    }

    void excluirMensagem(Mensagem m) {
        mensagens.remove(m);
        salvar();
    }

    void excluirMidia(Midia m) {
        midias.remove(m);
        new File(pastaMidias(), m.arquivo).delete();
        for (Mensagem msg : mensagens) if (m.id.equals(msg.midia)) msg.midia = "";
        salvar();
    }

    void criarConta(String nome, String email, String senha) {
        conta = new Conta();
        conta.nome = nome;
        conta.email = email;
        definirSenha(senha);
        logado = true;
        salvar();
    }

    void definirSenha(String senha) {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        conta.sal = hex(b);
        conta.hash = hash(conta.sal, senha);
    }

    boolean senhaConfere(String email, String senha) {
        return conta != null && conta.email.equalsIgnoreCase(email.trim())
                && conta.hash.equals(hash(conta.sal, senha));
    }

    /** Apaga absolutamente tudo e volta o app ao estado de recém-instalado. */
    void excluirTudo() {
        for (Midia m : new ArrayList<Midia>(midias)) new File(pastaMidias(), m.arquivo).delete();
        arquivoFoto().delete();
        conta = null;
        logado = false;
        contatos.clear();
        mensagens.clear();
        midias.clear();
        historico.clear();
        ativa = "";
        telegramToken = "";
        salvar();
    }

    private static String hash(String sal, String senha) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest((sal + senha).getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    // ------------------------------------------------------------------ gravação

    /** Grava e avisa o widget e os lembretes de que algo mudou. */
    void salvar() {
        gravar();
        L.ingles = "en".equals(idioma);
        WidgetEnviar.atualizar(ctx);
        Lembretes.reagendar(ctx);
    }

    /** Só grava no celular. */
    private void gravar() {
        try {
            JSONObject o = new JSONObject();
            if (conta != null) {
                JSONObject c = new JSONObject();
                c.put("nome", conta.nome);
                c.put("email", conta.email);
                c.put("hash", conta.hash);
                c.put("sal", conta.sal);
                o.put("conta", c);
            }
            o.put("logado", logado);
            o.put("ativa", ativa);
            o.put("escuro", escuro);
            o.put("contraste", contraste);
            o.put("vibrar", vibrar);
            o.put("fonte", fonte);
            o.put("idioma", idioma);
            o.put("telegramToken", telegramToken);
            o.put("confirmarEnvio", confirmar);
            o.put("whatsJunto", whatsJunto);
            o.put("cor", cor);
            JSONArray hs = new JSONArray();
            for (Registro r : historico) {
                JSONObject j = new JSONObject();
                j.put("quando", r.quando);
                j.put("nome", r.nome);
                j.put("total", r.total);
                hs.put(j);
            }
            o.put("historico", hs);

            JSONArray cs = new JSONArray();
            for (Contato c : contatos) {
                JSONObject j = new JSONObject();
                j.put("id", c.id);
                j.put("nome", c.nome);
                j.put("telefone", c.telefone);
                j.put("usuario", c.usuario);
                j.put("meio", c.meio);
                j.put("criado", c.criado);
                cs.put(j);
            }
            o.put("contatos", cs);

            JSONArray ms = new JSONArray();
            for (Mensagem m : mensagens) {
                JSONObject j = new JSONObject();
                j.put("id", m.id);
                j.put("nome", m.nome);
                j.put("texto", m.texto);
                j.put("rede", m.rede);
                j.put("midia", m.midia);
                j.put("criado", m.criado);
                j.put("destinatarios", new JSONArray(m.destinatarios));
                j.put("lembrete", m.lembrete);
                j.put("lembreteInicio", m.lembreteInicio);
                j.put("lembreteFim", m.lembreteFim);
                j.put("lembreteDias", m.lembreteDias);
                j.put("lembreteFeito", m.lembreteFeito);
                ms.put(j);
            }
            o.put("mensagens", ms);

            JSONArray md = new JSONArray();
            for (Midia m : midias) {
                JSONObject j = new JSONObject();
                j.put("id", m.id);
                j.put("nome", m.nome);
                j.put("arquivo", m.arquivo);
                j.put("mime", m.mime);
                j.put("tamanho", m.tamanho);
                j.put("criado", m.criado);
                md.put(j);
            }
            o.put("midias", md);

            p.edit().putString("dados", o.toString()).apply();
        } catch (Exception e) {
            // não deveria acontecer: só gravamos tipos simples
        }
    }

    private void carregar() {
        try {
            JSONObject o = new JSONObject(p.getString("dados", "{}"));
            JSONObject c = o.optJSONObject("conta");
            if (c != null) {
                conta = new Conta();
                conta.nome = c.getString("nome");
                conta.email = c.getString("email");
                conta.hash = c.getString("hash");
                conta.sal = c.getString("sal");
            }
            logado = o.optBoolean("logado", false) && conta != null;
            ativa = o.optString("ativa", "");
            escuro = o.optBoolean("escuro", false);
            contraste = o.optBoolean("contraste", false);
            vibrar = o.optBoolean("vibrar", true);
            fonte = o.optInt("fonte", 0);
            idioma = o.optString("idioma", "pt");
            telegramToken = o.optString("telegramToken", "");
            confirmar = o.optBoolean("confirmarEnvio", true);
            whatsJunto = o.optBoolean("whatsJunto", true);
            cor = o.optInt("cor", 0);
            JSONArray hs = o.optJSONArray("historico");
            for (int i = 0; hs != null && i < hs.length(); i++) {
                JSONObject j = hs.getJSONObject(i);
                Registro r = new Registro();
                r.quando = j.optLong("quando");
                r.nome = j.optString("nome");
                r.total = j.optInt("total");
                historico.add(r);
            }

            JSONArray cs = o.optJSONArray("contatos");
            for (int i = 0; cs != null && i < cs.length(); i++) {
                JSONObject j = cs.getJSONObject(i);
                Contato x = new Contato();
                x.id = j.getString("id");
                x.nome = j.getString("nome");
                x.telefone = j.optString("telefone", "");
                x.usuario = j.optString("usuario", "");
                x.meio = j.optString("meio", Redes.WHATSAPP);
                x.criado = j.optLong("criado");
                contatos.add(x);
            }
            JSONArray ms = o.optJSONArray("mensagens");
            for (int i = 0; ms != null && i < ms.length(); i++) {
                JSONObject j = ms.getJSONObject(i);
                Mensagem x = new Mensagem();
                x.id = j.getString("id");
                x.nome = j.getString("nome");
                x.texto = j.optString("texto", "");
                x.rede = j.optString("rede", "");
                x.midia = j.optString("midia", "");
                x.criado = j.optLong("criado");
                x.lembrete = j.optBoolean("lembrete", false);
                x.lembreteInicio = j.optInt("lembreteInicio", 16 * 60);
                x.lembreteFim = j.optInt("lembreteFim", 19 * 60);
                x.lembreteDias = j.optInt("lembreteDias", 127);
                x.lembreteFeito = j.optInt("lembreteFeito", 0);
                JSONArray d = j.optJSONArray("destinatarios");
                for (int k = 0; d != null && k < d.length(); k++) x.destinatarios.add(d.getString(k));
                mensagens.add(x);
            }
            JSONArray md = o.optJSONArray("midias");
            for (int i = 0; md != null && i < md.length(); i++) {
                JSONObject j = md.getJSONObject(i);
                Midia x = new Midia();
                x.id = j.getString("id");
                x.nome = j.getString("nome");
                x.arquivo = j.getString("arquivo");
                x.mime = j.optString("mime", "application/octet-stream");
                x.tamanho = j.optLong("tamanho");
                x.criado = j.optLong("criado");
                midias.add(x);
            }
        } catch (Exception e) {
            // dados corrompidos: começa vazio
        }
    }

    /** Traz os contatos e a mensagem da versão 1.x (sem telas) para o formato novo. */
    private void migrarVersaoAntiga() {
        SharedPreferences antigo = ctx.getSharedPreferences("dados", Context.MODE_PRIVATE);
        if (!antigo.contains("contatos") && !antigo.contains("mensagem")) return;
        try {
            long agora = System.currentTimeMillis();
            JSONArray arr = new JSONArray(antigo.getString("contatos", "[]"));
            Mensagem m = new Mensagem();
            m.id = novoId();
            m.nome = L.t("Minha mensagem", "My message");
            m.texto = antigo.getString("mensagem", "");
            m.criado = agora;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Contato c = new Contato();
                c.id = novoId();
                c.nome = o.getString("nome");
                c.meio = o.getString("canal");
                c.criado = agora;
                if (Redes.TELEGRAM.equals(c.meio)) c.usuario = o.getString("destino");
                else c.telefone = o.getString("destino");
                contatos.add(c);
                if (o.optBoolean("selecionado", true)) m.destinatarios.add(c.id);
            }
            if (!m.texto.isEmpty() || !m.destinatarios.isEmpty()) {
                mensagens.add(m);
                ativa = m.id;
            }
            if (telegramToken.isEmpty()) telegramToken = antigo.getString("telegram_token", "");
            // gravar(), não salvar(): ainda estamos no construtor, e o widget e os lembretes
            // pediriam Dados.de() de novo, criando outro Dados sem fim.
            gravar();
            antigo.edit().clear().apply();
        } catch (Exception e) {
            // se não der para migrar, segue com os dados novos
        }
    }
}
