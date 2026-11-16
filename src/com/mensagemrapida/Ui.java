package com.mensagemrapida;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

/** Cores, tamanhos e peças de interface reaproveitadas por todas as telas. */
final class Ui {

    // Paleta atual (muda com tema claro/escuro, cor escolhida e alto contraste).
    // primaria: textos, links e contornos. destaque: preenchimento de botões; sobreDestaque: texto em cima dele.
    static int fundo, superficie, texto, textoSec, primaria, destaque, sobreDestaque, laranja, linha, contorno;
    static boolean escuro, contraste;
    static float escalaFonte = 1f;
    private static float densidade = 1f;
    private static boolean areia;

    // A paleta "Areia": BBBB88 · CCC68D · EEDD99 · EEC290 · EEAA88.
    static final int OLIVA = Color.rgb(0xBB, 0xBB, 0x88), CAQUI = Color.rgb(0xCC, 0xC6, 0x8D),
            AMARELO = Color.rgb(0xEE, 0xDD, 0x99), PESSEGO = Color.rgb(0xEE, 0xC2, 0x90),
            SALMAO = Color.rgb(0xEE, 0xAA, 0x88);
    private static final int MARROM = Color.rgb(63, 36, 22);

    /** Outras cores principais que o usuário pode escolher (índices 1+): {no claro, no escuro}. */
    static final int[][] CORES = {
            {Color.rgb(184, 96, 58), SALMAO},                    // areia (padrão; trata-se à parte)
            {Color.rgb(59, 91, 219), Color.rgb(138, 164, 255)},  // índigo
            {Color.rgb(13, 148, 136), Color.rgb(94, 214, 200)},  // verde-água
            {Color.rgb(124, 77, 219), Color.rgb(184, 156, 255)}, // roxo
            {Color.rgb(214, 62, 122), Color.rgb(255, 140, 185)}, // rosa
    };

    static String[] nomesCores() {
        return new String[]{L.t("Areia (padrão)", "Sand (default)"), L.t("Índigo", "Indigo"),
                L.t("Verde-água", "Teal"), L.t("Roxo", "Purple"), L.t("Rosa", "Pink")};
    }

    private static int indiceCor(Dados d) {
        return Math.max(0, Math.min(d.cor, CORES.length - 1));
    }

    /** A cor que representa o app fora dele (notificações). */
    static int corPrincipal(Dados d) {
        return CORES[indiceCor(d)][d.escuro ? 1 : 0];
    }

    static void aplicar(Context c, Dados d) {
        densidade = c.getResources().getDisplayMetrics().density;
        escuro = d.escuro;
        contraste = d.contraste;
        escalaFonte = d.fonte == 2 ? 1.3f : d.fonte == 1 ? 1.15f : 1f;
        areia = indiceCor(d) == 0;
        primaria = corPrincipal(d);
        if (escuro) {
            fundo = Color.rgb(20, 22, 28);
            superficie = Color.rgb(31, 34, 43);
            texto = Color.rgb(238, 240, 245);
            textoSec = Color.rgb(154, 161, 174);
            laranja = Color.rgb(255, 176, 103);
            contorno = Color.rgb(52, 57, 70);
        } else {
            fundo = Color.rgb(250, 244, 237);
            superficie = Color.WHITE;
            texto = Color.rgb(27, 37, 64);
            textoSec = Color.rgb(112, 108, 118);
            laranja = Color.rgb(226, 122, 34);
            contorno = Color.rgb(228, 217, 205);
        }
        destaque = primaria;
        sobreDestaque = escuro ? fundo : Color.WHITE;
        linha = primaria;
        if (areia) {
            // Tons quentes: fundo amarelado bem claro, oliva nos detalhes, salmão nos botões.
            if (escuro) {
                fundo = Color.rgb(29, 28, 21);
                superficie = Color.rgb(42, 41, 32);
                texto = Color.rgb(244, 239, 217);
                textoSec = Color.rgb(185, 180, 142);
                laranja = CAQUI;
                contorno = Color.rgb(69, 67, 47);
            } else {
                fundo = Color.rgb(252, 248, 233);
                superficie = Color.rgb(255, 254, 249);
                texto = Color.rgb(54, 53, 42);
                textoSec = Color.rgb(118, 117, 80);
                laranja = Color.rgb(134, 134, 72);
                contorno = Color.rgb(221, 216, 172);
            }
            destaque = SALMAO;
            sobreDestaque = MARROM;
            linha = OLIVA;
        }
        if (contraste) {
            fundo = escuro ? Color.BLACK : Color.WHITE;
            superficie = fundo;
            texto = escuro ? Color.WHITE : Color.BLACK;
            textoSec = texto;
            contorno = texto;
            primaria = escuro ? Color.rgb(160, 200, 255) : Color.rgb(0, 50, 160);
            destaque = primaria;
            sobreDestaque = fundo;
            linha = primaria;
            laranja = escuro ? Color.rgb(255, 190, 90) : Color.rgb(160, 70, 0);
        }
    }

