package com.mensagemrapida;

import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

/** Tela 1: "Chegou" com Entrar / Criar conta. */
class TelaBoasVindas extends Tela {

    @Override
    boolean temBarra() {
        return false;
    }

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView nome = Ui.texto(a, "Chegou", 64, Ui.texto);
        nome.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        nome.setGravity(Gravity.CENTER);
        nome.setPadding(0, Ui.dp(64), 0, 0);
        col.addView(nome);

        // Os dois traços azuis embaixo do nome, como no rascunho.
        for (int i = 0; i < 2; i++) {
            View traco = new View(a);
            traco.setBackgroundColor(Ui.primaria);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(170 - i * 20), Ui.dp(2));
            lp.topMargin = Ui.dp(i == 0 ? 2 : 4);
            col.addView(traco, lp);
        }

        TextView sub = Ui.texto(a, L.t("Sua mensagem, para quem você quiser, com um toque.",
                "Your message, to whoever you want, in one tap."), 15, Ui.textoSec);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(20), 0, Ui.dp(56));
        col.addView(sub);

        TextView entrar = Ui.botao(a, L.t("Entrar", "Sign in"));
        TextView criarConta = Ui.botao(a, L.t("Criar conta", "Create account"));
        entrar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (d.conta == null) {
                    a.aviso(L.t("Ainda não há conta neste celular. Crie uma!",
                            "There's no account on this phone yet. Create one!"));
                } else {
                    a.abrir(new TelaEntrar());
                }
            }
        });
        criarConta.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (d.conta == null) {
                    a.abrir(new TelaCriarConta());
                    return;
                }
                a.confirmar(L.t("Já existe uma conta neste celular. Criar outra apaga a atual e todos os "
                                + "contatos e mensagens dela. Você tem certeza?",
                        "There is already an account on this phone. Creating another erases it with all its "
                                + "contacts and messages. Are you sure?"), new Runnable() {
                    public void run() {
                        d.excluirTudo();
                        a.abrir(new TelaCriarConta());
                    }
                });
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(220), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(14);
        col.addView(entrar, lp);
        col.addView(criarConta, new LinearLayout.LayoutParams(lp));

        TextView nota = Ui.texto(a, L.t("Sua conta e seus dados ficam guardados só neste celular.",
                "Your account and data are stored only on this phone."), 12, Ui.textoSec);
        nota.setGravity(Gravity.CENTER);
        nota.setPadding(0, Ui.dp(32), 0, 0);
        col.addView(nota);
        return Ui.rolagem(a, col);
    }
}

/** Tela 2: foto, e-mail, senha e "esqueceu?". */
class TelaEntrar extends Tela {

    @Override
    boolean temBarra() {
        return false;
    }

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(Ui.dp(28), Ui.dp(56), Ui.dp(28), Ui.dp(20));

        col.addView(Ui.foto(a, d.arquivoFoto(), 110));

        final EditText email = Ui.campoPilula(a, L.t("E-mail", "E-mail"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText senha = Ui.campoPilula(a, L.t("Senha", "Password"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams lp = Ui.margemBaixo(14);
        lp.topMargin = Ui.dp(40);
        col.addView(email, lp);
        col.addView(senha, Ui.margemBaixo(0));

        TextView esqueceu = Ui.link(a, L.t("esqueceu?", "forgot?"));
        LinearLayout.LayoutParams lpE = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpE.gravity = Gravity.END;
        col.addView(esqueceu, lpE);
        esqueceu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                a.dialogo()
                        .setTitle(L.t("Esqueceu a senha?", "Forgot your password?"))
                        .setMessage(L.t("Sua conta fica só neste celular, então não dá para enviar um "
                                        + "e-mail de recuperação. Você pode apagar a conta e criar outra "
                                        + "(contatos e mensagens serão apagados).",
                                "Your account lives only on this phone, so there's no recovery e-mail. "
                                        + "You can erase the account and create a new one "
                                        + "(contacts and messages will be erased)."))
                        .setPositiveButton(L.t("Apagar e criar nova", "Erase and create new"),
                                new android.content.DialogInterface.OnClickListener() {
                                    public void onClick(android.content.DialogInterface di, int w) {
                                        d.excluirTudo();
                                        a.irPara(new TelaBoasVindas());
                                        a.abrir(new TelaCriarConta());
                                    }
                                })
                        .setNegativeButton(L.t("Cancelar", "Cancel"), null)
                        .show();
            }
        });

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView entrar = Ui.botaoCheio(a, L.t("Entrar", "Sign in"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        entrar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (d.senhaConfere(email.getText().toString(), senha.getText().toString())) {
                    d.logado = true;
                    d.salvar();
                    a.irPara(new TelaInicio());
                } else {
                    a.aviso(L.t("E-mail ou senha incorretos.", "Wrong e-mail or password."));
                }
            }
        });
        col.addView(Ui.barraBotoes(a, voltar, entrar));
        return Ui.rolagem(a, col);
    }
}

