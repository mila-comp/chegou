package com.mensagemrapida;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Tela 18 ("Contas vinculadas"): cada rede e se está pronta para uso. */
class TelaContas extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Contas vinculadas", "Linked accounts")));
        col.addView(Ui.texto(a, L.t("O Chegou usa os apps instalados no seu celular. Toque numa rede para "
                        + "configurar ou instalar.",
                "Chegou uses the apps installed on your phone. Tap a network to set it up or install it."),
                13, Ui.textoSec), Ui.margemBaixo(8));

        for (final String rede : Redes.TODAS) {
            String estado;
            if (Redes.TELEGRAM.equals(rede)) {
                estado = d.telegramToken.trim().isEmpty() ? L.t("configurar bot ›", "set up bot ›")
                        : L.t("bot pronto ✓", "bot ready ✓");
            } else if (Redes.SMS.equals(rede)) {
                estado = a.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                        ? L.t("permitido ✓", "allowed ✓") : L.t("permitir ›", "allow ›");
            } else {
                estado = Redes.instalado(a, rede) ? L.t("instalado ✓", "installed ✓")
                        : L.t("instalar ›", "install ›");
            }
            LinearLayout item = Ui.itemMenu(a, rede, estado, Redes.cor(rede));
            ((TextView) item.getChildAt(0)).setTypeface(Typeface.DEFAULT_BOLD);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { tocar(rede); }
            });
            col.addView(item);
        }

        TextView como = Ui.texto(a, L.t("Como cada uma envia:\n"
                        + "• SMS e Telegram: sozinhos, sem você tocar em nada.\n"
                        + "• WhatsApp: abre a conversa com a mensagem pronta; é só tocar em enviar.\n"
                        + "• Instagram, Facebook, X e Snapchat: abrem a conversa e a mensagem fica "
                        + "copiada para você colar. Essas redes não deixam outros apps enviarem por você.",
                "How each one sends:\n"
                        + "• SMS and Telegram: on their own, no taps needed.\n"
                        + "• WhatsApp: opens the chat with the message ready; just tap send.\n"
                        + "• Instagram, Facebook, X and Snapchat: open the chat with the message copied "
                        + "for you to paste. These networks don't let other apps send for you."),
                13, Ui.textoSec);
        como.setPadding(0, Ui.dp(20), 0, 0);
        col.addView(como);
        return Ui.rolagem(a, col);
    }

    private void tocar(String rede) {
        if (Redes.TELEGRAM.equals(rede)) {
            configurarTelegram();
        } else if (Redes.SMS.equals(rede)) {
            a.pedirPermissaoSms(new MainActivity.Resposta() {
                public void sim(boolean ok) { a.desenhar(); }
            });
        } else if (!Redes.instalado(a, rede)) {
            String pkg = Redes.pacote(rede);
            try {
                a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
            } catch (Exception e) {
                a.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }
        } else {
            a.aviso(rede + L.t(" está pronto para uso.", " is ready to use."));
        }
    }

    private void configurarTelegram() {
        LinearLayout form = new LinearLayout(a);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(24), Ui.dp(8), Ui.dp(24), 0);
        form.addView(Ui.texto(a, L.t("1. No Telegram, fale com @BotFather e envie /newbot.\n"
                        + "2. Cole aqui o token que ele te der.\n"
                        + "3. Peça para cada pessoa abrir o seu bot e mandar qualquer mensagem.\n"
                        + "4. Toque em \"Buscar contatos\" para adicioná-las.",
                "1. In Telegram, talk to @BotFather and send /newbot.\n"
                        + "2. Paste the token it gives you here.\n"
                        + "3. Ask each person to open your bot and send any message.\n"
                        + "4. Tap \"Find contacts\" to add them."), 14, Ui.texto));
        final EditText token = Ui.campo(a, L.t("Token do bot", "Bot token"), InputType.TYPE_CLASS_TEXT);
        token.setText(d.telegramToken);
        form.addView(token);

        a.dialogo()
                .setTitle(L.t("Bot do Telegram", "Telegram bot"))
                .setView(form)
                .setPositiveButton(L.t("Salvar", "Save"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        d.telegramToken = token.getText().toString().trim();
                        d.salvar();
                        a.desenhar();
                    }
                })
                .setNeutralButton(L.t("Buscar contatos", "Find contacts"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        d.telegramToken = token.getText().toString().trim();
                        d.salvar();
                        a.desenhar();
                        buscarContatos();
                    }
                })
                .setNegativeButton(L.t("Cancelar", "Cancel"), null)
                .show();
    }

    private void buscarContatos() {
        a.aviso(L.t("Buscando…", "Searching…"));
        TelegramBot.buscarChats(a, d.telegramToken, new TelegramBot.Chats() {
            public void encontrados(final List<String[]> chats) {
                // Tira quem já está salvo.
                for (int i = chats.size() - 1; i >= 0; i--) {
                    for (Dados.Contato c : d.contatos) {
                        if (Redes.TELEGRAM.equals(c.meio) && c.usuario.equals(chats.get(i)[0])) {
                            chats.remove(i);
                            break;
                        }
                    }
                }
                if (chats.isEmpty()) {
                    a.dialogo().setMessage(TelegramBot.textoNenhum()).setPositiveButton("OK", null).show();
                    return;
                }
                String[] nomes = new String[chats.size()];
                final boolean[] marcados = new boolean[chats.size()];
                for (int i = 0; i < nomes.length; i++) {
                    nomes[i] = chats.get(i)[1];
                    marcados[i] = true;
                }
                a.dialogo().setTitle(L.t("Adicionar contatos", "Add contacts"))
                        .setMultiChoiceItems(nomes, marcados, new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface di, int i, boolean m) { marcados[i] = m; }
                        })
                        .setPositiveButton(L.t("Adicionar", "Add"), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface di, int w) {
                                int n = 0;
                                for (int i = 0; i < marcados.length; i++) {
                                    if (!marcados[i]) continue;
                                    Dados.Contato c = new Dados.Contato();
                                    c.id = Dados.novoId();
                                    c.nome = chats.get(i)[1];
                                    c.usuario = chats.get(i)[0];
                                    c.meio = Redes.TELEGRAM;
                                    c.criado = System.currentTimeMillis();
                                    d.contatos.add(c);
                                    n++;
                                }
                                d.salvar();
                                a.aviso(n + L.t(" contato(s) adicionado(s).", " contact(s) added."));
                            }
                        })
                        .setNegativeButton(L.t("Cancelar", "Cancel"), null)
                        .show();
            }
        });
    }
}

