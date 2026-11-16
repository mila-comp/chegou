package com.mensagemrapida;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Ao segurar um item: Editar / Excluir / Info (tela 10). */
final class Opcoes {
    interface Acoes {
        void editar();
        void excluir();
        void info();
    }

    static void mostrar(View ancora, final Acoes acoes) {
        android.content.Context c = ancora.getContext();
        LinearLayout caixa = new LinearLayout(c);
        caixa.setOrientation(LinearLayout.VERTICAL);
        caixa.setBackground(Ui.forma(Ui.superficie, Ui.primaria, 4));
        caixa.setElevation(Ui.dp(8));
        final PopupWindow janela = new PopupWindow(caixa, Ui.dp(150),
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        janela.setElevation(Ui.dp(8));
        String[] nomes = {L.t("Editar", "Edit"), L.t("Excluir", "Delete"), L.t("Info", "Info")};
        for (int i = 0; i < nomes.length; i++) {
            final int qual = i;
            TextView t = Ui.texto(c, nomes[i], 16, Ui.primaria);
            t.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(16), Ui.dp(12));
            t.setBackground(Ui.clicavel(i < 2 ? Ui.linhaEmbaixo(Ui.primaria, Ui.superficie, Ui.dp(1))
                    : Ui.forma(Ui.superficie, 0, 4)));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    janela.dismiss();
                    if (qual == 0) acoes.editar();
                    else if (qual == 1) acoes.excluir();
                    else acoes.info();
                }
            });
            caixa.addView(t);
        }
        // Aparece embaixo do item, mais para a direita, como no rascunho.
        janela.showAsDropDown(ancora, Math.max(0, ancora.getWidth() / 2 - Ui.dp(40)), -Ui.dp(8));
    }

    private Opcoes() { }
}

/** Telas 9 a 13 ("Mensagens salvas"). */
class TelaMensagens extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Mensagens salvas", "Saved messages")));
        if (d.mensagens.isEmpty()) {
            TextView nova = Ui.botao(a, L.t("Nova mensagem", "New message"));
            nova.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaFormMensagem(null)); }
            });
            col.addView(Ui.estadoVazio(a, "💬", L.t("Nada ainda :)", "Nothing yet :)"), nova));
            return Ui.rolagem(a, col);
        }
        col.addView(Ui.texto(a, L.t("Toque para ver; segure para mais opções.",
                "Tap to view; hold for more options."), 13, Ui.textoSec), Ui.margemBaixo(12));
        for (final Dados.Mensagem m : d.mensagens) {
            String detalhe = m.texto.replace('\n', ' ');
            if (!m.midia.isEmpty()) detalhe = "📎 " + detalhe;
            final LinearLayout p = Ui.pilula(a, null, m.nome, detalhe, Ui.textoSec, Dados.dataCurta(m.criado));
            p.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaInfoMensagem(m)); }
            });
            p.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    Opcoes.mostrar(p, new Opcoes.Acoes() {
                        public void editar() { a.abrir(new TelaFormMensagem(m)); }
                        public void info() { a.abrir(new TelaInfoMensagem(m)); }
                        public void excluir() {
                            a.confirmar(L.t("Você tem certeza?", "Are you sure?"), new Runnable() {
                                public void run() {
                                    d.excluirMensagem(m);
                                    a.desenhar();
                                    a.aviso(L.t("Excluído com sucesso", "Deleted successfully"));
                                }
                            });
                        }
                    });
                    return true;
                }
            });
            col.addView(p, Ui.margemBaixo(10));
        }
        TextView mais = Ui.fab(a);
        mais.setContentDescription(L.t("Nova mensagem", "New message"));
        mais.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormMensagem(null)); }
        });
        return Ui.comFab(a, Ui.rolagem(a, col), mais);
    }
}

/** Tela 14 (Info): a mensagem, quando foi criada, por onde vai, a mídia e para quem. */
class TelaInfoMensagem extends Tela {
    private final Dados.Mensagem m;
    private String rede, midia;
    private List<String> escolhidos;
    private boolean lembrete;
    private int inicio, fim, dias;

    TelaInfoMensagem(Dados.Mensagem m) {
        this.m = m;
    }

