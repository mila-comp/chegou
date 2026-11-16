package com.mensagemrapida;

import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Tela 5 ("Salvar novas"): Novo contato / Nova mídia / Nova mensagem. */
class TelaSalvarNovo extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Salvar novo", "Save new")));
        String[] nomes = {L.t("Novo contato", "New contact"), L.t("Nova mídia", "New media"),
                L.t("Nova mensagem", "New message")};
        for (int i = 0; i < nomes.length; i++) {
            final int qual = i;
            LinearLayout item = Ui.itemMenu(a, nomes[i], "›", Ui.texto);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (qual == 0) a.abrir(new TelaFormContato(null));
                    else if (qual == 1) a.abrir(new TelaFormMidia());
                    else a.abrir(new TelaFormMensagem(null));
                }
            });
            col.addView(item);
        }
        return Ui.rolagem(a, col);
    }
}

/** Tela 6 ("Novo contato") — também usada para Editar. */
class TelaFormContato extends Tela {
    private final Dados.Contato existente;
    private String meio;
    private EditText nome, telefone, usuario;

    TelaFormContato(Dados.Contato c) {
        existente = c;
    }

    @Override
    View criar() {
        // Guarda o que já foi digitado caso a tela seja redesenhada (ex.: troca do meio).
        String n = nome != null ? nome.getText().toString() : existente != null ? existente.nome : "";
        String t = telefone != null ? telefone.getText().toString() : existente != null ? existente.telefone : "";
        String u = usuario != null ? usuario.getText().toString() : existente != null ? existente.usuario : "";
        if (meio == null) meio = existente != null ? existente.meio : null;

        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(existente == null ? L.t("Novo contato", "New contact")
                : L.t("Editar contato", "Edit contact")));

