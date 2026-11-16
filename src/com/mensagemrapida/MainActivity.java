package com.mensagemrapida;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A única Activity do app. Cada tela dos rascunhos é uma classe Tela* que monta sua View;
 * aqui ficam a pilha de navegação, a barra de baixo com o ☰, a gaveta e os avisos.
 */
public class MainActivity extends Activity {

    interface Resposta {
        void sim(boolean ok);
    }

    interface ArquivoEscolhido {
        void escolhido(Uri uri);
    }

    private static final int REQ_SMS = 1;
    private static final int REQ_ARQUIVO = 2;

    Dados d;
    Envio envio;

    private final List<Tela> pilha = new ArrayList<Tela>();
    private FrameLayout raiz;
    private FrameLayout conteudo;
    private View barra;
    private View gaveta;
    private View folha;
    private View veu;
    private TextView aviso;

    private Resposta respostaSms;
    private ArquivoEscolhido arquivoEscolhido;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable esconderAviso = new Runnable() {
        public void run() {
            aviso.animate().alpha(0f).setDuration(250).withEndAction(new Runnable() {
                public void run() { aviso.setVisibility(View.GONE); }
            });
        }
    };

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onCreate(Bundle salvo) {
        d = Dados.de(this);
        setTheme(d.escuro ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(salvo);
        Ui.aplicar(this, d);
        envio = new Envio(this);
        montar();
        Lembretes.reagendar(this);

        if (!d.logado) {
            irPara(new TelaBoasVindas());
        } else if (salvo != null && "config".equals(salvo.getString("tela"))) {
            // Voltando de uma troca de tema: reabre as configurações.
            irPara(new TelaInicio());
            abrir(new TelaConfig());
        } else {
            irPara(new TelaInicio());
            if (salvo == null) tratarIntent(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (!pilha.isEmpty() && topo() instanceof TelaConfig) out.putString("tela", "config");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        tratarIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        envio.aoRetornar();
    }

    /** Toque no widget ou na notificação de lembrete. */
    private void tratarIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        // Reabrir o app pela lista de recentes reentrega o intent original; não reenviar.
        if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return;
        boolean enviar = WidgetEnviar.ACAO_ENVIAR.equals(intent.getAction());
        Dados.Mensagem m = d.mensagem(String.valueOf(intent.getStringExtra(Lembretes.EXTRA_MENSAGEM)));
        if (!enviar && m == null) return;
        setIntent(new Intent(this, MainActivity.class));
        if (!d.logado) return;
        if (m != null) {
            // Veio de um lembrete: essa passa a ser a mensagem do Início.
            d.ativa = m.id;
            d.salvar();
        }
        irPara(new TelaInicio());
        if (enviar) envio.enviar(m != null ? m : d.mensagemAtiva());
    }

    /** Aplica tema/fonte/idioma novos redesenhando tudo. */
    void reaplicarAparencia() {
        recreate();
    }

    // ------------------------------------------------------------------ estrutura

    private void montar() {
        getWindow().setStatusBarColor(Ui.fundo);
        getWindow().setNavigationBarColor(Ui.fundo);
        // No tema claro, os ícones das barras do sistema precisam ser escuros para aparecer.
        // Ícones escuros na barra de navegação só existem a partir do Android 8 (flag 0x10);
        // antes disso a barra fica preta, com os ícones brancos de sempre.
        int flags = 0;
        if (!Ui.escuro) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= 26) flags |= 0x10; // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            else getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);

        raiz = new FrameLayout(this);
        raiz.setBackgroundColor(Ui.fundo);

        LinearLayout coluna = new LinearLayout(this);
        coluna.setOrientation(LinearLayout.VERTICAL);
        conteudo = new FrameLayout(this);
        coluna.addView(conteudo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        barra = montarBarra();
        coluna.addView(barra);
        raiz.addView(coluna);

        aviso = Ui.texto(this, "", 14, Ui.texto);
        aviso.setGravity(Gravity.CENTER);
        aviso.setBackground(Ui.forma(Ui.superficie, Ui.primaria, 30));
        aviso.setPadding(Ui.dp(20), Ui.dp(12), Ui.dp(20), Ui.dp(12));
        aviso.setElevation(Ui.dp(6));
        aviso.setVisibility(View.GONE);
        FrameLayout.LayoutParams lpAviso = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        lpAviso.setMargins(Ui.dp(24), 0, Ui.dp(24), Ui.dp(72));
        raiz.addView(aviso, lpAviso);

        veu = new View(this);
        veu.setBackgroundColor(0x66000000);
        veu.setVisibility(View.GONE);
        veu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                fecharGaveta();
                fecharFolha();
            }
        });
        raiz.addView(veu);