    @Override
    View criar() {
        if (escolhidos == null) {
            escolhidos = new ArrayList<String>(m.destinatarios);
            rede = m.rede;
            midia = m.midia;
            lembrete = m.lembrete;
            inicio = m.lembreteInicio;
            fim = m.lembreteFim;
            dias = m.lembreteDias;
        }
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(m.nome));

        col.addView(Ui.rotulo(a, L.t("Mensagem:", "Message:")));
        TextView txt = Ui.texto(a, m.texto, 15, Ui.texto);
        txt.setPadding(Ui.dp(12), Ui.dp(10), Ui.dp(12), Ui.dp(10));
        txt.setBackground(Ui.clicavel(Ui.forma(Ui.superficie, Ui.contorno, 4)));
        txt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormMensagem(m)); }
        });
        col.addView(txt);

        col.addView(Ui.rotulo(a, L.t("Criada em:", "Created on:")));
        col.addView(Ui.texto(a, Dados.data(m.criado), 16, Ui.texto));

        // "Vinculado com": a rede usada para todos, ou o meio principal de cada contato.
        col.addView(Ui.rotulo(a, L.t("Vinculado com:", "Linked to:")));
        TextView selRede = Ui.seletor(a, rede.isEmpty() ? L.t("Meio principal de cada contato",
                "Each contact's main app") : rede);
        if (!rede.isEmpty()) selRede.setTextColor(Redes.cor(rede));
        selRede.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String[] ops = new String[Redes.TODAS.length + 1];
                ops[0] = L.t("Meio principal de cada contato", "Each contact's main app");
                System.arraycopy(Redes.TODAS, 0, ops, 1, Redes.TODAS.length);
                a.dialogo().setItems(ops, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        rede = i == 0 ? "" : ops[i];
                        a.desenhar();
                    }
                }).show();
            }
        });
        col.addView(selRede);

        col.addView(Ui.rotulo(a, L.t("Mídia anexada:", "Attached media:")));
        Dados.Midia md = d.midia(midia);
        TextView selMidia = Ui.seletor(a, md == null ? L.t("Nenhuma", "None") : "📎 " + md.nome);
        selMidia.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherMidia(); }
        });
        col.addView(selMidia);

        col.addView(Ui.rotulo(a, L.t("Encaminhar para:", "Send to:")));
        if (d.contatos.isEmpty()) {
            col.addView(Ui.texto(a, L.t("Ninguém ainda.", "Nobody yet."), 15, Ui.textoSec));
        }
        for (final Dados.Contato c : d.contatos) {
            String via = rede.isEmpty() ? c.meio : rede;
            CheckBox cb = Ui.caixa(a, c.nome + "  ·  " + via);
            cb.setChecked(escolhidos.contains(c.id));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean marcado) {
                    escolhidos.remove(c.id);
                    if (marcado) escolhidos.add(c.id);
                }
            });
            col.addView(cb);
        }
        TextView novo = Ui.link(a, L.t("＋ Novo contato", "＋ New contact"));
        novo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormContato(null)); }
        });
        col.addView(novo);

        secaoLembrete(col);

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView salvar = Ui.botaoCheio(a, L.t("Salvar", "Save"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        salvar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m.destinatarios.clear();
                m.destinatarios.addAll(escolhidos);
                m.rede = rede;
                m.midia = midia;
                boolean ligou = lembrete && !m.lembrete;
                m.lembrete = lembrete;
                m.lembreteInicio = inicio;
                m.lembreteFim = fim;
                m.lembreteDias = dias;
                m.lembreteFeito = 0; // mudou a configuração: vale de novo para hoje
                d.ativa = m.id;
                d.salvar();
                a.voltar();
                a.aviso(L.t("Salvo! Esta é a mensagem do Início agora.", "Saved! This is now the Home message."));
                if (ligou && android.os.Build.VERSION.SDK_INT >= 33
                        && a.checkSelfPermission(Lembretes.PERMISSAO_NOTIFICAR)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Android 13+: o usuário precisa permitir notificações.
                    a.pedirPermissao(Lembretes.PERMISSAO_NOTIFICAR, new MainActivity.Resposta() {
                        public void sim(boolean ok) {
                            if (ok) Lembretes.reagendar(a);
                            else a.aviso(L.t("Sem permissão de notificações, o lembrete não aparece.",
                                    "Without notification permission the reminder can't show."));
                        }
                    });
                }
            }
        });
        col.addView(Ui.barraBotoes(a, voltar, salvar));
        return Ui.rolagem(a, col);
    }

    /** Lembrete: notificação com botão Enviar numa janela de horário, nos dias marcados. */
    private void secaoLembrete(LinearLayout col) {
        col.addView(Ui.rotulo(a, L.t("Lembrete na notificação:", "Notification reminder:")));
        CheckBox liga = Ui.caixa(a, L.t("Mostrar botão Enviar na barra de notificações", "Show a Send button in notifications"));
        liga.setChecked(lembrete);
        liga.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean marcado) {
                lembrete = marcado;
                a.desenhar();
            }
        });
        col.addView(liga);
        if (!lembrete) return;

        LinearLayout horas = new LinearLayout(a);
        horas.setGravity(android.view.Gravity.CENTER_VERTICAL);
        horas.addView(Ui.texto(a, L.t("Das  ", "From  "), 15, Ui.texto));
        TextView de = Ui.seletor(a, Dados.hora(inicio));
        horas.addView(de);
        horas.addView(Ui.texto(a, L.t("  às  ", "  to  "), 15, Ui.texto));
        TextView ate = Ui.seletor(a, Dados.hora(fim));
        horas.addView(ate);
        de.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherHora(true); }
        });
        ate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { escolherHora(false); }
        });
        col.addView(horas, Ui.margemBaixo(10));

        // Dias da semana, de domingo a sábado.
        String[] letras = L.t("D S T Q Q S S", "S M T W T F S").split(" ");
        LinearLayout semana = new LinearLayout(a);
        for (int i = 0; i < 7; i++) {
            final int bit = 1 << i;
            TextView dia = Ui.chip(a, letras[i], (dias & bit) != 0);
            dia.setGravity(android.view.Gravity.CENTER);
            dia.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            dia.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dias ^= bit;
                    a.desenhar();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            if (i < 6) lp.rightMargin = Ui.dp(6);
            semana.addView(dia, lp);
        }
        col.addView(semana);
        col.addView(Ui.texto(a, L.t("Nesse horário aparece uma notificação. Com SMS e Telegram, o botão envia "
                        + "na hora, sem abrir o app; com as outras redes, abre a tela de envio.",
                "A notification shows during this time. With SMS and Telegram the button sends right away "
                        + "without opening the app; other networks open the sending screen."), 13, Ui.textoSec));
    }

    private void escolherHora(final boolean doInicio) {
        int atual = doInicio ? inicio : fim;
        new android.app.TimePickerDialog(a, Ui.estiloDialogo(), new android.app.TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(android.widget.TimePicker tp, int h, int min) {
                if (doInicio) inicio = h * 60 + min;
                else fim = h * 60 + min;
                a.desenhar();
            }
        }, atual / 60, atual % 60, true).show();
    }

    private void escolherMidia() {
        final String[] ops = new String[d.midias.size() + 2];
        ops[0] = L.t("Nenhuma", "None");
        for (int i = 0; i < d.midias.size(); i++) ops[i + 1] = d.midias.get(i).nome;
        ops[ops.length - 1] = L.t("＋ Nova mídia", "＋ New media");
        a.dialogo().setItems(ops, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface di, int i) {
                if (i == 0) midia = "";
                else if (i == ops.length - 1) {
                    a.abrir(new TelaFormMidia());
                    return;
                } else midia = d.midias.get(i - 1).id;
                a.desenhar();
            }
        }).show();
    }
}