/** Tela 19 ("Configurações"). */
class TelaConfig extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        View foto = Ui.foto(a, d.arquivoFoto(), 84);
        foto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { trocarFoto(); }
        });
        col.addView(foto);
        String nome = d.conta != null ? d.conta.nome : "";
        String email = d.conta != null ? d.conta.email : "";
        TextView u = Ui.texto(a, nome, 18, Ui.texto);
        u.setTypeface(Typeface.DEFAULT_BOLD);
        u.setPadding(0, Ui.dp(8), 0, 0);
        u.setGravity(Gravity.CENTER);
        col.addView(u);
        TextView e = Ui.texto(a, email, 14, Ui.textoSec);
        e.setPadding(0, 0, 0, Ui.dp(12));
        e.setGravity(Gravity.CENTER);
        col.addView(e);

        item(col, L.t("Aparência", "Appearance"), d.escuro ? L.t("Escuro", "Dark") : L.t("Claro", "Light"),
                new Runnable() { public void run() { aparencia(); } });
        item(col, L.t("Cor do app", "App color"), Ui.nomesCores()[Math.max(0, Math.min(d.cor, Ui.CORES.length - 1))],
                new Runnable() { public void run() { cor(); } });
        item(col, L.t("Conta", "Account"), null, new Runnable() { public void run() { conta(); } });
        item(col, L.t("Notificações", "Notifications"), d.vibrar ? L.t("Vibrar", "Vibrate") : L.t("Silencioso", "Silent"),
                new Runnable() { public void run() { notificacoes(); } });
        item(col, L.t("Confirmar antes de enviar", "Confirm before sending"),
                d.confirmar ? L.t("Sim", "Yes") : L.t("Não", "No"), new Runnable() {
                    public void run() {
                        d.confirmar = !d.confirmar;
                        d.salvar();
                        a.desenhar();
                    }
                });
        item(col, L.t("WhatsApp: todos de uma vez", "WhatsApp: everyone at once"),
                d.whatsJunto ? L.t("Sim", "Yes") : L.t("Não", "No"), new Runnable() {
                    public void run() { whatsApp(); }
                });
        item(col, L.t("Armazenamento e dados", "Storage and data"), null,
                new Runnable() { public void run() { a.abrir(new TelaArmazenamento()); } });
        item(col, L.t("Acessibilidade", "Accessibility"), null, new Runnable() { public void run() { acessibilidade(); } });
        item(col, L.t("Idioma", "Language"), d.idioma.equals("en") ? "English" : "Português (BR)",
                new Runnable() { public void run() { idioma(); } });
        item(col, L.t("Convidar amigos", "Invite friends"), null, new Runnable() { public void run() { convidar(); } });
        item(col, L.t("Versão do app", "App version"), versao(), new Runnable() {
            public void run() { a.aviso("Chegou " + versao()); }
        });
        LinearLayout excluir = item(col, L.t("Excluir conta", "Delete account"), null,
                new Runnable() { public void run() { excluirConta(); } });
        ((TextView) excluir.getChildAt(0)).setTextColor(android.graphics.Color.rgb(205, 50, 50));
        return Ui.rolagem(a, col);
    }

    private LinearLayout item(LinearLayout col, String nome, String valor, final Runnable acao) {
        LinearLayout it = Ui.itemMenu(a, nome, valor, Ui.texto);
        it.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { acao.run(); }
        });
        col.addView(it, Ui.margemBaixo(0));
        return it;
    }

    private String versao() {
        try {
            return a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private void aparencia() {
        String[] ops = {L.t("Claro", "Light"), L.t("Escuro", "Dark")};
        a.dialogo().setTitle(L.t("Tema do app", "App theme"))
                .setSingleChoiceItems(ops, d.escuro ? 1 : 0, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        di.dismiss();
                        if (d.escuro == (i == 1)) return;
                        d.escuro = i == 1;
                        d.salvar();
                        a.reaplicarAparencia();
                    }
                }).show();
    }

    private void cor() {
        a.dialogo().setTitle(L.t("Cor do app", "App color"))
                .setSingleChoiceItems(Ui.nomesCores(), d.cor, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        di.dismiss();
                        d.cor = i;
                        d.salvar();
                        a.reaplicarAparencia();
                    }
                }).show();
    }

    private void whatsApp() {
        String[] ops = {L.t("Todos de uma vez: o WhatsApp abre uma vez, você marca as pessoas e envia",
                "Everyone at once: WhatsApp opens once, you tick the people and send"),
                L.t("Uma conversa por vez: já abre na pessoa certa, uma de cada vez",
                        "One chat at a time: opens on the right person, one by one")};
        a.dialogo().setTitle("WhatsApp")
                .setSingleChoiceItems(ops, d.whatsJunto ? 0 : 1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        d.whatsJunto = i == 0;
                        d.salvar();
                        di.dismiss();
                        a.desenhar();
                    }
                }).show();
    }

    private void conta() {
        String[] ops = {L.t("Trocar foto", "Change photo"), L.t("Trocar nome de usuário", "Change username"),
                L.t("Trocar e-mail", "Change e-mail"), L.t("Trocar senha", "Change password"), L.t("Sair", "Sign out")};
        a.dialogo().setTitle(L.t("Conta", "Account")).setItems(ops, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface di, int i) {
                if (i == 0) trocarFoto();
                else if (i == 1) trocarTexto(L.t("Nome de usuário", "Username"), d.conta.nome, false);
                else if (i == 2) trocarTexto(L.t("E-mail", "E-mail"), d.conta.email, true);
                else if (i == 3) trocarSenha();
                else {
                    d.logado = false;
                    d.salvar();
                    a.irPara(new TelaBoasVindas());
                }
            }
        }).show();
    }

    private void trocarFoto() {
        a.escolherArquivo("image/*", new MainActivity.ArquivoEscolhido() {
            public void escolhido(Uri uri) {
                if (d.copiar(uri, d.arquivoFoto(), Dados.LIMITE_MIDIA) > 0) a.desenhar();
                else a.aviso(L.t("Não foi possível usar essa foto.", "Couldn't use that photo."));
            }
        });
    }

    private void trocarTexto(String titulo, String atual, final boolean email) {
        final EditText campo = Ui.campo(a, titulo, email
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        campo.setText(atual);
        a.dialogo().setTitle(titulo).setView(envolver(campo))
                .setPositiveButton(L.t("Salvar", "Save"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        String v = campo.getText().toString().trim();
                        if (v.isEmpty() || (email && !Patterns.EMAIL_ADDRESS.matcher(v).matches())) {
                            a.aviso(L.t("Valor inválido.", "Invalid value."));
                            return;
                        }
                        if (email) d.conta.email = v;
                        else d.conta.nome = v;
                        d.salvar();
                        a.desenhar();
                    }
                })
                .setNegativeButton(L.t("Cancelar", "Cancel"), null).show();
    }

    private void trocarSenha() {
        LinearLayout f = new LinearLayout(a);
        f.setOrientation(LinearLayout.VERTICAL);
        final EditText atual = Ui.campo(a, L.t("Senha atual", "Current password"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText nova = Ui.campo(a, L.t("Nova senha", "New password"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        f.addView(atual, Ui.margemBaixo(12));
        f.addView(nova);
        a.dialogo().setTitle(L.t("Trocar senha", "Change password")).setView(envolver(f))
                .setPositiveButton(L.t("Salvar", "Save"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        if (!d.senhaConfere(d.conta.email, atual.getText().toString())) {
                            a.aviso(L.t("Senha atual incorreta.", "Wrong current password."));
                        } else if (nova.getText().length() < 4) {
                            a.aviso(L.t("A senha precisa de pelo menos 4 caracteres.", "Password needs at least 4 characters."));
                        } else {
                            d.definirSenha(nova.getText().toString());
                            d.salvar();
                            a.aviso(L.t("Senha trocada!", "Password changed!"));
                        }
                    }
                })
                .setNegativeButton(L.t("Cancelar", "Cancel"), null).show();
    }

    private void notificacoes() {
        String[] ops = {L.t("Vibrar ao enviar", "Vibrate when sent"), L.t("Silencioso", "Silent")};
        a.dialogo().setTitle(L.t("Notificações", "Notifications"))
                .setSingleChoiceItems(ops, d.vibrar ? 0 : 1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        d.vibrar = i == 0;
                        d.salvar();
                        di.dismiss();
                        a.desenhar();
                    }
                }).show();
    }

    private void acessibilidade() {
        LinearLayout f = new LinearLayout(a);
        f.setOrientation(LinearLayout.VERTICAL);
        f.addView(Ui.rotulo(a, L.t("Tamanho da fonte", "Font size")));
        final android.widget.RadioGroup grupo = new android.widget.RadioGroup(a);
        String[] tamanhos = {L.t("Normal", "Normal"), L.t("Grande", "Large"), L.t("Muito grande", "Extra large")};
        for (int i = 0; i < tamanhos.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(a);
            rb.setId(100 + i);
            rb.setText(tamanhos[i]);
            rb.setTextColor(Ui.texto);
            rb.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.primaria));
            grupo.addView(rb);
        }
        grupo.check(100 + d.fonte);
        f.addView(grupo);
        f.addView(Ui.rotulo(a, L.t("Contraste", "Contrast")));
        final CheckBox alto = Ui.caixa(a, L.t("Alto contraste", "High contrast"));
        alto.setChecked(d.contraste);
        f.addView(alto);
        a.dialogo().setTitle(L.t("Acessibilidade", "Accessibility")).setView(envolver(f))
                .setPositiveButton(L.t("Aplicar", "Apply"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        d.fonte = grupo.getCheckedRadioButtonId() - 100;
                        d.contraste = alto.isChecked();
                        d.salvar();
                        a.reaplicarAparencia();
                    }
                })
                .setNegativeButton(L.t("Cancelar", "Cancel"), null).show();
    }

    private void idioma() {
        String[] ops = {"Português (BR)", "English"};
        a.dialogo().setTitle(L.t("Idioma", "Language"))
                .setSingleChoiceItems(ops, d.idioma.equals("en") ? 1 : 0, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        di.dismiss();
                        d.idioma = i == 1 ? "en" : "pt";
                        d.salvar();
                        a.reaplicarAparencia();
                    }
                }).show();
    }

    private void convidar() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, L.t("Estou usando o Chegou para mandar mensagens com um toque só! "
                        + "Me pede o instalador que eu te passo.",
                "I'm using Chegou to send messages with a single tap! Ask me for the installer."));
        a.startActivity(Intent.createChooser(i, L.t("Convidar amigos", "Invite friends")));
    }

    private void excluirConta() {
        a.confirmar(L.t("Excluir a conta apaga a foto, os contatos, as mensagens e as mídias deste app. "
                        + "Isso não pode ser desfeito. Você tem certeza?",
                "Deleting the account erases the photo, contacts, messages and media in this app. "
                        + "This can't be undone. Are you sure?"), new Runnable() {
            public void run() {
                d.excluirTudo();
                a.irPara(new TelaBoasVindas());
                a.aviso(L.t("Conta excluída.", "Account deleted."));
            }
        });
    }

    private View envolver(View v) {
        LinearLayout l = new LinearLayout(a);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(Ui.dp(24), Ui.dp(8), Ui.dp(24), 0);
        l.addView(v);
        return l;
    }
}