        col.addView(Ui.rotulo(a, L.t("Nome:", "Name:")));
        nome = Ui.campo(a, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nome.setText(n);
        col.addView(nome);

        col.addView(Ui.rotulo(a, L.t("Telefone:", "Phone:")));
        telefone = Ui.campo(a, "(DDD) 91234-5678", InputType.TYPE_CLASS_PHONE);
        telefone.setText(t);
        col.addView(telefone);
        TextView agenda = Ui.link(a, L.t("Escolher da agenda", "Pick from contacts"));
        agenda.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherDaAgenda(); }
        });
        col.addView(agenda);

        col.addView(Ui.rotulo(a, L.t("Principal meio de comunicação:", "Main way to reach them:")));
        TextView sel = Ui.seletor(a, meio == null ? L.t("SELECIONA", "SELECT") : meio);
        if (meio != null) sel.setTextColor(Redes.cor(meio));
        sel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.dialogo().setTitle(L.t("Meio de comunicação", "Main app"))
                        .setItems(Redes.TODAS, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int i) {
                                meio = Redes.TODAS[i];
                                a.desenhar();
                            }
                        }).show();
            }
        });
        col.addView(sel);

        usuario = Ui.campo(a, "", InputType.TYPE_CLASS_TEXT);
        usuario.setText(u);
        if (meio != null && !Redes.usaTelefone(meio)) {
            col.addView(Ui.rotulo(a, Redes.rotuloUsuario(meio) + ":"));
            usuario.setHint(Redes.TELEGRAM.equals(meio) ? "123456789" : "@usuario");
            col.addView(usuario);
            if (Redes.TELEGRAM.equals(meio)) {
                TextView buscar = Ui.link(a, L.t("Buscar no meu bot do Telegram", "Find in my Telegram bot"));
                buscar.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { buscarNoBot(); }
                });
                col.addView(buscar);
            }
        }

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView salvar = Ui.botaoCheio(a, L.t("Salvar", "Save"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        salvar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { salvar(); }
        });
        col.addView(Ui.barraBotoes(a, voltar, salvar));
        return Ui.rolagem(a, col);
    }

    private void salvar() {
        String n = nome.getText().toString().trim();
        String t = telefone.getText().toString().trim();
        String u = usuario.getText().toString().trim();
        if (n.isEmpty()) {
            a.aviso(L.t("Digite o nome.", "Enter the name."));
            return;
        }
        if (meio == null) {
            a.aviso(L.t("Escolha o meio de comunicação.", "Choose the main app."));
            return;
        }
        if (Redes.usaTelefone(meio) && t.isEmpty()) {
            a.aviso(L.t("Digite o telefone.", "Enter the phone number."));
            return;
        }
        if (!Redes.usaTelefone(meio) && u.isEmpty()) {
            a.aviso(L.t("Preencha: ", "Fill in: ") + Redes.rotuloUsuario(meio));
            return;
        }
        Dados.Contato c = existente;
        if (c == null) {
            c = new Dados.Contato();
            c.id = Dados.novoId();
            c.criado = System.currentTimeMillis();
            d.contatos.add(c);
        }
        c.nome = n;
        c.telefone = t;
        c.usuario = u;
        c.meio = meio;
        d.salvar();
        a.voltar();
        a.aviso(L.t("Contato salvo!", "Contact saved!"));
    }

    private void escolherDaAgenda() {
        a.escolherTelefone(new MainActivity.ArquivoEscolhido() {
            public void escolhido(Uri uri) {
                Cursor cur = null;
                try {
                    cur = a.getContentResolver().query(uri, new String[]{
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null, null);
                    if (cur != null && cur.moveToFirst()) {
                        telefone.setText(cur.getString(0));
                        if (nome.getText().toString().trim().isEmpty()) nome.setText(cur.getString(1));
                    }
                } catch (Exception e) {
                    a.aviso(L.t("Não foi possível ler o contato.", "Couldn't read the contact."));
                } finally {
                    if (cur != null) cur.close();
                }
            }
        });
    }

    /** Lista quem já mandou mensagem para o bot e preenche o Chat ID escolhido. */
    private void buscarNoBot() {
        if (d.telegramToken.trim().isEmpty()) {
            a.aviso(L.t("Configure o bot em Contas vinculadas > Telegram.",
                    "Set up the bot in Linked accounts > Telegram."));
            return;
        }
        a.aviso(L.t("Buscando…", "Searching…"));
        TelegramBot.buscarChats(a, d.telegramToken, new TelegramBot.Chats() {
            public void encontrados(final List<String[]> chats) {
                if (chats.isEmpty()) {
                    a.dialogo().setMessage(TelegramBot.textoNenhum()).setPositiveButton("OK", null).show();
                    return;
                }
                String[] nomes = new String[chats.size()];
                for (int i = 0; i < nomes.length; i++) nomes[i] = chats.get(i)[1];
                a.dialogo().setTitle(L.t("Quem é?", "Who is it?"))
                        .setItems(nomes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int i) {
                                usuario.setText(chats.get(i)[0]);
                                if (nome.getText().toString().trim().isEmpty()) nome.setText(chats.get(i)[1]);
                            }
                        }).show();
            }
        });
    }
}

/** Tela 7 ("Nova mídia"): escolher arquivo (até 10 MB) e dar um nome. */
class TelaFormMidia extends Tela {
    private Uri arquivo;
    private String nomeArquivo, mime;
    private long tamanho;
    private EditText nome;