/** Telas 16 e 17 ("Contatos salvos"). */
class TelaContatos extends Tela {
    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(L.t("Contatos salvos", "Saved contacts")));
        if (d.contatos.isEmpty()) {
            TextView novo = Ui.botao(a, L.t("Novo contato", "New contact"));
            novo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaFormContato(null)); }
            });
            col.addView(Ui.estadoVazio(a, "👥", L.t("Ninguém ainda", "Nobody yet"), novo));
            return Ui.rolagem(a, col);
        }
        col.addView(Ui.texto(a, L.t("Toque para ver; segure para mais opções.",
                "Tap to view; hold for more options."), 13, Ui.textoSec), Ui.margemBaixo(12));
        for (final Dados.Contato c : d.contatos) {
            final LinearLayout p = Ui.pilula(a, Ui.avatar(a, c.nome, 40), c.nome, c.meio, Redes.cor(c.meio),
                    Dados.dataCurta(c.criado));
            p.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { a.abrir(new TelaInfoContato(c)); }
            });
            p.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    Opcoes.mostrar(p, new Opcoes.Acoes() {
                        public void editar() { a.abrir(new TelaFormContato(c)); }
                        public void info() { a.abrir(new TelaInfoContato(c)); }
                        public void excluir() {
                            a.confirmar(L.t("Você tem certeza?", "Are you sure?"), new Runnable() {
                                public void run() {
                                    d.excluirContato(c);
                                    a.desenhar();
                                    a.aviso(L.t("Excluído com sucesso", "Deleted successfully"));
                                }
                            });
                        }
                    });
                    return true;
                }
            });
            col.addView(p, Ui.margemBaixo(10));
        }
        TextView mais = Ui.fab(a);
        mais.setContentDescription(L.t("Novo contato", "New contact"));
        mais.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.abrir(new TelaFormContato(null)); }
        });
        return Ui.comFab(a, Ui.rolagem(a, col), mais);
    }
}