/** Criar conta: foto (opcional), nome de usuário, e-mail e senha. */
class TelaCriarConta extends Tela {

    private Uri fotoEscolhida;

    @Override
    boolean temBarra() {
        return false;
    }

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.setPadding(Ui.dp(28), Ui.dp(40), Ui.dp(28), Ui.dp(20));
        col.addView(Ui.titulo(a, L.t("Criar conta", "Create account")));

        final LinearLayout areaFoto = new LinearLayout(a);
        areaFoto.setGravity(Gravity.CENTER_HORIZONTAL);
        areaFoto.setOrientation(LinearLayout.VERTICAL);
        col.addView(areaFoto);
        mostrarFoto(areaFoto);

        final EditText nome = Ui.campo(a, L.t("Nome de usuário", "Username"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final EditText email = Ui.campo(a, L.t("E-mail", "E-mail"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        final EditText senha = Ui.campo(a, L.t("Senha (mínimo 4 caracteres)", "Password (at least 4 characters)"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final EditText senha2 = Ui.campo(a, L.t("Repita a senha", "Repeat the password"),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText[] campos = {nome, email, senha, senha2};
        for (EditText e : campos) col.addView(e, Ui.margemBaixo(14));

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView criarBt = Ui.botaoCheio(a, L.t("Criar", "Create"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        criarBt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String n = nome.getText().toString().trim();
                String e = email.getText().toString().trim();
                String s = senha.getText().toString();
                if (n.isEmpty()) {
                    a.aviso(L.t("Escolha um nome de usuário.", "Choose a username."));
                } else if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
                    a.aviso(L.t("Digite um e-mail válido.", "Enter a valid e-mail."));
                } else if (s.length() < 4) {
                    a.aviso(L.t("A senha precisa de pelo menos 4 caracteres.", "Password needs at least 4 characters."));
                } else if (!s.equals(senha2.getText().toString())) {
                    a.aviso(L.t("As senhas não são iguais.", "Passwords don't match."));
                } else {
                    d.criarConta(n, e, s);
                    a.irPara(new TelaInicio());
                    a.aviso(L.t("Bem-vindo(a), ", "Welcome, ") + n + "!");
                }
            }
        });
        col.addView(Ui.barraBotoes(a, voltar, criarBt));
        return Ui.rolagem(a, col);
    }

    private void mostrarFoto(final LinearLayout area) {
        area.removeAllViews();
        File f = d.arquivoFoto();
        View foto = Ui.foto(a, fotoEscolhida != null ? f : null, 96);
        area.addView(foto);
        TextView t = Ui.link(a, L.t("Escolher foto (opcional)", "Choose photo (optional)"));
        area.addView(t);
        View.OnClickListener escolher = new View.OnClickListener() {
            public void onClick(View v) {
                a.escolherArquivo("image/*", new MainActivity.ArquivoEscolhido() {
                    public void escolhido(Uri uri) {
                        if (d.copiar(uri, d.arquivoFoto(), Dados.LIMITE_MIDIA) > 0) {
                            fotoEscolhida = uri;
                            mostrarFoto(area);
                        } else {
                            a.aviso(L.t("Não foi possível usar essa foto.", "Couldn't use that photo."));
                        }
                    }
                });
            }
        };
        foto.setOnClickListener(escolher);
        t.setOnClickListener(escolher);
    }
}