        setContentView(raiz);
    }

    /** Barra de baixo: linha azul e o ☰ que abre a gaveta (tela 4). */
    private View montarBarra() {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackground(Ui.forma(Ui.fundo, 0, 0));
        b.setPadding(Ui.dp(8), 0, Ui.dp(8), 0);
        TextView menu = Ui.texto(this, "☰", 26, Ui.primaria);
        menu.setPadding(Ui.dp(12), Ui.dp(6), Ui.dp(12), Ui.dp(6));
        menu.setBackground(Ui.clicavel(Ui.forma(android.graphics.Color.TRANSPARENT, 0, 8)));
        menu.setContentDescription(L.t("Menu", "Menu"));
        menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { abrirGaveta(); }
        });
        b.addView(menu);

        LinearLayout comLinha = new LinearLayout(this);
        comLinha.setOrientation(LinearLayout.VERTICAL);
        View linha = new View(this);
        linha.setBackgroundColor(Ui.linha);
        comLinha.addView(linha, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Ui.dp(Ui.contraste ? 2 : 1.5f)));
        comLinha.addView(b, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(56)));
        return comLinha;
    }

    // ------------------------------------------------------------------ gaveta (tela 4)

    private void abrirGaveta() {
        if (gaveta != null) raiz.removeView(gaveta);
        Ui.fundoAtualMenu = Ui.superficie;
        LinearLayout g = new LinearLayout(this);
        g.setOrientation(LinearLayout.VERTICAL);
        g.setBackgroundColor(Ui.superficie);
        g.setPadding(Ui.dp(20), Ui.dp(32), Ui.dp(20), Ui.dp(20));
        g.setElevation(Ui.dp(12));
        g.setClickable(true);

        // Quem está usando o app, no topo da gaveta.
        if (d.conta != null) {
            LinearLayout quem = new LinearLayout(this);
            quem.setGravity(Gravity.CENTER_VERTICAL);
            quem.addView(Ui.foto(this, d.arquivoFoto(), 48));
            LinearLayout textos = new LinearLayout(this);
            textos.setOrientation(LinearLayout.VERTICAL);
            textos.setPadding(Ui.dp(12), 0, 0, 0);
            TextView nome = Ui.texto(this, d.conta.nome, 17, Ui.texto);
            nome.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            textos.addView(nome);
            textos.addView(Ui.texto(this, d.conta.email, 13, Ui.textoSec));
            quem.addView(textos);
            g.addView(quem, Ui.margemBaixo(16));
        }

        Tela[] destinos = {new TelaInicio(), new TelaSalvarNovo(), new TelaMensagens(),
                new TelaContatos(), new TelaContas(), new TelaConfig()};
        String[] nomes = {"🏠  " + L.t("Início", "Home"), "➕  " + L.t("Salvar novo", "Save new"),
                "💬  " + L.t("Mensagens salvas", "Saved messages"), "👥  " + L.t("Contatos salvos", "Saved contacts"),
                "🔗  " + L.t("Contas vinculadas", "Linked accounts"), "⚙️  " + L.t("Configuração", "Settings")};
        for (int i = 0; i < destinos.length; i++) {
            final Tela t = destinos[i];
            LinearLayout item = Ui.itemMenu(this, nomes[i], null, Ui.primaria);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    fecharGaveta();
                    if (t instanceof TelaInicio) irPara(t);
                    else {
                        irPara(new TelaInicio());
                        abrir(t);
                    }
                }
            });
            g.addView(item);
        }
        Ui.fundoAtualMenu = Ui.fundo;

        int largura = Math.min(Ui.dp(300), (int) (getResources().getDisplayMetrics().widthPixels * 0.78f));
        raiz.addView(g, new FrameLayout.LayoutParams(largura, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START));
        gaveta = g;
        veu.setVisibility(View.VISIBLE);
        veu.setAlpha(0f);
        veu.animate().alpha(1f).setDuration(200);
        g.setTranslationX(-largura);
        g.animate().translationX(0).setDuration(220);
    }

    private void fecharGaveta() {
        if (gaveta == null) return;
        final View g = gaveta;
        gaveta = null;
        veu.animate().alpha(0f).setDuration(180).withEndAction(new Runnable() {
            public void run() { veu.setVisibility(View.GONE); }
        });
        g.animate().translationX(-g.getWidth()).setDuration(200).withEndAction(new Runnable() {
            public void run() { raiz.removeView(g); }
        });
    }

    // ------------------------------------------------------------------ folha de baixo

    /** Painel que sobe de baixo da tela (usado na confirmação de envio). */
    void abrirFolha(View conteudo) {
        if (folha != null) raiz.removeView(folha);
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(LinearLayout.VERTICAL);
        f.setBackground(Ui.forma(Ui.superficie, Ui.contorno, 24));
        f.setPadding(Ui.dp(24), Ui.dp(22), Ui.dp(24), Ui.dp(20));
        f.setElevation(Ui.dp(16));
        f.setClickable(true);
        f.addView(conteudo);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        lp.setMargins(Ui.dp(10), 0, Ui.dp(10), Ui.dp(10));
        raiz.addView(f, lp);
        folha = f;
        veu.setVisibility(View.VISIBLE);
        veu.setAlpha(0f);
        veu.animate().alpha(1f).setDuration(160);
        f.setTranslationY(Ui.dp(320));
        f.animate().translationY(0).setDuration(220)
                .setInterpolator(new android.view.animation.DecelerateInterpolator());
    }

    void fecharFolha() {
        if (folha == null) return;
        final View f = folha;
        folha = null;
        veu.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
            public void run() { if (gaveta == null && folha == null) veu.setVisibility(View.GONE); }
        });
        f.animate().translationY(f.getHeight() + Ui.dp(20)).setDuration(180).withEndAction(new Runnable() {
            public void run() { raiz.removeView(f); }
        });
    }

    // ------------------------------------------------------------------ navegação

    private Tela topo() {
        return pilha.get(pilha.size() - 1);
    }

    /** Abre uma tela por cima da atual (VOLTAR retorna). */
    void abrir(Tela t) {
        t.a = this;
        t.d = d;
        pilha.add(t);
        desenhar(1);
    }

    boolean podeVoltar() {
        return pilha.size() > 1;
    }

    /** Troca a tela atual por outra (sem guardar a atual na pilha). */
    void trocar(Tela t) {
        if (!pilha.isEmpty()) pilha.remove(pilha.size() - 1);
        abrir(t);
    }

    /** Esvazia a pilha e começa nesta tela. */
    void irPara(Tela t) {
        pilha.clear();
        abrir(t);
    }

    void voltar() {
        if (pilha.size() <= 1) {
            finish();
            return;
        }
        pilha.remove(pilha.size() - 1);
        desenhar(-1);
    }

    /** Redesenha a tela do topo (depois de mudar dados). */
    void desenhar() {
        desenhar(0);
    }

    /** direcao: 1 entra pela direita (avançar), -1 pela esquerda (voltar), 0 sem animação. */
    private void desenhar(int direcao) {
        Tela t = topo();
        Ui.fundoAtualMenu = Ui.fundo;
        // Redesenhar a mesma tela (ex.: marcar um dia da semana) não pode perder a posição da rolagem.
        android.widget.ScrollView antes = direcao == 0 && conteudo.getChildCount() > 0
                ? rolagemDe(conteudo.getChildAt(0)) : null;
        final int posicao = antes != null ? antes.getScrollY() : 0;
        View v = t.criar();
        conteudo.removeAllViews();
        conteudo.addView(v);
        final android.widget.ScrollView depois = rolagemDe(v);
        if (posicao > 0 && depois != null) {
            depois.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    depois.getViewTreeObserver().removeOnPreDrawListener(this);
                    depois.scrollTo(0, posicao);
                    return true;
                }
            });
        }
        barra.setVisibility(t.temBarra() ? View.VISIBLE : View.GONE);
        if (direcao != 0) {
            esconderTeclado();
            v.setAlpha(0f);
            v.setTranslationX(direcao * Ui.dp(32));
            v.animate().alpha(1f).translationX(0f).setDuration(220)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator());
        }
    }

    /** A área rolável de uma tela (ela mesma, ou dentro do quadro que segura o botão "+"). */
    private static android.widget.ScrollView rolagemDe(View v) {
        if (v instanceof android.widget.ScrollView) return (android.widget.ScrollView) v;
        if (v instanceof ViewGroup && ((ViewGroup) v).getChildCount() > 0
                && ((ViewGroup) v).getChildAt(0) instanceof android.widget.ScrollView) {
            return (android.widget.ScrollView) ((ViewGroup) v).getChildAt(0);
        }
        return null;
    }

    Tela telaAtual() {
        return topo();
    }

    private void esconderTeclado() {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(raiz.getWindowToken(), 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (folha != null) fecharFolha();
        else if (gaveta != null) fecharGaveta();
        else voltar();
    }

    // ------------------------------------------------------------------ utilidades para as telas

    /** Aviso que aparece embaixo e some sozinho (tela 12, "Excluído com sucesso"). */
    void aviso(String s) {
        main.removeCallbacks(esconderAviso);
        aviso.setText(s);
        aviso.setVisibility(View.VISIBLE);
        aviso.setAlpha(0f);
        aviso.animate().alpha(1f).setDuration(200);
        main.postDelayed(esconderAviso, 2800);
    }

    AlertDialog.Builder dialogo() {
        return new AlertDialog.Builder(this, Ui.estiloDialogo());
    }

    /** "Você tem certeza?" com Sim / Não (tela 11). */
    void confirmar(String pergunta, final Runnable seSim) {
        dialogo()
                .setMessage(pergunta)
                .setPositiveButton(L.t("Sim", "Yes"), new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface di, int w) { seSim.run(); }
                })
                .setNegativeButton(L.t("Não", "No"), null)
                .show();
    }

    void pedirPermissaoSms(Resposta r) {
        pedirPermissao(Manifest.permission.SEND_SMS, r);
    }

    void pedirPermissao(String permissao, Resposta r) {
        respostaSms = r;
        requestPermissions(new String[]{permissao}, REQ_SMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode != REQ_SMS || respostaSms == null) return;
        Resposta r = respostaSms;
        respostaSms = null;
        r.sim(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED);
    }

    /** Abre o seletor de arquivos do Android. Ex.: "image/*", ou "*&#47;*" para qualquer um. */
    void escolherArquivo(String tipo, ArquivoEscolhido cb) {
        arquivoEscolhido = cb;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(tipo);
        try {
            startActivityForResult(i, REQ_ARQUIVO);
        } catch (ActivityNotFoundException e) {
            aviso(L.t("Não foi possível abrir os arquivos.", "Could not open files."));
        }
    }

    /** Abre a agenda do celular para escolher um telefone. */
    void escolherTelefone(ArquivoEscolhido cb) {
        arquivoEscolhido = cb;
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        try {
            startActivityForResult(i, REQ_ARQUIVO);
        } catch (ActivityNotFoundException e) {
            aviso(L.t("Não foi possível abrir a agenda.", "Could not open contacts."));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_ARQUIVO || arquivoEscolhido == null) return;
        ArquivoEscolhido cb = arquivoEscolhido;
        arquivoEscolhido = null;
        if (resultCode == RESULT_OK && data != null && data.getData() != null) cb.escolhido(data.getData());
    }
}