/** Info de um contato: dados e as mensagens que vão para ele. */
class TelaInfoContato extends Tela {
    private final Dados.Contato c;

    TelaInfoContato(Dados.Contato c) {
        this.c = c;
    }

    @Override
    View criar() {
        LinearLayout col = Ui.coluna(a);
        col.addView(cabecalho(c.nome));
        campo(col, L.t("Telefone:", "Phone:"), c.telefone.isEmpty() ? "—" : c.telefone);
        campo(col, L.t("Principal meio de comunicação:", "Main app:"), c.meio);
        if (!c.usuario.isEmpty()) campo(col, Redes.rotuloUsuario(c.meio) + ":", c.usuario);
        campo(col, L.t("Criado em:", "Created on:"), Dados.data(c.criado));

        col.addView(Ui.rotulo(a, L.t("Recebe as mensagens:", "Receives the messages:")));
        int n = 0;
        for (Dados.Mensagem m : d.mensagens) {
            if (!m.destinatarios.contains(c.id)) continue;
            col.addView(Ui.texto(a, "• " + m.nome, 16, Ui.texto));
            n++;
        }
        if (n == 0) col.addView(Ui.texto(a, L.t("Nenhuma ainda.", "None yet."), 15, Ui.textoSec));

        TextView enviarSo = Ui.botao(a, L.t("Enviar mensagem só para ", "Send a message just to ")
                + c.nome.trim().split("\\s+")[0]);
        enviarSo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { enviarSoParaEle(); }
        });
        LinearLayout.LayoutParams lpE = Ui.margemBaixo(0);
        lpE.topMargin = Ui.dp(24);
        col.addView(enviarSo, lpE);

        TextView voltar = Ui.botao(a, L.t("Voltar", "Back"));
        TextView editar = Ui.botaoCheio(a, L.t("Editar", "Edit"));
        voltar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.voltar(); }
        });
        editar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { a.trocar(new TelaFormContato(c)); }
        });
        col.addView(Ui.barraBotoes(a, voltar, editar));
        return Ui.rolagem(a, col);
    }

    private void enviarSoParaEle() {
        if (d.mensagens.isEmpty()) {
            a.aviso(L.t("Crie uma mensagem primeiro.", "Create a message first."));
            return;
        }
        String[] nomes = new String[d.mensagens.size()];
        for (int i = 0; i < nomes.length; i++) nomes[i] = d.mensagens.get(i).nome;
        a.dialogo().setTitle(L.t("Qual mensagem?", "Which message?"))
                .setItems(nomes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int i) {
                        List<Dados.Contato> so = new ArrayList<Dados.Contato>();
                        so.add(c);
                        a.envio.enviar(d.mensagens.get(i), so);
                    }
                }).show();
    }

    private void campo(LinearLayout col, String rotulo, String valor) {
        col.addView(Ui.rotulo(a, rotulo));
        TextView t = Ui.texto(a, valor, 16, Ui.texto);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(t);
    }
}