    @Override
    View criar() {
        String n = nome != null ? nome.getText().toString() : "";
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Nova mídia", "New media")));

        col.addView(Ui.rotulo(a, L.t("Selecione a mídia:", "Select the media:")));
        TextView sel = Ui.seletor(a, arquivo == null ? L.t("Escolher arquivo", "Choose file")
                : nomeArquivo + "  (" + Dados.tamanho(tamanho) + ")");
        sel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolher(); }
        });
        col.addView(sel);
        TextView limite = Ui.texto(a, L.t("até 10 MB · foto, vídeo, áudio ou documento",
                "up to 10 MB · photo, video, audio or document"), 12, Ui.textoSec);
        limite.setGravity(android.view.Gravity.END);
        col.addView(limite);

        col.addView(Ui.rotulo(a, L.t("Nome para salvar:", "Name to save as:")));
        nome = Ui.campo(a, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nome.setText(n);
        col.addView(nome);

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView salvar = Ui.botaoCheio(a, L.t("Salvar", "Save"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        salvar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { salvar(); }
        });
        col.addView(Ui.barraBotoes(a, voltar, salvar));
        return Ui.rolagem(a, col);
    }

    private void escolher() {
        a.escolherArquivo("*/*", new MainActivity.ArquivoEscolhido() {
            public void escolhido(Uri uri) {
                Cursor c = null;
                try {
                    c = a.getContentResolver().query(uri, null, null, null, null);
                    if (c != null && c.moveToFirst()) {
                        nomeArquivo = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        tamanho = c.getLong(c.getColumnIndex(OpenableColumns.SIZE));
                    }
                } catch (Exception e) {
                    nomeArquivo = "arquivo";
                } finally {
                    if (c != null) c.close();
                }
                if (tamanho > Dados.LIMITE_MIDIA) {
                    a.aviso(L.t("Arquivo grande demais (máximo 10 MB).", "File too large (max 10 MB)."));
                    return;
                }
                arquivo = uri;
                if (nomeArquivo == null) nomeArquivo = "arquivo";
                mime = a.getContentResolver().getType(uri);
                if (mime == null) {
                    String ext = android.webkit.MimeTypeMap.getFileExtensionFromUrl(nomeArquivo);
                    mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
                }
                if (mime == null) mime = "application/octet-stream";
                if (nome.getText().toString().trim().isEmpty()) {
                    nome.setText(nomeArquivo.replaceFirst("\\.[^.]*$", ""));
                }
                a.desenhar();
            }
        });
    }

    private void salvar() {
        String n = nome.getText().toString().trim();
        if (arquivo == null) {
            a.aviso(L.t("Escolha um arquivo.", "Choose a file."));
            return;
        }
        if (n.isEmpty()) {
            a.aviso(L.t("Dê um nome para a mídia.", "Give the media a name."));
            return;
        }
        Dados.Midia m = new Dados.Midia();
        m.id = Dados.novoId();
        String ext = nomeArquivo.contains(".") ? nomeArquivo.substring(nomeArquivo.lastIndexOf('.')) : "";
        m.arquivo = m.id.substring(0, 8) + ext.replaceAll("[^A-Za-z0-9.]", "");
        File destino = new File(d.pastaMidias(), m.arquivo);
        long t = d.copiar(arquivo, destino, Dados.LIMITE_MIDIA);
        if (t < 0) {
            a.aviso(L.t("Não foi possível salvar o arquivo.", "Couldn't save the file."));
            return;
        }
        m.nome = n;
        m.mime = mime;
        m.tamanho = t;
        m.criado = System.currentTimeMillis();
        d.midias.add(m);
        d.salvar();
        a.voltar();
        a.aviso(L.t("Mídia salva! Anexe a uma mensagem em Info.", "Media saved! Attach it to a message in Info."));
    }
}

/** Telas 8 e 15: escrever a mensagem (até 1000 caracteres) e dar um nome. */
class TelaFormMensagem extends Tela {
    private final Dados.Mensagem existente;
    private EditText texto, nome;