/** Armazenamento e dados: mídias salvas, espaço usado e limpeza. */
class TelaArmazenamento extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Armazenamento e dados", "Storage and data")));
        long total = 0;
        for (Dados.Midia m : d.midias) total += m.tamanho;
        col.addView(Ui.texto(a, d.contatos.size() + L.t(" contatos · ", " contacts · ") + d.mensagens.size()
                + L.t(" mensagens · ", " messages · ") + d.midias.size() + L.t(" mídias (", " media (")
                + Dados.tamanho(total) + ")", 15, Ui.texto), Ui.margemBaixo(16));

        col.addView(Ui.rotulo(a, L.t("Mídias salvas", "Saved media")));
        if (d.midias.isEmpty()) col.addView(Ui.texto(a, L.t("Nada ainda :)", "Nothing yet :)"), 15, Ui.textoSec));
        for (final Dados.Midia m : d.midias) {
            final LinearLayout p = Ui.pilula(a, null, m.nome, Dados.tamanho(m.tamanho), Ui.textoSec,
                    Dados.dataCurta(m.criado));
            View.OnClickListener apagar = new View.OnClickListener() {
                public void onClick(View v) {
                    a.confirmar(L.t("Excluir a mídia \"", "Delete media \"") + m.nome + "\"?", new Runnable() {
                        public void run() {
                            d.excluirMidia(m);
                            a.desenhar();
                            a.aviso(L.t("Excluído com sucesso", "Deleted successfully"));
                        }
                    });
                }
            };
            p.setOnClickListener(apagar);
            col.addView(p, Ui.margemBaixo(10));
        }
        if (!d.midias.isEmpty()) {
            col.addView(Ui.texto(a, L.t("Toque numa mídia para excluí-la.", "Tap a media item to delete it."),
                    12, Ui.textoSec));
        }
        TextView nova = Ui.link(a, L.t("＋ Nova mídia", "＋ New media"));
        nova.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormMidia()); }
        });
        col.addView(nova);
        return Ui.rolagem(a, col);
    }
}