    static int dp(float v) {
        return Math.round(v * densidade);
    }

    private static int borda() {
        return dp(contraste ? 2.5f : 1.5f);
    }

    static int estiloDialogo() {
        return escuro ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

    // ------------------------------------------------------------------ textos

    static TextView texto(Context c, CharSequence s, float sp, int cor) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(cor);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp * escalaFonte);
        return t;
    }

    static TextView titulo(Context c, String s) {
        TextView t = texto(c, s, 22, texto);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(4), 0, dp(16));
        return t;
    }

    static TextView rotulo(Context c, String s) {
        TextView t = texto(c, s, 14, texto);
        t.setAllCaps(true);
        t.setLetterSpacing(0.04f);
        t.setPadding(0, dp(14), 0, dp(4));
        return t;
    }

    // ------------------------------------------------------------------ fundos

    static GradientDrawable forma(int cor, int corBorda, float raioDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(cor);
        if (corBorda != 0) g.setStroke(borda(), corBorda);
        g.setCornerRadius(dp(raioDp));
        return g;
    }

    /** Fundo clicável com efeito de toque. */
    static Drawable clicavel(Drawable conteudo) {
        int r = Color.argb(escuro ? 60 : 45, Color.red(primaria), Color.green(primaria), Color.blue(primaria));
        return new RippleDrawable(ColorStateList.valueOf(r), conteudo, forma(Color.WHITE, 0, 12));
    }

    /** Fundo de campo com só a linha de baixo, como "Nome: ______". */
    static Drawable sublinhado() {
        return linhaEmbaixo(contraste ? texto : contorno, fundo, dp(2));
    }

    /** Desenha a cor da linha e cobre tudo com o fundo, menos a faixa de baixo. */
    static Drawable linhaEmbaixo(int corLinha, int corFundo, int espessura) {
        GradientDrawable l = new GradientDrawable();
        l.setColor(corLinha);
        GradientDrawable f = new GradientDrawable();
        f.setColor(corFundo);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{l, f});
        ld.setLayerInset(1, 0, 0, 0, espessura);
        return ld;
    }

    // ------------------------------------------------------------------ botões

    /** Botão retangular com contorno, como VOLTAR / SALVAR. */
    static TextView botao(Context c, String s) {
        TextView b = texto(c, s, 15, primaria);
        b.setAllCaps(true);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        b.setMinWidth(dp(96));
        b.setBackground(clicavel(forma(superficie, primaria, 6)));
        b.setClickable(true);
        b.setFocusable(true);
        pressionavel(b);
        return b;
    }

    static TextView botaoCheio(Context c, String s) {
        TextView b = botao(c, s);
        b.setTextColor(sobreDestaque);
        b.setBackground(clicavel(forma(destaque, 0, 6)));
        return b;
    }

    static TextView link(Context c, String s) {
        TextView t = texto(c, s, 14, primaria);
        t.setPadding(dp(4), dp(8), dp(4), dp(8));
        t.setBackground(clicavel(forma(Color.TRANSPARENT, 0, 8)));
        t.setClickable(true);
        return t;
    }

    /** Linha com VOLTAR à esquerda e SALVAR (ou outro) à direita. */
    static LinearLayout barraBotoes(Context c, View esquerda, View direita) {
        LinearLayout l = new LinearLayout(c);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(0, dp(28), 0, dp(8));
        l.addView(esquerda);
        View vao = new View(c);
        l.addView(vao, new LinearLayout.LayoutParams(0, 1, 1));
        l.addView(direita);
        return l;
    }

    // ------------------------------------------------------------------ campos

    static EditText campo(Context c, String hint, int tipo) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(tipo);
        e.setTextColor(texto);
        e.setHintTextColor(textoSec);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * escalaFonte);
        e.setBackground(sublinhado());
        e.setPadding(dp(2), dp(10), dp(2), dp(8));
        if ((tipo & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) e.setSingleLine(true);
        return e;
    }

    /** Campo em formato de pílula (tela de entrar). */
    static EditText campoPilula(Context c, String hint, int tipo) {
        EditText e = campo(c, hint, tipo);
        e.setBackground(forma(superficie, contorno, 30));
        e.setPadding(dp(18), dp(12), dp(18), dp(12));
        return e;
    }

    /** Caixa de texto grande com contador "n/1000". */
    static LinearLayout areaTexto(Context c, final EditText e, final int limite) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(false);
        e.setMinLines(5);
        e.setGravity(Gravity.TOP);
        e.setTextColor(texto);
        e.setHintTextColor(textoSec);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * escalaFonte);
        e.setBackground(forma(superficie, contorno, 4));
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(limite)});
        col.addView(e);
        final TextView cont = texto(c, e.length() + "/" + limite, 12, textoSec);
        cont.setGravity(Gravity.END);
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            public void onTextChanged(CharSequence s, int a, int b, int d) { }
            public void afterTextChanged(Editable s) { cont.setText(s.length() + "/" + limite); }
        });
        col.addView(cont, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return col;
    }

    /** Caixa "SELECIONA ▾" que abre uma lista ao tocar. */
    static TextView seletor(Context c, String valor) {
        TextView t = texto(c, valor + "  ▾", 15, texto);
        t.setPadding(dp(12), dp(10), dp(12), dp(10));
        t.setBackground(clicavel(forma(superficie, contorno, 4)));
        t.setClickable(true);
        return t;
    }

    static CheckBox caixa(Context c, String s) {
        CheckBox cb = new CheckBox(c);
        cb.setText(s);
        cb.setTextColor(texto);
        cb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * escalaFonte);
        cb.setButtonTintList(ColorStateList.valueOf(primaria));
        cb.setPadding(dp(4), dp(8), 0, dp(8));
        return cb;
    }

    // ------------------------------------------------------------------ listas

    /** Item em formato de pílula com o nome e "Criado: dd/mm" em laranja (telas 9 e 16). */
    static LinearLayout pilula(Context c, View avatar, String nome, String detalhe, int corDetalhe, String criado) {
        LinearLayout l = new LinearLayout(c);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(avatar != null ? 10 : 18), dp(10), dp(18), dp(10));
        l.setBackground(clicavel(forma(superficie, contorno, 30)));
        l.setClickable(true);
        l.setLongClickable(true);
        pressionavel(l);
        if (avatar != null) {
            LinearLayout.LayoutParams lpA = new LinearLayout.LayoutParams(
                    avatar.getLayoutParams().width, avatar.getLayoutParams().height);
            lpA.rightMargin = dp(12);
            l.addView(avatar, lpA);
        }

        LinearLayout textos = new LinearLayout(c);
        textos.setOrientation(LinearLayout.VERTICAL);
        TextView n = texto(c, nome, 17, texto);
        n.setSingleLine(true);
        n.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textos.addView(n);
        if (detalhe != null && !detalhe.isEmpty()) {
            TextView d = texto(c, detalhe, 13, corDetalhe);
            d.setSingleLine(true);
            d.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textos.addView(d);
        }
        l.addView(textos, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if (criado != null) {
            TextView cr = texto(c, L.t("Criado: ", "Created: ") + criado, 11, laranja);
            cr.setAllCaps(true);
            cr.setPadding(dp(8), 0, 0, 0);
            l.addView(cr);
        }
        return l;
    }

    /** Encolhe de leve ao tocar e volta ao soltar: dá a sensação de botão de verdade. */
    static void pressionavel(View v) {
        v.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View x, android.view.MotionEvent e) {
                int acao = e.getActionMasked();
                if (acao == android.view.MotionEvent.ACTION_DOWN) {
                    x.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80);
                } else if (acao == android.view.MotionEvent.ACTION_UP
                        || acao == android.view.MotionEvent.ACTION_CANCEL) {
                    x.animate().scaleX(1f).scaleY(1f).setDuration(140);
                }
                return false;
            }
        });
    }

    private static final int[] CORES_AVATAR = {
            Color.rgb(64, 112, 206), Color.rgb(222, 120, 30), Color.rgb(37, 150, 110),
            Color.rgb(168, 85, 247), Color.rgb(214, 73, 110), Color.rgb(20, 140, 170)};
    private static final int[] CORES_AVATAR_AREIA = {OLIVA, CAQUI, AMARELO, PESSEGO, SALMAO};

    /** Círculo colorido com a inicial do nome. */
    static TextView avatar(Context c, String nome, int tamanhoDp) {
        String n = nome.trim();
        int[] cores = areia && !contraste ? CORES_AVATAR_AREIA : CORES_AVATAR;
        TextView t = texto(c, n.isEmpty() ? "?" : n.substring(0, n.offsetByCodePoints(0, 1)).toUpperCase(),
                tamanhoDp * 0.42f / escalaFonte, cores == CORES_AVATAR ? Color.WHITE : MARROM);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setBackground(forma(cores[Math.abs(n.hashCode()) % cores.length], 0, tamanhoDp / 2f));
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(tamanhoDp), dp(tamanhoDp)));
        return t;
    }

    /** Etiqueta arredondada; "ativa" fica preenchida. */
    static TextView chip(Context c, String s, boolean ativa) {
        TextView t = texto(c, s, 14, ativa ? sobreDestaque : primaria);
        t.setSingleLine(true);
        t.setPadding(dp(14), dp(7), dp(14), dp(7));
        t.setBackground(clicavel(forma(ativa ? destaque : superficie, ativa ? destaque : primaria, 20)));
        t.setClickable(true);
        return t;
    }

    /** Botão redondo flutuante com "+". */
    static TextView fab(Context c) {
        TextView t = texto(c, "+", 30 / escalaFonte, sobreDestaque);
        t.setGravity(Gravity.CENTER);
        t.setIncludeFontPadding(false);
        t.setBackground(clicavel(forma(destaque, 0, 28)));
        t.setElevation(dp(6));
        t.setClickable(true);
        pressionavel(t);
        return t;
    }

    /** Coloca um botão flutuante no canto de baixo, por cima do conteúdo. */
    static View comFab(Context c, View conteudo, View fab) {
        FrameLayout f = new FrameLayout(c);
        f.addView(conteudo);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, dp(20), dp(20));
        f.addView(fab, lp);
        return f;
    }

    /** Tela vazia simpática: figura grande, frase e um botão para começar. */
    static LinearLayout estadoVazio(Context c, String figura, String frase, View botao) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(0, dp(48), 0, 0);
        TextView f = texto(c, figura, 56 / escalaFonte, texto);
        f.setGravity(Gravity.CENTER);
        l.addView(f);
        TextView t = texto(c, frase, 20, textoSec);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(8), 0, dp(20));
        l.addView(t);
        if (botao != null) {
            l.addView(botao, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        return l;
    }

    static LinearLayout.LayoutParams margemBaixo(int dpBaixo) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(dpBaixo);
        return lp;
    }

    /** Linha de menu com divisória embaixo (gaveta e configurações). */
    static LinearLayout itemMenu(Context c, String s, String direita, int cor) {
        LinearLayout l = new LinearLayout(c);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(4), dp(14), dp(4), dp(14));
        l.setBackground(clicavel(sublinhadoMenu()));
        l.setClickable(true);
        TextView t = texto(c, s, 17, cor);
        l.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (direita != null) l.addView(texto(c, direita, 13, textoSec));
        return l;
    }

    private static Drawable sublinhadoMenu() {
        return linhaEmbaixo(contorno, fundoAtualMenu, dp(1));
    }

    /** Cor de fundo atrás dos itens de menu (a gaveta usa superfície; o resto, fundo). */
    static int fundoAtualMenu;

    static LinearLayout coluna(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(20), dp(20), dp(20));
        return l;
    }

    static ScrollView rolagem(Context c, View conteudo) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.addView(conteudo);
        return s;
    }

    static TextView vazio(Context c, String s) {
        TextView t = texto(c, s, 20, textoSec);
        t.setPadding(dp(4), dp(8), dp(4), dp(8));
        return t;
    }

    // ------------------------------------------------------------------ foto

    /** Círculo com a foto do perfil, ou com a palavra FOTO quando não há. */
    static View foto(Context c, File arquivo, int tamanhoDp) {
        int px = dp(tamanhoDp);
        FrameLayout f = new FrameLayout(c);
        f.setBackground(forma(superficie, contorno, tamanhoDp / 2f));
        Bitmap b = arquivo != null && arquivo.exists() ? carregarCircular(arquivo, px) : null;
        if (b != null) {
            ImageView iv = new ImageView(c);
            iv.setImageBitmap(b);
            f.addView(iv, new FrameLayout.LayoutParams(px, px));
        } else {
            TextView t = texto(c, L.t("FOTO", "PHOTO"), 14, texto);
            t.setGravity(Gravity.CENTER);
            f.addView(t, new FrameLayout.LayoutParams(px, px));
        }
        f.setLayoutParams(new LinearLayout.LayoutParams(px, px));
        return f;
    }

    private static Bitmap carregarCircular(File arquivo, int px) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(arquivo.getPath(), o);
        int amostra = 1;
        while (o.outWidth / (amostra * 2) >= px && o.outHeight / (amostra * 2) >= px) amostra *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = amostra;
        Bitmap src = BitmapFactory.decodeFile(arquivo.getPath(), o);
        if (src == null) return null;
        int lado = Math.min(src.getWidth(), src.getHeight());
        Bitmap quad = Bitmap.createBitmap(src, (src.getWidth() - lado) / 2, (src.getHeight() - lado) / 2, lado, lado);
        Bitmap esc = Bitmap.createScaledBitmap(quad, px, px, true);
        Bitmap out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new BitmapShader(esc, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        new Canvas(out).drawCircle(px / 2f, px / 2f, px / 2f, p);
        return out;
    }

    private Ui() { }
}