    TelaFormMensagem(Dados.Mensagem m) {
        existente = m;
    }

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(existente == null ? L.t("Nova mensagem", "New message")
                : L.t("Editar mensagem", "Edit message")));

        col.addView(Ui.rotulo(a, L.t("Mensagem:", "Message:")));
        String t0 = texto != null ? texto.getText().toString() : existente != null ? existente.texto : "";
        String n0 = nome != null ? nome.getText().toString() : existente != null ? existente.nome : "";
        texto = new EditText(a);
        texto.setHint(L.t("Digite aqui", "Type here"));
        col.addView(Ui.areaTexto(a, texto, Dados.LIMITE_TEXTO));
        texto.setText(t0);

        // {nome} vira o primeiro nome de cada pessoa na hora do envio.
        LinearLayout dica = new LinearLayout(a);
        dica.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView inserir = Ui.chip(a, "+ {nome}", false);
        inserir.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int pos = Math.max(texto.getSelectionStart(), 0);
                texto.getText().insert(pos, "{nome}");
            }
        });
        dica.addView(inserir);
        TextView explica = Ui.texto(a, L.t("vira o nome de cada pessoa", "becomes each person's name"), 13, Ui.textoSec);
        explica.setPadding(Ui.dp(10), 0, 0, 0);
        dica.addView(explica);
        col.addView(dica);

        col.addView(Ui.rotulo(a, L.t("Nome da mensagem:", "Message name:")));
        nome = Ui.campo(a, L.t("Ex.: Bom dia", "E.g.: Good morning"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nome.setText(n0);
        col.addView(nome);

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView salvar = Ui.botaoCheio(a, L.t("Salvar", "Save"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        salvar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { salvar(); }
        });
        col.addView(Ui.barraBotoes(a, voltar, salvar));
        return Ui.rolagem(a, col);
    }

    private void salvar() {
        String t = texto.getText().toString().trim();
        String n = nome.getText().toString().trim();
        if (t.isEmpty()) {
            a.aviso(L.t("Escreva a mensagem.", "Write the message."));
            return;
        }
        if (n.isEmpty()) {
            // Sem nome: usa o começo do texto.
            n = t.length() > 24 ? t.substring(0, 24).trim() + "…" : t;
        }
        Dados.Mensagem m = existente;
        if (m == null) {
            m = new Dados.Mensagem();
            m.id = Dados.novoId();
            m.criado = System.currentTimeMillis();
            d.mensagens.add(m);
        }
        m.texto = t;
        m.nome = n;
        d.ativa = m.id;
        d.salvar();
        if (existente == null) {
            // Mensagem nova: segue direto para escolher quem recebe.
            a.trocar(new TelaInfoMensagem(m));
            a.aviso(L.t("Salva! Agora escolha para quem enviar.", "Saved! Now choose who gets it."));
        } else {
            a.voltar();
            a.aviso(L.t("Mensagem salva!", "Message saved!"));
        }
    }
}

/** Busca no Telegram quem já conversou com o bot do usuário. */
final class TelegramBot {
    interface Chats {
        /** Cada item: {chat_id, nome}. */
        void encontrados(List<String[]> chats);
    }

    static String textoNenhum() {
        return L.t("Ninguém encontrado. Peça para a pessoa abrir o seu bot no Telegram e mandar qualquer "
                        + "mensagem, depois tente de novo (o Telegram só guarda as últimas 24 horas).",
                "Nobody found. Ask the person to open your bot in Telegram and send any message, then try "
                        + "again (Telegram only keeps the last 24 hours).");
    }

    static void buscarChats(final MainActivity a, final String token, final Chats cb) {
        new Thread(new Runnable() {
            public void run() {
                final List<String[]> achados = new ArrayList<String[]>();
                String erro = null;
                try {
                    JSONObject r = new JSONObject(Envio.http(
                            "https://api.telegram.org/bot" + token.trim() + "/getUpdates", null));
                    if (!r.optBoolean("ok")) {
                        erro = L.t("Token inválido: ", "Invalid token: ") + r.optString("description");
                    } else {
                        JSONArray up = r.getJSONArray("result");
                        for (int i = 0; i < up.length(); i++) {
                            JSONObject msg = up.getJSONObject(i).optJSONObject("message");
                            if (msg == null) continue;
                            JSONObject chat = msg.getJSONObject("chat");
                            String id = String.valueOf(chat.getLong("id"));
                            boolean repetido = false;
                            for (String[] x : achados) if (x[0].equals(id)) repetido = true;
                            if (repetido) continue;
                            String n = (chat.optString("first_name", "") + " " + chat.optString("last_name", "")).trim();
                            if (n.isEmpty()) n = chat.optString("title", id);
                            achados.add(new String[]{id, n});
                        }
                    }
                } catch (Exception e) {
                    erro = L.t("Sem conexão com o Telegram.", "No connection to Telegram.");
                }
                final String erroFinal = erro;
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        if (erroFinal != null) a.aviso(erroFinal);
                        else cb.encontrados(achados);
                    }
                });
            }
        }).start();
    }

    private TelegramBot() { }
}
